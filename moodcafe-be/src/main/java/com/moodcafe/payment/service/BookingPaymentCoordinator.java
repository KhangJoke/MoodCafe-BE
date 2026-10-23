package com.moodcafe.payment.service;

import com.moodcafe.payment.abstraction.service.PayOSService;
import com.moodcafe.payment.dto.response.PayOSResponse;
import com.moodcafe.payment.entity.PaymentAttempt;
import com.moodcafe.reservation.abstraction.repository.ReservationRepository;
import com.moodcafe.reservation.dto.CreateReservationRequest;
import com.moodcafe.reservation.dto.ReservationResponse;
import com.moodcafe.reservation.entity.Reservation;
import com.moodcafe.reservation.service.ReservationPaymentCaptureService;
import com.moodcafe.reservation.service.ReservationService;
import com.moodcafe.shared.error.ErrorCode;
import com.moodcafe.shared.exceptions.AppException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingPaymentCoordinator {

    private final ReservationService reservationService;
    private final ReservationRepository reservationRepository;
    private final PayOSService payOSService;
    private final PaymentAttemptService paymentAttemptService;
    private final ReservationPaymentCaptureService captureService;

    /**
     * Coordinates reservation creation with durable payment attempt,
     * executing network gateway calls OUTSIDE of the lock-bearing database transaction.
     */
    public ReservationResponse createReservation(CreateReservationRequest request, UUID customerId, String intentKey) {
        String effectiveIntentKey = (intentKey != null && !intentKey.isBlank())
                ? intentKey.trim()
                : (request.getIntentKey() != null ? request.getIntentKey().trim() : null);

        // 1. Scoped Idempotency Check: (customerId, "BOOKING", intentKey)
        if (effectiveIntentKey != null && !effectiveIntentKey.isBlank()) {
            Optional<PaymentAttempt> existingAttemptOpt = paymentAttemptService
                    .findByActorIdAndPurposeAndIntentKey(customerId, "BOOKING", effectiveIntentKey);
            if (existingAttemptOpt.isPresent()) {
                PaymentAttempt attempt = existingAttemptOpt.get();
                String incomingHash = ReservationService.computeBookingPayloadHash(request);
                if (attempt.getPayloadHash() != null && !attempt.getPayloadHash().equals(incomingHash)) {
                    throw new AppException(ErrorCode.CONFLICT,
                            "Idempotency-Key đã được sử dụng với thông tin đặt bàn khác");
                }
                log.info("Replaying existing reservation for actor: {}, intentKey: {}, orderCode: {}",
                        customerId, effectiveIntentKey, attempt.getOrderCode());
                try {
                    UUID resId = UUID.fromString(attempt.getBusinessId());
                    Optional<Reservation> resOpt = reservationRepository.findById(resId);
                    if (resOpt.isPresent()) {
                        return reservationService.toResponse(resOpt.get());
                    }
                } catch (Exception e) {
                    log.warn("Could not find reservation for businessId: {}", attempt.getBusinessId());
                }
            }
        }

        // 2. Phase 1: Prepare reservation, price snapshot, lock voucher & persist payment attempt (committed DB transaction)
        Reservation preparedReservation = reservationService.prepareReservation(request, customerId, effectiveIntentKey);
        Long orderCode = preparedReservation.getPayosOrderCode();
        BigDecimal customerPaid = preparedReservation.getCustomerPaidAmount();

        // 3. Phase 2: Outside lock
        // Case 1: 100% Voucher Zero-Pay
        if (customerPaid.compareTo(BigDecimal.ZERO) == 0) {
            log.info("Processing zero-payable reservation {} internally without gateway", preparedReservation.getReservationId());
            captureService.capturePaymentSuccess(preparedReservation.getReservationId(), orderCode, 0L);
            paymentAttemptService.markStatus(orderCode, "PAID");
            Reservation updated = reservationRepository.findById(preparedReservation.getReservationId())
                    .orElse(preparedReservation);
            return reservationService.toResponse(updated);
        }

        // Case 2: Gateway Payment Required
        if (!payOSService.isConfigured()) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Cổng thanh toán PayOS chưa được kích hoạt");
        }

        try {
            PayOSResponse payosResp = payOSService.createBookingPaymentLink(
                    preparedReservation.getReservationId(),
                    orderCode,
                    customerPaid.longValue(),
                    "DATBAN " + orderCode
            );

            // Record gateway response into DB
            String realPaymentLinkId = payosResp.getPaymentLinkId();
            reservationService.updatePaymentDetails(
                    preparedReservation.getReservationId(),
                    payosResp.getCheckoutUrl(),
                    realPaymentLinkId
            );

            paymentAttemptService.recordGatewayResponse(
                    orderCode,
                    realPaymentLinkId,
                    payosResp.getCheckoutUrl(),
                    payosResp.getQrCode()
            );

            Reservation updated = reservationRepository.findById(preparedReservation.getReservationId())
                    .orElse(preparedReservation);
            return reservationService.toResponse(updated);

        } catch (Exception e) {
            // Network failure or gateway timeout:
            // The reservation and payment attempt are ALREADY durably saved in the DB.
            // Do NOT rollback or delete the reservation or release the locked voucher prematurely.
            log.error("PayOS gateway call failed/timed out for reservation {}, orderCode {}: {}",
                    preparedReservation.getReservationId(), orderCode, e.getMessage());
            paymentAttemptService.markStatus(orderCode, "UNKNOWN");
            throw new AppException(ErrorCode.INTERNAL_ERROR, "Không thể kết nối cổng thanh toán PayOS. Vui lòng thử lại sau.");
        }
    }
}
