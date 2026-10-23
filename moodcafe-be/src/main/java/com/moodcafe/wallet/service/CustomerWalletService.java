package com.moodcafe.wallet.service;

import com.moodcafe.shared.error.ErrorCode;
import com.moodcafe.shared.exceptions.AppException;
import com.moodcafe.wallet.abstraction.repository.CustomerWalletRepository;
import com.moodcafe.wallet.abstraction.repository.PointLotRepository;
import com.moodcafe.wallet.abstraction.repository.PointTransactionRepository;
import com.moodcafe.wallet.dto.CustomerWalletResponse;
import com.moodcafe.wallet.dto.PointAllocation;
import com.moodcafe.wallet.dto.RefundPointCommand;
import com.moodcafe.wallet.dto.RewardPointCommand;
import com.moodcafe.wallet.entity.CustomerWallet;
import com.moodcafe.wallet.entity.PointLot;
import com.moodcafe.wallet.entity.PointTransaction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerWalletService {

    private final CustomerWalletRepository customerWalletRepository;
    private final PointLotRepository pointLotRepository;
    private final PointTransactionRepository pointTransactionRepository;

    @Transactional(readOnly = true)
    public CustomerWalletResponse getWallet(UUID userId) {
        if (userId == null) {
            throw new AppException(ErrorCode.BAD_REQUEST, "User ID không được để trống");
        }
        Instant now = Instant.now();
        CustomerWallet wallet = customerWalletRepository.findByUserId(userId)
                .orElseGet(() -> CustomerWallet.builder()
                        .userId(userId)
                        .pointsBalance(0L)
                        .build());

        Long activePoints = pointLotRepository.sumActivePointsByUserId(userId, now);

        return CustomerWalletResponse.builder()
                .userId(userId)
                .pointsBalance(wallet.getPointsBalance())
                .conversionRateVnd(1L)
                .spendablePoints(activePoints != null ? activePoints : 0L)
                .build();
    }

    @Transactional
    public CustomerWallet getOrCreateWalletWithLock(UUID userId) {
        return customerWalletRepository.findByUserIdWithLock(userId)
                .orElseGet(() -> {
                    CustomerWallet newWallet = CustomerWallet.builder()
                            .userId(userId)
                            .pointsBalance(0L)
                            .build();
                    return customerWalletRepository.save(newWallet);
                });
    }

    /**
     * Credits 1:1 VND refund into MoodPoints. Never expires. Idempotent via businessKey.
     */
    @Transactional
    public PointTransaction creditRefundPoints(RefundPointCommand cmd) {
        if (cmd == null || cmd.getUserId() == null || cmd.getPoints() == null || cmd.getPoints() <= 0) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Số điểm hoàn phải lớn hơn 0");
        }

        // 1. Idempotency check
        Optional<PointTransaction> existing = pointTransactionRepository.findByBusinessKey(cmd.getBusinessKey());
        if (existing.isPresent()) {
            log.info("Idempotent creditRefundPoints duplicate ignored for key: {}", cmd.getBusinessKey());
            return existing.get();
        }

        // 2. Lock customer wallet
        CustomerWallet wallet = getOrCreateWalletWithLock(cmd.getUserId());

        // 3. Create REFUND lot (expiresAt is NULL -> never expires)
        PointLot lot = PointLot.builder()
                .userId(cmd.getUserId())
                .sourceType("REFUND")
                .sourceId(cmd.getRefundId())
                .issuedPoints(cmd.getPoints())
                .remainingPoints(cmd.getPoints())
                .expiresAt(null)
                .build();
        pointLotRepository.save(lot);

        // 4. Update wallet balance
        long newBalance = wallet.getPointsBalance() + cmd.getPoints();
        wallet.setPointsBalance(newBalance);
        customerWalletRepository.save(wallet);

        // 5. Append immutable transaction
        PointTransaction tx = PointTransaction.builder()
                .businessKey(cmd.getBusinessKey())
                .userId(cmd.getUserId())
                .amount(cmd.getPoints())
                .balanceAfter(newBalance)
                .txType("REFUND_CREDIT")
                .description(cmd.getDescription() != null ? cmd.getDescription() : "Hoàn tiền đơn đặt bàn thành MoodPoints (1:1)")
                .build();

        return pointTransactionRepository.save(tx);
    }

    /**
     * Credits reward / loyalty MoodPoints. Expires in 365 days by default. Idempotent via businessKey.
     */
    @Transactional
    public PointTransaction creditRewardPoints(RewardPointCommand cmd) {
        if (cmd == null || cmd.getUserId() == null || cmd.getPoints() == null || cmd.getPoints() <= 0) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Số điểm thưởng phải lớn hơn 0");
        }

        // 1. Idempotency check
        Optional<PointTransaction> existing = pointTransactionRepository.findByBusinessKey(cmd.getBusinessKey());
        if (existing.isPresent()) {
            log.info("Idempotent creditRewardPoints duplicate ignored for key: {}", cmd.getBusinessKey());
            return existing.get();
        }

        // 2. Lock customer wallet
        CustomerWallet wallet = getOrCreateWalletWithLock(cmd.getUserId());

        Instant expiresAt = cmd.getExpiresAt() != null
                ? cmd.getExpiresAt()
                : Instant.now().plus(365, ChronoUnit.DAYS);

        // 3. Create REWARD lot
        PointLot lot = PointLot.builder()
                .userId(cmd.getUserId())
                .sourceType("REWARD")
                .sourceId(cmd.getSourceId())
                .issuedPoints(cmd.getPoints())
                .remainingPoints(cmd.getPoints())
                .expiresAt(expiresAt)
                .build();
        pointLotRepository.save(lot);

        // 4. Update wallet balance
        long newBalance = wallet.getPointsBalance() + cmd.getPoints();
        wallet.setPointsBalance(newBalance);
        customerWalletRepository.save(wallet);

        // 5. Append immutable transaction
        PointTransaction tx = PointTransaction.builder()
                .businessKey(cmd.getBusinessKey())
                .userId(cmd.getUserId())
                .amount(cmd.getPoints())
                .balanceAfter(newBalance)
                .txType("REWARD_CREDIT")
                .description(cmd.getDescription() != null ? cmd.getDescription() : "Điểm thưởng hành vi " + cmd.getRewardType())
                .build();

        return pointTransactionRepository.save(tx);
    }

    /**
     * Atomically allocates points to redeem a voucher:
     * - Consumes REFUND lots first (oldest first).
     * - Then consumes REWARD lots (earliest expiring first).
     * - Deducts remaining points from lots and decrements wallet balance.
     * - Records allocation split (refund vs reward) for accurate financial subsidy tracking.
     */
    @Transactional
    public PointAllocation allocatePoints(UUID userId, Long pointsToAllocate, String businessKey, String description) {
        if (pointsToAllocate == null || pointsToAllocate <= 0) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Số điểm cần đổi voucher phải lớn hơn 0");
        }

        CustomerWallet wallet = getOrCreateWalletWithLock(userId);
        if (wallet.getPointsBalance() < pointsToAllocate) {
            throw new AppException(ErrorCode.BAD_REQUEST,
                    "Số dư MoodPoints không đủ để đổi voucher (yêu cầu " + pointsToAllocate + " điểm, hiện có " + wallet.getPointsBalance() + " điểm)");
        }

        Instant now = Instant.now();
        List<PointLot> lots = pointLotRepository.findSpendableLotsWithLock(userId, now);

        long remainingNeeded = pointsToAllocate;
        long refundAllocated = 0L;
        long rewardAllocated = 0L;
        List<PointAllocation.LotAllocationItem> allocationItems = new ArrayList<>();

        for (PointLot lot : lots) {
            if (remainingNeeded <= 0) break;
            long take = Math.min(lot.getRemainingPoints(), remainingNeeded);
            lot.setRemainingPoints(lot.getRemainingPoints() - take);
            pointLotRepository.save(lot);

            if ("REFUND".equalsIgnoreCase(lot.getSourceType())) {
                refundAllocated += take;
            } else {
                rewardAllocated += take;
            }

            allocationItems.add(PointAllocation.LotAllocationItem.builder()
                    .lotId(lot.getLotId())
                    .allocatedPoints(take)
                    .fundingType(lot.getSourceType())
                    .build());

            remainingNeeded -= take;
        }

        if (remainingNeeded > 0) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Không đủ điểm khả dụng hợp lệ để hoàn tất đổi voucher");
        }

        long newBalance = wallet.getPointsBalance() - pointsToAllocate;
        wallet.setPointsBalance(newBalance);
        customerWalletRepository.save(wallet);

        PointTransaction tx = PointTransaction.builder()
                .businessKey(businessKey)
                .userId(userId)
                .amount(-pointsToAllocate)
                .balanceAfter(newBalance)
                .txType("REDEEM_DEBIT")
                .description(description != null ? description : "Đổi MoodPoints lấy Voucher")
                .build();
        pointTransactionRepository.save(tx);

        return PointAllocation.builder()
                .totalAllocatedPoints(pointsToAllocate)
                .refundPointsAllocated(refundAllocated)
                .rewardPointsAllocated(rewardAllocated)
                .items(allocationItems)
                .build();
    }
}
