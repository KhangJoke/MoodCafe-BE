package com.moodcafe.booking.scheduler;

import com.moodcafe.booking.abstraction.repository.BookingRepository;
import com.moodcafe.booking.abstraction.repository.StoreBookingConfigRepository;
import com.moodcafe.booking.abstraction.service.BookingService;
import com.moodcafe.booking.entity.Booking;
import com.moodcafe.booking.entity.StoreBookingConfig;
import com.moodcafe.booking.entity.enums.BookingEventType;
import com.moodcafe.booking.entity.enums.BookingStatus;
import com.moodcafe.store.entity.Store;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookingScheduledJobs {

    private static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final BookingRepository bookingRepository;
    private final StoreBookingConfigRepository configRepository;
    private final BookingService bookingService;

    /**
     * Tác vụ 1: Quét các đơn PENDING_PAYMENT quá 10 phút chưa thanh toán
     * -> Chuyển sang PAYMENT_FAILED và lập tức giải phóng slot bàn.
     */
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void timeoutUnpaidBookings() {
        Instant threshold = Instant.now().minus(Duration.ofMinutes(10));
        List<Booking> unpaidList = bookingRepository.findAllByStatusAndCreatedAtBefore(
                BookingStatus.PENDING_PAYMENT,
                threshold
        );

        if (!unpaidList.isEmpty()) {
            log.info("Processing {} unpaid bookings past 10 minutes timeout", unpaidList.size());
            for (Booking booking : unpaidList) {
                booking.setStatus(BookingStatus.PAYMENT_FAILED);
                bookingService.dispatchBookingNotification(
                        booking,
                        BookingEventType.PAYMENT_FAILED,
                        "Đơn đặt bàn " + booking.getBookingCode() + " đã hết hạn thanh toán 10 phút."
                );
            }
            bookingRepository.saveAll(unpaidList);
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
        for (Booking booking : candidates) {
            StoreBookingConfig config = configRepository.findByStoreStoreId(booking.getStore().getStoreId())
                    .orElse(null);
            int graceMins = config != null ? config.getGracePeriodMins() : 30;

            Instant expiryMoment = booking.getStartTime().plus(Duration.ofMinutes(graceMins));
            if (now.isAfter(expiryMoment)) {
                booking.setStatus(BookingStatus.TABLE_EXPIRED);
                bookingRepository.save(booking);
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
     * Quét các đơn CHECKED_IN đã quá endTime quy định -> Chuyển sang COMPLETED và giải phóng slot bàn.
     */
    @Scheduled(fixedRate = 120000)
    @Transactional
    public void autoReleaseSittingTime() {
        List<Booking> overtimed = bookingRepository.findAllCheckedInPastEndTime(
                BookingStatus.CHECKED_IN,
                Instant.now()
        );

        for (Booking booking : overtimed) {
            StoreBookingConfig config = configRepository.findByStoreStoreId(booking.getStore().getStoreId())
                    .orElse(null);

            if (config == null || Boolean.TRUE.equals(config.getIsTimeLimitEnabled())) {
                booking.setStatus(BookingStatus.COMPLETED);
                booking.setCompletedAt(Instant.now());
                bookingRepository.save(booking);
                bookingService.dispatchBookingNotification(
                        booking,
                        BookingEventType.COMPLETED,
                        "Đơn đặt bàn " + booking.getBookingCode() + " đã hết thời gian ngồi quy định."
                );
                log.info("Booking {} automatically completed after sitting duration, table slot released", booking.getBookingCode());
            }
        }
    }

    /**
     * Tác vụ 4: Quét các đơn TABLE_EXPIRED vào cuối ngày khi quán đóng cửa (store_closing_time)
     * -> Nếu khách không tới lấy nước trước giờ đóng cửa, chuyển sang EXPIRED_NO_SHOW (đóng đơn vĩnh viễn).
     */
    @Scheduled(fixedRate = 300000)
    @Transactional
    public void expireNoShowAtClosing() {
        Instant now = Instant.now();
        ZonedDateTime nowZoned = now.atZone(VN_ZONE);
        LocalTime currentTime = nowZoned.toLocalTime();

        // Quét các đơn TABLE_EXPIRED của ngày hôm nay
        ZonedDateTime startOfDay = nowZoned.toLocalDate().atStartOfDay(VN_ZONE);
        ZonedDateTime endOfDay = nowZoned.toLocalDate().atTime(LocalTime.MAX).atZone(VN_ZONE);

        List<Booking> expiredCandidates = bookingRepository.findAll().stream()
                .filter(b -> b.getStatus() == BookingStatus.TABLE_EXPIRED)
                .filter(b -> b.getStartTime().isAfter(startOfDay.toInstant()) && b.getStartTime().isBefore(endOfDay.toInstant()))
                .toList();

        for (Booking booking : expiredCandidates) {
            Store store = booking.getStore();
            LocalTime closingTime = store.getClosingTime() != null ? store.getClosingTime() : LocalTime.of(23, 0);

            if (currentTime.isAfter(closingTime)) {
                booking.setStatus(BookingStatus.EXPIRED_NO_SHOW);
                bookingRepository.save(booking);
                log.info("Booking {} marked as EXPIRED_NO_SHOW at store closing time", booking.getBookingCode());
            }
        }
    }
}
