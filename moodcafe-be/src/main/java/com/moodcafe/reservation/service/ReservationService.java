package com.moodcafe.reservation.service;

import com.moodcafe.finance.dto.ReservationFinancialSnapshot;
import com.moodcafe.finance.dto.ReservationPricingInput;
import com.moodcafe.finance.service.ReservationPricingService;
import com.moodcafe.loyalty.service.LoyaltyRewardService;
import com.moodcafe.payment.service.OrderCodeGeneratorService;
import com.moodcafe.payment.service.PaymentAttemptService;
import com.moodcafe.reservation.abstraction.repository.ReservationRepository;
import com.moodcafe.reservation.abstraction.repository.TableCategoryRepository;
import com.moodcafe.reservation.dto.CreateReservationRequest;
import com.moodcafe.reservation.dto.ReservationResponse;
import com.moodcafe.reservation.entity.Reservation;
import com.moodcafe.reservation.entity.TableCategory;
import com.moodcafe.shared.error.ErrorCode;
import com.moodcafe.shared.exceptions.AppException;
import com.moodcafe.store.abstraction.repository.StoreRepository;
import com.moodcafe.store.abstraction.service.StoreStaffService;
import com.moodcafe.store.entity.Store;
import com.moodcafe.voucher.abstraction.repository.CustomerVoucherRepository;
import com.moodcafe.voucher.abstraction.repository.VoucherPointAllocationRepository;
import com.moodcafe.voucher.entity.CustomerVoucher;
import com.moodcafe.voucher.service.VoucherLifecycleService;
import com.moodcafe.wallet.service.MerchantWalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final TableCategoryRepository tableCategoryRepository;
    private final StoreRepository storeRepository;
    private final ServiceDayDeadlineResolver serviceDayDeadlineResolver;
    private final OrderCodeGeneratorService orderCodeGeneratorService;
    private final PaymentAttemptService paymentAttemptService;
    private final ReservationPricingService pricingService;
    private final CustomerVoucherRepository customerVoucherRepository;
    private final VoucherPointAllocationRepository allocationRepository;
    private final VoucherLifecycleService voucherLifecycleService;
    private final MerchantWalletService merchantWalletService;
    private final LoyaltyRewardService loyaltyRewardService;
    private final StoreStaffService storeStaffService;
    private final ReservationQrVerificationService qrVerificationService;

    public static String computeBookingPayloadHash(CreateReservationRequest request) {
        if (request == null) {
            return "";
        }
        try {
            String payload = String.format("%s:%s:%s:%s:%s",
                    request.getStoreId(),
                    request.getCategoryId(),
                    request.getBookingTime() != null ? request.getBookingTime().toEpochMilli() : 0,
                    request.getGuestCount() != null ? request.getGuestCount() : 0,
                    request.getVoucherId() != null ? request.getVoucherId() : ""
            );
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(payload.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(hashBytes);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    /**
     * Phase 1: Validates category, quotes pricing, creates reservation with orderCode & locks voucher,
     * persists durable payment attempt within an atomic DB transaction.
     */
    @Transactional
    public Reservation prepareReservation(CreateReservationRequest request, UUID userId, String intentKey) {
        if (request == null || request.getStoreId() == null || request.getBookingTime() == null) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Store ID và thời gian đặt bàn không được để trống");
        }

        if (request.getCategoryId() == null) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Vui lòng chọn loại bàn (categoryId)");
        }

        TableCategory category = tableCategoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Không tìm thấy loại bàn"));

        if (!request.getStoreId().equals(category.getStoreId())
                || Boolean.FALSE.equals(category.getIsActive())
                || Boolean.TRUE.equals(category.getIsDeleted())) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Loại bàn không thuộc cơ sở này, đã bị xóa hoặc không còn hoạt động");
        }

        BigDecimal grossAmount = category.getMinSpendAmount();

        BigDecimal voucherDiscount = BigDecimal.ZERO;
        BigDecimal refundFunded = BigDecimal.ZERO;
        BigDecimal rewardFunded = BigDecimal.ZERO;

        if (request.getVoucherId() != null) {
            CustomerVoucher voucher = customerVoucherRepository.findByIdWithLock(request.getVoucherId())
                    .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Không tìm thấy voucher"));

            if (!voucher.getUserId().equals(userId)) {
                throw new AppException(ErrorCode.FORBIDDEN, "Bạn không có quyền sử dụng voucher này");
            }
            if (!"AVAILABLE".equalsIgnoreCase(voucher.getStatus())) {
                throw new AppException(ErrorCode.BAD_REQUEST, "Voucher không ở trạng thái khả dụng");
            }
            if (voucher.getExpiresAt().isBefore(Instant.now())) {
                throw new AppException(ErrorCode.BAD_REQUEST, "Voucher đã hết hạn");
            }

            voucherDiscount = voucher.getFaceValueVnd();
            Long refundPts = allocationRepository.sumAllocatedPointsByVoucherIdAndFundingType(voucher.getVoucherId(), "REFUND");
            Long rewardPts = allocationRepository.sumAllocatedPointsByVoucherIdAndFundingType(voucher.getVoucherId(), "REWARD");

            refundFunded = refundPts != null ? BigDecimal.valueOf(refundPts) : BigDecimal.ZERO;
            rewardFunded = rewardPts != null ? BigDecimal.valueOf(rewardPts) : BigDecimal.ZERO;
        }

        // Quote financial snapshot with anti-loss protections
        ReservationPricingInput pricingInput = ReservationPricingInput.builder()
                .storeId(request.getStoreId())
                .grossAmount(grossAmount)
                .userId(userId)
                .voucherId(request.getVoucherId())
                .voucherDiscount(voucherDiscount)
                .voucherRefundFunded(refundFunded)
                .voucherRewardFunded(rewardFunded)
                .build();

        ReservationFinancialSnapshot snapshot = pricingService.quote(pricingInput);

        Store store = storeRepository.findById(request.getStoreId()).orElse(null);
        LocalTime opening = store != null ? store.getOpeningTime() : null;
        LocalTime closing = store != null ? store.getClosingTime() : null;

        Instant graceDeadline = request.getBookingTime().plus(15, ChronoUnit.MINUTES);
        Instant closingDeadline = serviceDayDeadlineResolver.resolve(request.getBookingTime(), opening, closing, null);

        long orderCode = orderCodeGeneratorService.nextOrderCode();

        Reservation reservation = Reservation.builder()
                .storeId(request.getStoreId())
                .userId(userId)
                .categoryId(request.getCategoryId())
                .bookingTime(request.getBookingTime())
                .guestCount(request.getGuestCount() != null ? request.getGuestCount() : 2)
                .grossAmount(snapshot.getGrossAmount())
                .voucherDiscount(snapshot.getVoucherDiscount())
                .customerPaidAmount(snapshot.getCustomerPaidAmount())
                .commissionAmount(snapshot.getCommissionAmount())
                .merchantNetAmount(snapshot.getMerchantNetAmount())
                .voucherId(request.getVoucherId())
                .voucherRefundFundedAmount(snapshot.getVoucherRefundFundedAmount())
                .voucherRewardFundedAmount(snapshot.getVoucherRewardFundedAmount())
                .commissionRateSnapshot(snapshot.getCommissionRate())
                .graceDeadline(graceDeadline)
                .serviceClosingDeadline(closingDeadline)
                .paymentPurpose("BOOKING")
                .notes(request.getNotes())
                .status("PENDING_PAYMENT")
                .payosOrderCode(orderCode)
                .build();

        reservation = reservationRepository.save(reservation);

        // Lock voucher
        if (request.getVoucherId() != null) {
            voucherLifecycleService.lockVoucher(userId, request.getVoucherId(), reservation.getReservationId(), Instant.now().plusSeconds(900));
        }

        // Persist durable PaymentAttempt
        String payloadHash = computeBookingPayloadHash(request);
        paymentAttemptService.createAttempt(
                "BOOKING",
                reservation.getReservationId().toString(),
                orderCode,
                snapshot.getCustomerPaidAmount(),
                "VND",
                intentKey,
                userId,
                payloadHash
        );

        return reservation;
    }

    @Transactional
    public void updatePaymentDetails(UUID reservationId, String paymentUrl, String paymentLinkId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Không tìm thấy đơn đặt bàn"));
        reservation.setPaymentUrl(paymentUrl);
        reservation.setPayosPaymentLinkId(paymentLinkId);
        reservationRepository.save(reservation);
    }

    /**
     * Check-in when customer arrives:
     * - Distinguishes between Customer self-check-in (with table/store QR) and Staff check-in.
     * - Strangers (neither customer nor store staff) receive 403 FORBIDDEN.
     * - Checks early bound (cannot check in before bookingTime - 30 mins).
     * - On-time check-in (now <= graceDeadline): CHECKED_IN + awards 1.000 MoodPoints.
     * - Overdue check-in (now > graceDeadline): customer self check-in blocked; store staff can accept as CLAIMED_LATE.
     */
    @Transactional
    public ReservationResponse checkIn(UUID reservationId, UUID actorId, String qrToken) {
        Reservation reservation = reservationRepository.findByIdWithLock(reservationId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Không tìm thấy đơn đặt bàn"));

        if (!"PAID_CONFIRMED".equalsIgnoreCase(reservation.getStatus()) && !"TABLE_EXPIRED".equalsIgnoreCase(reservation.getStatus())) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Đơn đặt bàn chưa sẵn sàng để check-in hoặc đã hoàn tất");
        }

        Instant now = Instant.now();
        boolean isCustomer = reservation.getUserId().equals(actorId);

        if (!isCustomer) {
            // Verify actor is active staff of this store
            storeStaffService.requireStoreAccess(reservation.getStoreId(), "OWNER", "MANAGER", "CASHIER");
        }

        // Early check-in protection: cannot check-in more than 30 minutes before bookingTime
        if (now.isBefore(reservation.getBookingTime().minus(30, ChronoUnit.MINUTES))) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Chưa đến giờ nhận bàn (chỉ có thể nhận bàn trước tối đa 30 phút)");
        }

        reservation.setCheckInTime(now);

        if (now.isBefore(reservation.getGraceDeadline()) || now.equals(reservation.getGraceDeadline())) {
            // On-time check-in
            if (isCustomer) {
                qrVerificationService.validateStoreQrToken(reservation.getStoreId(), qrToken);
            }
            reservation.setStatus("CHECKED_IN");
            loyaltyRewardService.awardOnTimeCheckIn(reservation);
        } else {
            // Overdue check-in: customer self-checkin is NOT allowed; staff can accept late
            if (isCustomer) {
                throw new AppException(ErrorCode.BAD_REQUEST,
                        "Đã quá thời gian nhận bàn (15 phút grace). Vui lòng liên hệ thu ngân tại quầy để được phục vụ nước uống.");
            }

            if (now.isAfter(reservation.getServiceClosingDeadline())) {
                throw new AppException(ErrorCode.BAD_REQUEST, "Đã quá giờ đóng cửa của cơ sở, không thể tiếp nhận đơn");
            }

            reservation.setStatus("CLAIMED_LATE");
            reservation.setClaimedLateTime(now);

            // Settle to merchant available balance
            merchantWalletService.settleToAvailable(
                    reservation.getStoreId(),
                    reservation.getMerchantNetAmount(),
                    reservationId.toString(),
                    "Giải ngân đơn đặt bàn nhận nước trễ #" + reservationId
            );

            // Award spend reward (no on-time bonus)
            loyaltyRewardService.awardSpendReward(reservation);
        }

        return toResponse(reservationRepository.save(reservation));
    }

    /**
     * Check-out upon departure:
     * - If Customer self-checkout via QR and duration >= 30 mins: verifies physical store QR and awards 1.000 pts SELF_CHECK_OUT bonus.
     * - If Staff checkout: COMPLETED, settles merchant funds, 0 self-checkout bonus.
     * - Strangers receive 403 FORBIDDEN.
     */
    @Transactional
    public ReservationResponse checkOut(UUID reservationId, UUID actorId, boolean isSelfQr, String qrToken) {
        Reservation reservation = reservationRepository.findByIdWithLock(reservationId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Không tìm thấy đơn đặt bàn"));

        if (!"CHECKED_IN".equalsIgnoreCase(reservation.getStatus())) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Đơn chưa check-in hoặc đã hoàn tất");
        }

        boolean isCustomer = reservation.getUserId().equals(actorId);
        if (!isCustomer) {
            storeStaffService.requireStoreAccess(reservation.getStoreId(), "OWNER", "MANAGER", "CASHIER");
        } else {
            qrVerificationService.validateStoreQrToken(reservation.getStoreId(), qrToken);
        }

        Instant now = Instant.now();
        reservation.setCheckOutTime(now);
        reservation.setStatus("COMPLETED");

        merchantWalletService.settleToAvailable(
                reservation.getStoreId(),
                reservation.getMerchantNetAmount(),
                reservationId.toString(),
                "Giải ngân đơn hoàn tất phục vụ #" + reservationId
        );

        loyaltyRewardService.awardSpendReward(reservation);

        // Customer self-checkout bonus (stay >= 30 mins with verified physical QR)
        if (isCustomer && isSelfQr && reservation.getCheckInTime() != null) {
            long minutes = Duration.between(reservation.getCheckInTime(), now).toMinutes();
            if (minutes >= 30) {
                qrVerificationService.validateStoreQrToken(reservation.getStoreId(), qrToken);
                loyaltyRewardService.awardSelfCheckOut(reservation, minutes);
            }
        }

        return toResponse(reservationRepository.save(reservation));
    }

    @Transactional
    public ReservationResponse checkOut(UUID reservationId, UUID actorId, boolean isSelfQr) {
        return checkOut(reservationId, actorId, isSelfQr, null);
    }

    private Instant calculateServiceClosingDeadline(Instant bookingTime) {
        // Service day closing time: 23:00 on the day of booking in Asia/Ho_Chi_Minh
        ZoneId zone = ZoneId.of("Asia/Ho_Chi_Minh");
        ZonedDateTime bookingZdt = bookingTime.atZone(zone);
        ZonedDateTime closingZdt = bookingZdt.with(LocalTime.of(23, 0, 0));
        if (closingZdt.isBefore(bookingZdt)) {
            closingZdt = closingZdt.plusDays(1);
        }
        return closingZdt.toInstant();
    }

    private synchronized long generateBookingOrderCode() {
        long code = System.currentTimeMillis() / 1000;
        while (reservationRepository.findByPayosOrderCode(code).isPresent()) {
            code++;
        }
        return code;
    }

    public ReservationResponse toResponse(Reservation r) {
        return ReservationResponse.builder()
                .reservationId(r.getReservationId())
                .storeId(r.getStoreId())
                .userId(r.getUserId())
                .categoryId(r.getCategoryId())
                .bookingTime(r.getBookingTime())
                .guestCount(r.getGuestCount())
                .status(r.getStatus())
                .grossAmount(r.getGrossAmount())
                .voucherDiscount(r.getVoucherDiscount())
                .customerPaidAmount(r.getCustomerPaidAmount())
                .commissionAmount(r.getCommissionAmount())
                .merchantNetAmount(r.getMerchantNetAmount())
                .voucherId(r.getVoucherId())
                .payosOrderCode(r.getPayosOrderCode())
                .paymentUrl(r.getPaymentUrl())
                .checkInTime(r.getCheckInTime())
                .checkOutTime(r.getCheckOutTime())
                .graceDeadline(r.getGraceDeadline())
                .notes(r.getNotes())
                .build();
    }
}
