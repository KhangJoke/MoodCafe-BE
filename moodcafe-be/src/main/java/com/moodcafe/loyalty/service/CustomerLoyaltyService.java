package com.moodcafe.loyalty.service;

import com.moodcafe.loyalty.abstraction.repository.CustomerWalletRepository;
import com.moodcafe.loyalty.abstraction.repository.PointLotRepository;
import com.moodcafe.loyalty.abstraction.repository.PointTransactionRepository;
import com.moodcafe.loyalty.dto.CustomerWalletResponse;
import com.moodcafe.loyalty.dto.PointAllocation;
import com.moodcafe.loyalty.dto.RefundPointCommand;
import com.moodcafe.loyalty.dto.RewardPointCommand;
import com.moodcafe.loyalty.entity.CustomerWallet;
import com.moodcafe.loyalty.entity.PointLot;
import com.moodcafe.loyalty.entity.PointTransaction;
import com.moodcafe.shared.error.ErrorCode;
import com.moodcafe.shared.exceptions.AppException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service quản lý tài khoản Loyalty và sổ cái điểm tích lũy MoodPoints của người dùng.
 * Quản lý tích điểm (thưởng), hoàn điểm, hạn sử dụng điểm và phân bổ nguồn điểm khi đổi voucher.
 */
@Slf4j
@Service
@Primary
@RequiredArgsConstructor
public class CustomerLoyaltyService {

    protected final CustomerWalletRepository customerWalletRepository;
    protected final PointLotRepository pointLotRepository;
    protected final PointTransactionRepository pointTransactionRepository;

    @Transactional(readOnly = true)
    public CustomerWalletResponse getLoyaltyAccount(UUID userId) {
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

    /** Alias for backwards compatibility */
    @Transactional(readOnly = true)
    public CustomerWalletResponse getWallet(UUID userId) {
        return getLoyaltyAccount(userId);
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
     * Hoàn tiền đặt bàn thành MoodPoints tỉ lệ 1:1 VNĐ. Không bao giờ hết hạn. Idempotent qua businessKey.
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

        // 2. Lock customer loyalty wallet
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
     * Cộng điểm thưởng hành vi (đặt bàn, làm nhiệm vụ). Mặc định hết hạn sau 365 ngày. Idempotent qua businessKey.
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

        // 2. Lock customer loyalty wallet
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
     * Phân bổ trừ điểm khi đổi voucher:
     * - Trừ lot REFUND trước (theo thứ tự cũ nhất).
     * - Sau đó trừ lot REWARD (theo thứ tự sắp hết hạn nhất).
     * - Ghi nhận chi tiết nguồn vốn (REFUND vs REWARD) phục vụ đối soát tài chính trợ giá voucher.
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

