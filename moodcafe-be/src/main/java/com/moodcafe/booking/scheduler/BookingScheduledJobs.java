package com.moodcafe.booking.scheduler;

import com.moodcafe.booking.abstraction.repository.BookingRepository;
import com.moodcafe.booking.abstraction.repository.StoreBookingConfigRepository;
import com.moodcafe.booking.abstraction.service.BookingService;
import com.moodcafe.booking.entity.Booking;
import com.moodcafe.booking.entity.StoreBookingConfig;
import com.moodcafe.booking.entity.enums.BookingEventType;
import com.moodcafe.booking.entity.enums.BookingStatus;
import com.moodcafe.finance.service.BookingCommissionService;
import com.moodcafe.store.entity.Store;
import com.moodcafe.wallet.service.MerchantWalletService;
import com.moodcafe.gamification.abstraction.service.QuestEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookingScheduledJobs {

    private static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final BookingCommissionService commissionService;
    private final BookingRepository bookingRepository;
    private final StoreBookingConfigRepository configRepository;
    private final BookingService bookingService;
    private final MerchantWalletService merchantWalletService;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private QuestEventService questEventService;

    /**
     * Tác vụ 1: Quét các đơn PENDING_PAYMENT quá thời hạn thanh toán
     * -> Áp dụng khoảng đệm an toàn 2 phút (chờ Webhook ngân hàng về trễ) trước khi hủy đơn.
     */
    @Scheduled(fixedRate = 60000)
    public void timeoutUnpaidBookings() {
        // Đệm 2 phút: chỉ quét các đơn có paymentExpiresAt trước thời điểm hiện tại trừ 2 phút
        Instant threshold = Instant.now().minus(Duration.ofMinutes(2));
        for (Booking candidate : bookingRepository.findAllByStatusAndPaymentExpiresAtBefore(BookingStatus.PENDING_PAYMENT, threshold)) {
            try {
                bookingService.timeoutPendingBooking(candidate.getBookingId());
            } catch (Exception e) {
                log.warn("Deferring timeout of booking {}: {}", candidate.getBookingId(), e.getMessage());
            }
        }
    }

    /**
     * Tác vụ 2: Quét các đơn PAID_CONFIRMED quá thời gian giữ bàn (grace_period_mins) mà khách chưa check-in
     * -> Chuyển sang TABLE_EXPIRED: Thu hồi quyền giữ bàn (nhả slot), nhưng bảo lưu hạn mức tiền nước trong ngày.
     */
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void expireLateBookings() {
        // Quét các đơn đã đến hoặc qua giờ hẹn
        List<Booking> candidates = bookingRepository.findAllPaidConfirmedPastGracePeriod(
                BookingStatus.PAID_CONFIRMED,
                Instant.now()
        );

        Instant now = Instant.now();
        for (Booking candidate : candidates) {
            Booking booking = bookingRepository.findByIdForUpdate(candidate.getBookingId()).orElse(null);
            if (booking == null || booking.getStatus() != BookingStatus.PAID_CONFIRMED) continue;
            StoreBookingConfig config = configRepository.findByStoreStoreId(booking.getStore().getStoreId())
                    .orElse(null);
            int graceMins = config != null ? config.getGracePeriodMins() : 30;

            Instant expiryMoment = booking.getStartTime().plus(Duration.ofMinutes(graceMins));
            if (now.isAfter(expiryMoment)) {
                booking.setStatus(BookingStatus.TABLE_EXPIRED);
                bookingRepository.save(booking);

                try {
                    if (questEventService != null) {
                        questEventService.onBookingTableExpired(booking);
                    }
                } catch (Exception ex) {
                    log.warn("Failed to trigger quest event at table expiry: {}", ex.getMessage());
                }

                bookingService.dispatchBookingNotification(
                        booking,
                        BookingEventType.TABLE_EXPIRED,
                        "Đơn đặt bàn " + booking.getBookingCode() + " đã quá thời gian giữ bàn."
                );
                log.info("Booking {} expired table hold (TABLE_EXPIRED), table slot released", booking.getBookingCode());
            }
        }
    }

    /**
     * Tác vụ 3: Với quán bật giới hạn thời gian ngồi (is_time_limit_enabled = true)
     * Quét các đơn CHECKED_IN đã quá endTime quy định -> Phát cảnh báo quá giờ (OVERSTAY_ALERT)
     * đến quầy thu ngân và khách, KHÔNG tự động mở bàn mù quáng khi khách vẫn đang ngồi tại quán.
     */
    @Scheduled(fixedRate = 120000)
    @Transactional
    public void notifyOverstayBookings() {
        List<Booking> overtimed = bookingRepository.findAllCheckedInPastEndTime(
                BookingStatus.CHECKED_IN,
                Instant.now()
        );

        for (Booking candidate : overtimed) {
            Booking booking = bookingRepository.findByIdForUpdate(candidate.getBookingId()).orElse(null);
            if (booking == null || booking.getStatus() != BookingStatus.CHECKED_IN) continue;
            StoreBookingConfig config = configRepository.findByStoreStoreId(booking.getStore().getStoreId())
                    .orElse(null);

            if (config == null || Boolean.TRUE.equals(config.getIsTimeLimitEnabled())) {
                bookingService.dispatchBookingNotification(
                        booking,
                        BookingEventType.OVERSTAY_ALERT,
                        "Đơn đặt bàn " + booking.getBookingCode() + " đã quá thời gian ngồi quy định."
                );
                log.info("Booking {} exceeded sitting duration, dispatched OVERSTAY_ALERT", booking.getBookingCode());
            }
        }
    }

    /**
     * Tác vụ 4: Quét các đơn TABLE_EXPIRED vào cuối ngày khi quán đóng cửa (store_closing_time)
     * -> Nếu khách không tới lấy nước trước giờ đóng cửa, chuyển sang EXPIRED_NO_SHOW (đóng đơn vĩnh viễn).
     */
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void expireNoShowAtClosing() {
        Instant now = Instant.now();
        List<Booking> expiredCandidates = bookingRepository.findAllByStatusWithStore(BookingStatus.TABLE_EXPIRED);

        for (Booking candidate : expiredCandidates) {
            Booking booking = bookingRepository.findByIdForUpdate(candidate.getBookingId()).orElse(null);
            if (booking == null || booking.getStatus() != BookingStatus.TABLE_EXPIRED) continue;
            LocalDate bookingDate = booking.getStartTime().atZone(VN_ZONE).toLocalDate();
            Store store = booking.getStore();
            LocalTime closingTime = (store != null && store.getClosingTime() != null)
                    ? store.getClosingTime()
                    : LocalTime.of(23, 0);
            Instant closingInstant = booking.getServiceClosingDeadline() != null ? booking.getServiceClosingDeadline()
                    : bookingDate.atTime(closingTime).atZone(VN_ZONE).toInstant();

            if (now.isAfter(closingInstant)) {
                booking.setStatus(BookingStatus.EXPIRED_NO_SHOW);
                bookingRepository.save(booking);

                if (commissionService.record(booking, "SETTLE") && booking.getMerchantNetAmount() != null && booking.getMerchantNetAmount().compareTo(BigDecimal.ZERO) > 0) {
                    merchantWalletService.settleToAvailable(
                            booking.getStore().getStoreId(),
                            booking.getMerchantNetAmount(),
                            "BOOKING",
                            booking.getBookingId().toString(),
                            "Giải ngân đơn no-show cuối ngày #" + booking.getBookingCode()
                    );
                }

                bookingService.dispatchBookingNotification(
                        booking,
                        BookingEventType.EXPIRED_NO_SHOW,
                        "Đơn đặt bàn " + booking.getBookingCode() + " đã hết hạn bảo lưu nước do đã qua giờ đóng cửa quán."
                );
                log.info("Booking {} marked as EXPIRED_NO_SHOW at store closing time", booking.getBookingCode());
            }
        }
    }
}
