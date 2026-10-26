package com.moodcafe.voucher.service;

import com.moodcafe.shared.error.ErrorCode;
import com.moodcafe.shared.exceptions.AppException;
import com.moodcafe.voucher.abstraction.repository.CustomerVoucherRepository;
import com.moodcafe.voucher.abstraction.repository.VoucherPointAllocationRepository;
import com.moodcafe.voucher.entity.CustomerVoucher;
import com.moodcafe.wallet.dto.RefundPointCommand;
import com.moodcafe.wallet.service.CustomerWalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class VoucherLifecycleService {

    private final CustomerVoucherRepository customerVoucherRepository;
    private final VoucherPointAllocationRepository allocationRepository;
    private final CustomerWalletService customerWalletService;

    /**
     * Locks a voucher when creating a reservation order awaiting payment.
     */
    @Transactional
    public void lockVoucher(UUID userId, UUID voucherId, UUID reservationId, Instant holdUntil) {
        CustomerVoucher voucher = customerVoucherRepository.findByIdWithLock(voucherId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Không tìm thấy voucher"));

        if (!voucher.getUserId().equals(userId)) {
            throw new AppException(ErrorCode.FORBIDDEN, "Bạn không có quyền sử dụng voucher này");
        }

        if (!"AVAILABLE".equalsIgnoreCase(voucher.getStatus())) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Voucher không ở trạng thái khả dụng để áp dụng");
        }

        if (voucher.getExpiresAt().isBefore(Instant.now())) {
            voucher.setStatus("EXPIRED");
            customerVoucherRepository.save(voucher);
            throw new AppException(ErrorCode.BAD_REQUEST, "Voucher đã hết hạn sử dụng");
        }

        voucher.setStatus("LOCKED");
        voucher.setLockedReservationId(reservationId);
        voucher.setHoldUntil(holdUntil != null ? holdUntil : Instant.now().plusSeconds(900)); // 15 mins hold
        customerVoucherRepository.save(voucher);
    }

    /**
     * Captures a locked voucher when reservation payment succeeds.
     */
    @Transactional
    public void captureVoucher(UUID reservationId) {
        Optional<CustomerVoucher> opt = customerVoucherRepository.findByLockedBookingIdWithLock(reservationId)
                .or(() -> customerVoucherRepository.findByLockedReservationIdWithLock(reservationId));
        if (opt.isPresent()) {
            CustomerVoucher voucher = opt.get();
            if ("USED".equalsIgnoreCase(voucher.getStatus())) {
                log.info("Voucher {} already in terminal USED state for reservation {}", voucher.getVoucherId(), reservationId);
                return;
            }
            if (!"LOCKED".equalsIgnoreCase(voucher.getStatus())) {
                log.warn("Cannot capture voucher {} in non-LOCKED state: {}", voucher.getVoucherId(), voucher.getStatus());
                return;
            }
            voucher.setStatus("USED");
            voucher.setUsedAt(Instant.now());
            voucher.setHoldUntil(null);
            customerVoucherRepository.save(voucher);
            log.info("Voucher {} captured (USED) for reservation {}", voucher.getVoucherId(), reservationId);
        }
    }

    /**
     * Releases a locked voucher back to AVAILABLE (e.g. payment failed / cancelled before confirmation).
     * Invariant: Never transition a USED voucher back to AVAILABLE!
     */
    @Transactional
    public void releaseVoucher(UUID reservationId) {
        Optional<CustomerVoucher> opt = customerVoucherRepository.findByLockedBookingIdWithLock(reservationId)
                .or(() -> customerVoucherRepository.findByLockedReservationIdWithLock(reservationId));
        if (opt.isPresent()) {
            CustomerVoucher voucher = opt.get();
            // Strict invariant check: a USED voucher must NEVER be released to AVAILABLE
            if ("USED".equalsIgnoreCase(voucher.getStatus())) {
                log.warn("Safety trigger: Voucher {} for reservation {} is already USED. Denying release to AVAILABLE.",
                        voucher.getVoucherId(), reservationId);
                return;
            }
            if (!"LOCKED".equalsIgnoreCase(voucher.getStatus())) {
                log.warn("Safety trigger: Voucher {} for reservation {} is in status {}, not LOCKED. Denying release.",
                        voucher.getVoucherId(), reservationId, voucher.getStatus());
                return;
            }

            voucher.setLockedBookingId(null);
            voucher.setLockedReservationId(null);
            voucher.setHoldUntil(null);

            if (voucher.getExpiresAt().isAfter(Instant.now())) {
                voucher.setStatus("AVAILABLE");
            } else {
                voucher.setStatus("EXPIRED");
                // Expired voucher refund protection
                refundExpiredVoucherIfEligible(voucher, "RELEASE_EXPIRED_" + reservationId);
            }
            customerVoucherRepository.save(voucher);
            log.info("Voucher {} released for reservation {}", voucher.getVoucherId(), reservationId);
        }
    }

    /**
     * Restores voucher when reservation is cancelled within free cancel hours:
     * - If voucher still has valid expiry -> AVAILABLE.
     * - If voucher has expired (Policy P3) -> EXPIRED, and refund its REFUND-funded points back into MoodPoints wallet!
     */
    @Transactional
    public void restoreVoucherForRefund(UUID refundId, UUID voucherId) {
        CustomerVoucher voucher = customerVoucherRepository.findByIdWithLock(voucherId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Không tìm thấy voucher"));

        voucher.setLockedReservationId(null);
        voucher.setLockedBookingId(null);
        voucher.setHoldUntil(null);
        voucher.setUsedAt(null);

        if (voucher.getExpiresAt().isAfter(Instant.now())) {
            voucher.setStatus("AVAILABLE");
            customerVoucherRepository.save(voucher);
            log.info("Voucher {} restored to AVAILABLE for refund {}", voucherId, refundId);
        } else {
            voucher.setStatus("EXPIRED");
            customerVoucherRepository.save(voucher);
            refundExpiredVoucherIfEligible(voucher, "REFUND_EXPIRED_VOUCHER_" + refundId);
        }
    }

    private void refundExpiredVoucherIfEligible(CustomerVoucher voucher, String businessKey) {
        Long refundPoints = allocationRepository.sumAllocatedPointsByVoucherIdAndFundingType(voucher.getVoucherId(), "REFUND");
        if (refundPoints != null && refundPoints > 0) {
            log.info("Refunding {} points from expired REFUND-backed voucher {} to user {}",
                    refundPoints, voucher.getVoucherId(), voucher.getUserId());
            customerWalletService.creditRefundPoints(RefundPointCommand.builder()
                    .userId(voucher.getUserId())
                    .points(refundPoints)
                    .refundId(voucher.getVoucherId().toString())
                    .businessKey("REFUND_EXPIRED_VOUCHER_" + voucher.getVoucherId())
                    .description("Hoàn lại MoodPoints từ voucher hết hạn chưa sử dụng (bảo toàn tiền hoàn)")
                    .build());
        }
    }
}
