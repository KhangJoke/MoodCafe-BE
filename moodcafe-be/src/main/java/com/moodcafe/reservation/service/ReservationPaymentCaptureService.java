package com.moodcafe.reservation.service;

import com.moodcafe.reservation.abstraction.repository.ReservationRepository;
import com.moodcafe.reservation.entity.Reservation;
import com.moodcafe.shared.error.ErrorCode;
import com.moodcafe.shared.exceptions.AppException;
import com.moodcafe.voucher.service.VoucherLifecycleService;
import com.moodcafe.wallet.service.MerchantWalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationPaymentCaptureService {

    private final ReservationRepository reservationRepository;
    private final MerchantWalletService merchantWalletService;
    private final VoucherLifecycleService voucherLifecycleService;

    /**
     * Atomically captures reservation payment upon PayOS webhook/polling confirmation:
     * - Verifies amount match (anti-fraud).
     * - Transitions reservation to PAID_CONFIRMED.
     * - Holds merchant net amount in merchant pending_balance.
     * - Captures applied voucher (status = USED).
     * - Idempotent: repeated calls do not duplicate funds or change state.
     */
    @Transactional
    public void capturePaymentSuccess(UUID reservationId, Long orderCode, Long paidAmountVnd) {
        Reservation reservation = reservationRepository.findByIdWithLock(reservationId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Không tìm thấy đơn đặt bàn"));

        if ("PAID_CONFIRMED".equalsIgnoreCase(reservation.getStatus())) {
            log.info("Reservation {} already captured as PAID_CONFIRMED", reservationId);
            return;
        }

        if (!"PENDING_PAYMENT".equalsIgnoreCase(reservation.getStatus())) {
            log.warn("Reservation {} in unexpected status {} for capture", reservationId, reservation.getStatus());
            return;
        }

        // Verify amount integrity
        if (paidAmountVnd == null) {
            log.error("Paid amount is null for reservation {}", reservationId);
            throw new AppException(ErrorCode.BAD_REQUEST, "Số tiền thanh toán không được để trống");
        }

        // Verify orderCode binding
        if (reservation.getPayosOrderCode() != null && !reservation.getPayosOrderCode().equals(orderCode)) {
            log.error("OrderCode mismatch for reservation {}: expected {}, received {}",
                    reservationId, reservation.getPayosOrderCode(), orderCode);
            throw new AppException(ErrorCode.BAD_REQUEST, "Mã đơn PayOS không khớp với đơn đặt bàn");
        }

        BigDecimal actualPaid = BigDecimal.valueOf(paidAmountVnd);
        if (actualPaid.compareTo(reservation.getCustomerPaidAmount()) < 0) {
            log.error("Paid amount {} is less than required customerPaidAmount {} for reservation {}",
                    actualPaid, reservation.getCustomerPaidAmount(), reservationId);
            throw new AppException(ErrorCode.BAD_REQUEST, "Số tiền thanh toán thực tế không khớp với đơn đặt bàn");
        }

        if (actualPaid.compareTo(reservation.getCustomerPaidAmount()) > 0) {
            log.warn("Overpayment detected for reservation {}: required {}, actual {}. Recording excess for reconciliation.",
                    reservationId, reservation.getCustomerPaidAmount(), actualPaid);
        }

        reservation.setActualPaidAmount(actualPaid.setScale(2, java.math.RoundingMode.HALF_UP));
        reservation.setStatus("PAID_CONFIRMED");
        reservation.setPaidAt(Instant.now());
        if (reservation.getPayosOrderCode() == null && orderCode != null) {
            reservation.setPayosOrderCode(orderCode);
        }

        // 1. Hold net amount in merchant pending balance
        merchantWalletService.holdPending(
                reservation.getStoreId(),
                reservation.getMerchantNetAmount(),
                reservation.getReservationId().toString(),
                "Doanh thu cọc đặt bàn (PayOS) #" + reservation.getReservationId()
        );

        // 2. Capture voucher permanently if used
        if (reservation.getVoucherId() != null) {
            voucherLifecycleService.captureVoucher(reservation.getReservationId());
        }

        reservationRepository.save(reservation);
        log.info("Successfully captured reservation {} as PAID_CONFIRMED", reservationId);
    }

    /**
     * Handles expired or abandoned PayOS payment attempt:
     * - Transitions reservation to PAYMENT_EXPIRED.
     * - Releases locked voucher back to AVAILABLE (or refunds if expired).
     */
    @Transactional
    public void handlePaymentExpiredOrFailed(UUID reservationId, String reason) {
        Reservation reservation = reservationRepository.findByIdWithLock(reservationId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Không tìm thấy đơn đặt bàn"));

        if ("PAID_CONFIRMED".equalsIgnoreCase(reservation.getStatus()) || "COMPLETED".equalsIgnoreCase(reservation.getStatus())) {
            log.warn("Cannot expire reservation {} because it is already confirmed", reservationId);
            return;
        }

        reservation.setStatus("PAYMENT_EXPIRED");
        reservationRepository.save(reservation);

        // Release voucher back to AVAILABLE
        if (reservation.getVoucherId() != null) {
            voucherLifecycleService.releaseVoucher(reservation.getReservationId());
        }
        log.info("Reservation {} payment expired/failed: {}", reservationId, reason);
    }
}
