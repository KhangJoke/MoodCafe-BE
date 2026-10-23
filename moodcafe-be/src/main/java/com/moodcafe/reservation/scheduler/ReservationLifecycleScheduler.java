package com.moodcafe.reservation.scheduler;

import com.moodcafe.finance.service.ReservationSettlementService;
import com.moodcafe.payment.dto.GatewayPaymentObservation;
import com.moodcafe.payment.entity.PaymentAttempt;
import com.moodcafe.payment.abstraction.service.PayOSService;
import com.moodcafe.payment.service.PaymentAttemptService;
import com.moodcafe.reservation.abstraction.repository.ReservationRepository;
import com.moodcafe.reservation.entity.Reservation;
import com.moodcafe.reservation.service.ReservationPaymentCaptureService;
import com.moodcafe.voucher.abstraction.repository.CustomerVoucherRepository;
import com.moodcafe.voucher.entity.CustomerVoucher;
import com.moodcafe.voucher.service.VoucherLifecycleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationLifecycleScheduler {

    private final ReservationRepository reservationRepository;
    private final ReservationSettlementService settlementService;
    private final ReservationPaymentCaptureService captureService;
    private final PayOSService payOSService;
    private final PaymentAttemptService paymentAttemptService;
    private final CustomerVoucherRepository customerVoucherRepository;
    private final VoucherLifecycleService voucherLifecycleService;

    /**
     * 1. Quá hạn grace (15 phút sau giờ hẹn) mà chưa check-in -> TABLE_EXPIRED.
     * Trả bàn vật lý cho quán, nhưng GIỮ tiền cọc trong pending để khách vẫn có quyền nhận nước đến giờ đóng cửa.
     */
    @Scheduled(fixedDelay = 60000)
    public void processOverdueGraceReservations() {
        try {
            Instant now = Instant.now();
            List<Reservation> overdue = reservationRepository.findOverdueGraceReservations(now);
            for (Reservation res : overdue) {
                try {
                    settlementService.settleReservation(res.getReservationId(), "TABLE_EXPIRED");
                    log.info("Auto-transitioned reservation {} to TABLE_EXPIRED after grace deadline", res.getReservationId());
                } catch (Exception e) {
                    log.error("Failed to process TABLE_EXPIRED for reservation {}: {}", res.getReservationId(), e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Error in processOverdueGraceReservations: {}", e.getMessage());
        }
    }

    /**
     * 2. Quá giờ đóng cửa (serviceClosingDeadline) mà khách không đến -> EXPIRED_NO_SHOW.
     * Giải ngân 100% net cọc từ pending -> available cho Quán. Khách nhận 0 điểm thưởng.
     */
    @Scheduled(fixedDelay = 300000)
    public void processClosingNoShowReservations() {
        try {
            Instant now = Instant.now();
            List<Reservation> noShows = reservationRepository.findClosingNoShowReservations(now);
            for (Reservation res : noShows) {
                try {
                    settlementService.settleReservation(res.getReservationId(), "EXPIRED_NO_SHOW");
                    log.info("Auto-settled reservation {} to EXPIRED_NO_SHOW at end of service day", res.getReservationId());
                } catch (Exception e) {
                    log.error("Failed to process EXPIRED_NO_SHOW for reservation {}: {}", res.getReservationId(), e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Error in processClosingNoShowReservations: {}", e.getMessage());
        }
    }

    /**
     * 3. Quét các đơn cọc chờ thanh toán quá hạn 15 phút (PENDING_PAYMENT):
     * - Kiểm tra gateway PayOS lần cuối:
     *   - Nếu đã thanh toán: capture thành PAID_CONFIRMED.
     *   - Nếu chưa thanh toán: hủy đơn cọc quá hạn và giải phóng voucher LOCKED về AVAILABLE.
     */
    @Scheduled(fixedDelay = 120000)
    public void sweepExpiredPaymentHolds() {
        try {
            Instant cutoff = Instant.now().minus(15, ChronoUnit.MINUTES);
            List<Reservation> pendingList = reservationRepository.findAll().stream()
                    .filter(r -> "PENDING_PAYMENT".equalsIgnoreCase(r.getStatus()) && r.getCreatedAt().isBefore(cutoff))
                    .toList();

            for (Reservation res : pendingList) {
                try {
                    if (res.getPayosOrderCode() != null && payOSService.isConfigured()) {
                        GatewayPaymentObservation obs = paymentAttemptService.reconcile(res.getPayosOrderCode());
                        if (obs.getStatus() == GatewayPaymentObservation.GatewayStatus.PAID) {
                            Long amountVnd = obs.getAmount() != null ? obs.getAmount().longValue() : null;
                            captureService.capturePaymentSuccess(res.getReservationId(), res.getPayosOrderCode(), amountVnd);
                            paymentAttemptService.markStatus(res.getPayosOrderCode(), "PAID");
                            log.info("Captured pending reservation {} via payment reconciliation in scheduler", res.getReservationId());
                            continue;
                        } else if (obs.getStatus() == GatewayPaymentObservation.GatewayStatus.UNKNOWN) {
                            // Gateway error / connection timeout. Do NOT expire or release voucher!
                            log.warn("Deferring sweep for reservation {} because gateway returned UNKNOWN", res.getReservationId());
                            continue;
                        } else if (obs.getStatus() == GatewayPaymentObservation.GatewayStatus.PENDING) {
                            // Still within buffer, skip expiry
                            if (res.getCreatedAt().isAfter(Instant.now().minus(30, ChronoUnit.MINUTES))) {
                                continue;
                            }
                        }
                    }

                    // Gateway explicitly confirmed expired/failed or hard timeout passed without payment
                    captureService.handlePaymentExpiredOrFailed(res.getReservationId(), "Quá hạn thanh toán 15 phút");
                    if (res.getPayosOrderCode() != null) {
                        paymentAttemptService.markStatus(res.getPayosOrderCode(), "EXPIRED");
                    }
                } catch (Exception e) {
                    log.error("Failed to sweep payment for reservation {}: {}", res.getReservationId(), e.getMessage());
                }
            }

            // Quét voucher LOCKED mồ côi (holdUntil < now)
            Instant now = Instant.now();
            List<CustomerVoucher> lockedOrphans = customerVoucherRepository.findAll().stream()
                    .filter(v -> "LOCKED".equalsIgnoreCase(v.getStatus()) && v.getHoldUntil() != null && v.getHoldUntil().isBefore(now))
                    .toList();

            for (CustomerVoucher v : lockedOrphans) {
                if (v.getLockedReservationId() != null) {
                    Optional<Reservation> resOpt = reservationRepository.findById(v.getLockedReservationId());
                    if (resOpt.isPresent()) {
                        Reservation r = resOpt.get();
                        // If reservation is already paid or active, voucher must NOT be released!
                        if ("PAID_CONFIRMED".equalsIgnoreCase(r.getStatus()) ||
                                "CHECKED_IN".equalsIgnoreCase(r.getStatus()) ||
                                "COMPLETED".equalsIgnoreCase(r.getStatus()) ||
                                "TABLE_EXPIRED".equalsIgnoreCase(r.getStatus())) {
                            log.info("Voucher {} was LOCKED but reservation {} is {}. Capturing voucher.",
                                    v.getVoucherId(), r.getReservationId(), r.getStatus());
                            voucherLifecycleService.captureVoucher(r.getReservationId());
                            continue;
                        }
                        // If reservation is still PENDING_PAYMENT, do not release if payment attempt is PENDING, UNKNOWN, or PAID
                        if ("PENDING_PAYMENT".equalsIgnoreCase(r.getStatus())) {
                            if (r.getPayosOrderCode() != null) {
                                Optional<PaymentAttempt> att = paymentAttemptService.findByOrderCode(r.getPayosOrderCode());
                                if (att.isPresent() && ("PENDING".equalsIgnoreCase(att.get().getStatus()) ||
                                        "UNKNOWN".equalsIgnoreCase(att.get().getStatus()) ||
                                        "PAID".equalsIgnoreCase(att.get().getStatus()))) {
                                    log.warn("Deferring orphan voucher {} release because attempt {} status is {}",
                                            v.getVoucherId(), r.getPayosOrderCode(), att.get().getStatus());
                                    continue;
                                }
                            }
                        }
                    }
                    voucherLifecycleService.releaseVoucher(v.getLockedReservationId());
                }
            }
        } catch (Exception e) {
            log.error("Error in sweepExpiredPaymentHolds: {}", e.getMessage());
        }
    }
}
