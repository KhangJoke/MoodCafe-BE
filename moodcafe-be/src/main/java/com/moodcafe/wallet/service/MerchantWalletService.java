package com.moodcafe.wallet.service;

import com.moodcafe.shared.error.ErrorCode;
import com.moodcafe.shared.exceptions.AppException;
import com.moodcafe.wallet.abstraction.repository.MerchantWalletRepository;
import com.moodcafe.wallet.abstraction.repository.MerchantWalletTransactionRepository;
import com.moodcafe.wallet.entity.MerchantWallet;
import com.moodcafe.wallet.entity.MerchantWalletTransaction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MerchantWalletService {

    private final MerchantWalletRepository merchantWalletRepository;
    private final MerchantWalletTransactionRepository merchantWalletTransactionRepository;

    @Transactional(readOnly = true)
    public MerchantWallet getWallet(UUID storeId) {
        if (storeId == null) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Store ID không được để trống");
        }
        return merchantWalletRepository.findByStoreId(storeId)
                .orElseGet(() -> MerchantWallet.builder()
                        .storeId(storeId)
                        .pendingBalance(BigDecimal.ZERO)
                        .availableBalance(BigDecimal.ZERO)
                        .lockedBalance(BigDecimal.ZERO)
                        .build());
    }

    @Transactional
    public MerchantWallet getOrCreateWalletWithLock(UUID storeId) {
        return merchantWalletRepository.findByStoreIdWithLock(storeId)
                .orElseGet(() -> {
                    MerchantWallet wallet = MerchantWallet.builder()
                            .storeId(storeId)
                            .pendingBalance(BigDecimal.ZERO)
                            .availableBalance(BigDecimal.ZERO)
                            .lockedBalance(BigDecimal.ZERO)
                            .build();
                    return merchantWalletRepository.save(wallet);
                });
    }

    /**
     * When customer pays reservation: hold merchant_net_amount in pending_balance.
     */
    @Transactional
    public void holdPending(UUID storeId, BigDecimal amount, String referenceId, String description) {
        holdPending(storeId, amount, "RESERVATION", referenceId, description);
    }

    @Transactional
    public void holdPending(UUID storeId, BigDecimal amount, String referenceType, String referenceId, String description) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) return;
        BigDecimal validAmount = amount.setScale(2, RoundingMode.HALF_UP);

        MerchantWallet wallet = getOrCreateWalletWithLock(storeId);
        wallet.setPendingBalance(wallet.getPendingBalance().add(validAmount));
        merchantWalletRepository.save(wallet);

        MerchantWalletTransaction tx = MerchantWalletTransaction.builder()
                .walletId(wallet.getWalletId())
                .amount(validAmount)
                .balanceType("PENDING")
                .txType("HOLD_PENDING")
                .referenceType(referenceType != null ? referenceType : "RESERVATION")
                .referenceId(referenceId)
                .description(description != null ? description : "Tạm giữ doanh thu đơn đặt bàn chờ phục vụ")
                .build();
        merchantWalletTransactionRepository.save(tx);
    }

    /**
     * When reservation is COMPLETED, CLAIMED_LATE, or EXPIRED_NO_SHOW:
     * move funds from pending_balance -> available_balance.
     */
    @Transactional
    public void settleToAvailable(UUID storeId, BigDecimal amount, String referenceId, String description) {
        settleToAvailable(storeId, amount, "RESERVATION", referenceId, description);
    }

    @Transactional
    public void settleToAvailable(UUID storeId, BigDecimal amount, String referenceType, String referenceId, String description) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) return;
        BigDecimal validAmount = amount.setScale(2, RoundingMode.HALF_UP);

        MerchantWallet wallet = getOrCreateWalletWithLock(storeId);
        if (wallet.getPendingBalance().compareTo(validAmount) < 0) {
            log.error("CRITICAL: Store {} pending balance {} is less than settle amount {}. Rolling back to prevent unbacked balance creation.",
                    storeId, wallet.getPendingBalance(), validAmount);
            throw new AppException(ErrorCode.BAD_REQUEST,
                    "Không thể giải ngân: Số dư tạm giữ (" + wallet.getPendingBalance() + "đ) nhỏ hơn số tiền giải ngân (" + validAmount + "đ)");
        }

        wallet.setPendingBalance(wallet.getPendingBalance().subtract(validAmount));
        wallet.setAvailableBalance(wallet.getAvailableBalance().add(validAmount));
        merchantWalletRepository.save(wallet);

        MerchantWalletTransaction tx = MerchantWalletTransaction.builder()
                .walletId(wallet.getWalletId())
                .amount(validAmount)
                .balanceType("AVAILABLE")
                .txType("RELEASE_TO_AVAILABLE")
                .referenceType(referenceType != null ? referenceType : "RESERVATION")
                .referenceId(referenceId)
                .description(description != null ? description : "Giải ngân doanh thu đơn đặt bàn sang số dư khả dụng")
                .build();
        merchantWalletTransactionRepository.save(tx);
    }

    /**
     * When reservation is cancelled before service: release pending_balance.
     */
    @Transactional
    public void revertPending(UUID storeId, BigDecimal amount, String referenceId, String description) {
        revertPending(storeId, amount, "RESERVATION", referenceId, description);
    }

    @Transactional
    public void revertPending(UUID storeId, BigDecimal amount, String referenceType, String referenceId, String description) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) return;
        BigDecimal validAmount = amount.setScale(2, RoundingMode.HALF_UP);

        MerchantWallet wallet = getOrCreateWalletWithLock(storeId);
        if (wallet.getPendingBalance().compareTo(validAmount) < 0) {
            log.error("CRITICAL: Store {} pending balance {} is less than revert amount {}. Refusing unbacked reversal.",
                    storeId, wallet.getPendingBalance(), validAmount);
            throw new AppException(ErrorCode.BAD_REQUEST,
                    "Không thể đảo tiền tạm giữ: Số dư tạm giữ (" + wallet.getPendingBalance() + "đ) nhỏ hơn số tiền cần đảo (" + validAmount + "đ)");
        }

        wallet.setPendingBalance(wallet.getPendingBalance().subtract(validAmount));
        merchantWalletRepository.save(wallet);

        MerchantWalletTransaction tx = MerchantWalletTransaction.builder()
                .walletId(wallet.getWalletId())
                .amount(validAmount.negate())
                .balanceType("PENDING")
                .txType("REVERSAL")
                .referenceType(referenceType != null ? referenceType : "RESERVATION")
                .referenceId(referenceId)
                .description(description != null ? description : "Đảo hoàn tiền tạm giữ do đơn bị hủy")
                .build();
        merchantWalletTransactionRepository.save(tx);
    }

    /**
     * Locks funds from available_balance -> locked_balance when owner requests withdrawal.
     */
    @Transactional
    public void lockForWithdrawal(UUID storeId, BigDecimal amount, String withdrawalId) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Số tiền rút phải lớn hơn 0");
        }
        BigDecimal validAmount = amount.setScale(2, RoundingMode.HALF_UP);

        MerchantWallet wallet = getOrCreateWalletWithLock(storeId);
        if (wallet.getAvailableBalance().compareTo(validAmount) < 0) {
            throw new AppException(ErrorCode.BAD_REQUEST,
                    "Số dư khả dụng không đủ để rút (khả dụng: " + wallet.getAvailableBalance() + "đ, yêu cầu: " + validAmount + "đ)");
        }

        wallet.setAvailableBalance(wallet.getAvailableBalance().subtract(validAmount));
        wallet.setLockedBalance(wallet.getLockedBalance().add(validAmount));
        merchantWalletRepository.save(wallet);

        MerchantWalletTransaction tx = MerchantWalletTransaction.builder()
                .walletId(wallet.getWalletId())
                .amount(validAmount)
                .balanceType("LOCKED")
                .txType("LOCK_FOR_WITHDRAWAL")
                .referenceType("WITHDRAWAL")
                .referenceId(withdrawalId)
                .description("Tạm khóa số dư chờ Admin chuyển khoản rút tiền")
                .build();
        merchantWalletTransactionRepository.save(tx);
    }

    /**
     * When Admin confirms payment: debit locked_balance.
     */
    @Transactional
    public void completeWithdrawal(UUID storeId, BigDecimal amount, String withdrawalId) {
        BigDecimal validAmount = amount.setScale(2, RoundingMode.HALF_UP);
        MerchantWallet wallet = getOrCreateWalletWithLock(storeId);

        if (wallet.getLockedBalance().compareTo(validAmount) < 0) {
            log.error("CRITICAL: Store {} locked balance {} is less than complete withdrawal amount {}.",
                    storeId, wallet.getLockedBalance(), validAmount);
            throw new AppException(ErrorCode.BAD_REQUEST,
                    "Không thể hoàn tất rút tiền: Số dư đang khóa (" + wallet.getLockedBalance() + "đ) nhỏ hơn số tiền rút (" + validAmount + "đ)");
        }
        wallet.setLockedBalance(wallet.getLockedBalance().subtract(validAmount));
        wallet.setTotalWithdrawn(wallet.getTotalWithdrawn().add(validAmount));
        merchantWalletRepository.save(wallet);

        MerchantWalletTransaction tx = MerchantWalletTransaction.builder()
                .walletId(wallet.getWalletId())
                .amount(validAmount.negate())
                .balanceType("LOCKED")
                .txType("WITHDRAWAL_PAID")
                .referenceType("WITHDRAWAL")
                .referenceId(withdrawalId)
                .description("Đã chuyển khoản rút tiền thành công")
                .build();
        merchantWalletTransactionRepository.save(tx);
    }

    /**
     * When Admin rejects or cancels withdrawal: unlock from locked_balance -> available_balance.
     */
    @Transactional
    public void unlockWithdrawal(UUID storeId, BigDecimal amount, String withdrawalId, String reason) {
        BigDecimal validAmount = amount.setScale(2, RoundingMode.HALF_UP);
        MerchantWallet wallet = getOrCreateWalletWithLock(storeId);

        if (wallet.getLockedBalance().compareTo(validAmount) < 0) {
            log.error("CRITICAL: Store {} locked balance {} is less than unlock amount {}.",
                    storeId, wallet.getLockedBalance(), validAmount);
            throw new AppException(ErrorCode.BAD_REQUEST,
                    "Không thể mở khóa rút tiền: Số dư đang khóa (" + wallet.getLockedBalance() + "đ) nhỏ hơn số tiền mở khóa (" + validAmount + "đ)");
        }
        wallet.setLockedBalance(wallet.getLockedBalance().subtract(validAmount));
        wallet.setAvailableBalance(wallet.getAvailableBalance().add(validAmount));
        merchantWalletRepository.save(wallet);

        MerchantWalletTransaction tx = MerchantWalletTransaction.builder()
                .walletId(wallet.getWalletId())
                .amount(validAmount)
                .balanceType("AVAILABLE")
                .txType("WITHDRAWAL_CANCELLED")
                .referenceType("WITHDRAWAL")
                .referenceId(withdrawalId)
                .description("Hoàn trả số dư do lệnh rút bị hủy: " + (reason != null ? reason : ""))
                .build();
        merchantWalletTransactionRepository.save(tx);
    }
}
