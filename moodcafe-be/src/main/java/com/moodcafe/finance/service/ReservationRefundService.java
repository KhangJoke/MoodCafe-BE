package com.moodcafe.finance.service;

import com.moodcafe.reservation.abstraction.repository.ReservationRepository;
import com.moodcafe.reservation.entity.Reservation;
import com.moodcafe.shared.error.ErrorCode;
import com.moodcafe.shared.exceptions.AppException;
import com.moodcafe.voucher.service.VoucherLifecycleService;
import com.moodcafe.wallet.dto.RefundPointCommand;
import com.moodcafe.wallet.service.CustomerWalletService;
import com.moodcafe.wallet.service.MerchantWalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationRefundService {

    private static final long FREE_CANCEL_HOURS = 2L;

    private final ReservationRepository reservationRepository;
    private final MerchantWalletService merchantWalletService;
    private final CustomerWalletService customerWalletService;
    private final VoucherLifecycleService voucherLifecycleService;

    /**
     * Processes cancellation and refund for a paid reservation:
     * 1. Checks 2-hour free cancellation window.
     * 2. Reverts merchant pending balance.
     * 3. Refunds PayOS paid amount 1:1 into MoodPoints (REFUND source).
     * 4. Restores voucher (or converts to MoodPoints if expired).
     */
    @Transactional
    public Reservation cancelAndRefund(UUID reservationId, UUID userId, String reason) {
        Reservation reservation = reservationRepository.findByIdWithLock(reservationId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Không tìm thấy đơn đặt bàn"));

        if (!reservation.getUserId().equals(userId)) {
            throw new AppException(ErrorCode.FORBIDDEN, "Bạn không có quyền thao tác trên đơn này");
        }

        if (!"PAID_CONFIRMED".equalsIgnoreCase(reservation.getStatus())) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Chỉ có thể hủy đơn đặt bàn đã thanh toán thành công");
        }

        Instant now = Instant.now();
        Duration untilBooking = Duration.between(now, reservation.getBookingTime());
        if (untilBooking.toHours() < FREE_CANCEL_HOURS) {
            throw new AppException(ErrorCode.BAD_REQUEST,
                    "Đã quá thời hạn hủy đơn miễn phí (chỉ được hủy trước giờ hẹn tối thiểu " + FREE_CANCEL_HOURS + " giờ)");
        }

        // 1. Update reservation status
        reservation.setStatus("CANCELLED_REFUNDED");
        reservation.setNotes(reason != null ? "Khách hủy: " + reason : "Khách hủy đúng hạn");

        // 2. Revert pending funds from merchant wallet
        merchantWalletService.revertPending(
                reservation.getStoreId(),
                reservation.getMerchantNetAmount(),
                reservationId.toString(),
                "Hoàn tiền đơn đặt bàn bị hủy #" + reservationId
        );

        // 3. Refund real cash paid amount (PayOS) 1:1 into MoodPoints
        if (reservation.getCustomerPaidAmount() != null && reservation.getCustomerPaidAmount().longValue() > 0) {
            long refundPoints = reservation.getCustomerPaidAmount().longValue();
            customerWalletService.creditRefundPoints(RefundPointCommand.builder()
                    .userId(userId)
                    .points(refundPoints)
                    .refundId(reservationId.toString())
                    .businessKey("REFUND_PAYOS_RES_" + reservationId)
                    .description("Hoàn tiền đơn đặt bàn #" + reservationId + " thành MoodPoints (1:1)")
                    .build());
            log.info("Refunded {} PayOS cash as MoodPoints to user {} for reservation {}",
                    refundPoints, userId, reservationId);
        }

        // 4. Restore applied voucher (handled separately, no double refund)
        if (reservation.getVoucherId() != null) {
            voucherLifecycleService.restoreVoucherForRefund(reservationId, reservation.getVoucherId());
        }

        return reservationRepository.save(reservation);
    }
}
