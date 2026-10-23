package com.moodcafe.finance.service;

import com.moodcafe.loyalty.service.LoyaltyRewardService;
import com.moodcafe.reservation.abstraction.repository.ReservationRepository;
import com.moodcafe.reservation.entity.Reservation;
import com.moodcafe.shared.error.ErrorCode;
import com.moodcafe.shared.exceptions.AppException;
import com.moodcafe.wallet.service.MerchantWalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationSettlementService {

    private final ReservationRepository reservationRepository;
    private final MerchantWalletService merchantWalletService;
    private final LoyaltyRewardService loyaltyRewardService;

    /**
     * Settles reservation upon service completion, late claim, grace expiry, or closing no-show.
     */
    @Transactional
    public Reservation settleReservation(UUID reservationId, String eventType) {
        Reservation reservation = reservationRepository.findByIdWithLock(reservationId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Không tìm thấy đơn đặt bàn"));

        String currentStatus = reservation.getStatus();

        switch (eventType) {
            case "TABLE_EXPIRED" -> {
                // Table grace period expired -> release physical table, but KEEP funds in pending (customer still has drink right)
                if ("PAID_CONFIRMED".equalsIgnoreCase(currentStatus)) {
                    reservation.setStatus("TABLE_EXPIRED");
                    log.info("Reservation {} reached grace period. Status set to TABLE_EXPIRED, pending retained.", reservationId);
                }
            }
            case "CLAIMED_LATE" -> {
                // Customer claims drinks after table expired
                if ("PAID_CONFIRMED".equalsIgnoreCase(currentStatus) || "TABLE_EXPIRED".equalsIgnoreCase(currentStatus)) {
                    reservation.setStatus("CLAIMED_LATE");
                    reservation.setClaimedLateTime(Instant.now());

                    // Settle net amount to merchant available balance
                    merchantWalletService.settleToAvailable(
                            reservation.getStoreId(),
                            reservation.getMerchantNetAmount(),
                            reservationId.toString(),
                            "Giải ngân đơn đặt bàn nhận nước trễ #" + reservationId
                    );

                    // Award SPEND points only (no on-time bonus)
                    loyaltyRewardService.awardSpendReward(reservation);
                }
            }
            case "COMPLETED" -> {
                // Normal completed service
                if ("PAID_CONFIRMED".equalsIgnoreCase(currentStatus) || "CHECKED_IN".equalsIgnoreCase(currentStatus)) {
                    reservation.setStatus("COMPLETED");
                    reservation.setCheckOutTime(Instant.now());

                    merchantWalletService.settleToAvailable(
                            reservation.getStoreId(),
                            reservation.getMerchantNetAmount(),
                            reservationId.toString(),
                            "Giải ngân đơn đặt bàn hoàn tất phục vụ #" + reservationId
                    );

                    loyaltyRewardService.awardSpendReward(reservation);
                }
            }
            case "EXPIRED_NO_SHOW" -> {
                // End of service day: customer never showed up
                if ("PAID_CONFIRMED".equalsIgnoreCase(currentStatus) || "TABLE_EXPIRED".equalsIgnoreCase(currentStatus)) {
                    reservation.setStatus("EXPIRED_NO_SHOW");

                    // Merchant still receives full net guaranteed settlement
                    merchantWalletService.settleToAvailable(
                            reservation.getStoreId(),
                            reservation.getMerchantNetAmount(),
                            reservationId.toString(),
                            "Giải ngân đơn khách không đến (No-show) #" + reservationId
                    );
                    // Customer gets 0 points for no-show
                }
            }
            default -> throw new AppException(ErrorCode.BAD_REQUEST, "Sự kiện thanh quyết toán không hợp lệ: " + eventType);
        }

        return reservationRepository.save(reservation);
    }
}
