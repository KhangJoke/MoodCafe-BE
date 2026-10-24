package com.moodcafe.wallet.service;

import com.moodcafe.shared.error.ErrorCode;
import com.moodcafe.shared.exceptions.AppException;
import com.moodcafe.configuration.abstraction.repository.SystemConfigurationRepository;
import com.moodcafe.wallet.abstraction.repository.StoreBankAccountRepository;
import com.moodcafe.wallet.abstraction.repository.WithdrawalRequestRepository;
import com.moodcafe.wallet.entity.StoreBankAccount;
import com.moodcafe.wallet.entity.WithdrawalRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class WithdrawalService {

    public static final String MIN_WITHDRAWAL_CONFIG_KEY = "MIN_WITHDRAWAL_AMOUNT_VND";
    public static final BigDecimal DEFAULT_MIN_WITHDRAWAL_AMOUNT = new BigDecimal("100000");

    private final WithdrawalRequestRepository withdrawalRequestRepository;
    private final SystemConfigurationRepository configRepository;
    private final StoreBankAccountRepository bankAccountRepository;
    private final MerchantWalletService merchantWalletService;
    private final StoreFinancialSecurityService financialSecurityService;
    private final PayoutBreakdownService breakdownService;
    private final com.moodcafe.notification.abstraction.service.NotificationDispatcherService notifications;

    /**
     * Store OWNER creates a payout request from available balance:
     * - Verifies active OWNER role.
     * - Minimum is controlled by Admin system configuration (default 100.000 VND).
     * - Enforces bank account belongs to this store and is not deleted.
     * - Idempotency support: identical key + payload returns existing request; different payload throws 409 CONFLICT.
     * - Atomically shifts funds from available_balance -> locked_balance.
     * - Snapshots destination bank account.
     */
    @Transactional
    public WithdrawalRequest requestWithdrawal(UUID ownerId, UUID storeId, BigDecimal amount, UUID bankAccountId, String idempotencyKey) {
        // Enforce OWNER security boundary at service level
        financialSecurityService.requireActiveStoreOwner(storeId);

        if (amount == null) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Số tiền rút không hợp lệ");
        }
        BigDecimal validAmount = amount.setScale(2, RoundingMode.HALF_UP);
        String payloadHash = calculatePayloadHash(storeId, validAmount, bankAccountId);

        // Check idempotency if key provided
        if (idempotencyKey != null && !idempotencyKey.trim().isEmpty()) {
            Optional<WithdrawalRequest> existingOpt = withdrawalRequestRepository.findByStoreIdAndIdempotencyKey(storeId, idempotencyKey);
            if (existingOpt.isPresent()) {
                WithdrawalRequest existing = existingOpt.get();
                if (payloadHash.equals(existing.getPayloadHash())) {
                    log.info("Idempotent requestWithdrawal match for store {} with key {}", storeId, idempotencyKey);
                    return existing;
                } else {
                    throw new AppException(ErrorCode.CONFLICT, "Idempotency key đã được sử dụng với thông tin rút tiền khác");
                }
            }
        }

        BigDecimal minimumAmount = getMinimumWithdrawalAmount();
        if (validAmount.compareTo(minimumAmount) < 0) {
            throw new AppException(ErrorCode.BAD_REQUEST,
                    "Số tiền rút tối thiểu là " + minimumAmount.setScale(0, RoundingMode.UNNECESSARY) + "đ");
        }

        StoreBankAccount bankAccount;
        if (bankAccountId != null) {
            bankAccount = bankAccountRepository.findById(bankAccountId)
                    .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Không tìm thấy thông tin tài khoản ngân hàng"));
            if (!storeId.equals(bankAccount.getStoreId()) || Boolean.TRUE.equals(bankAccount.getIsDeleted())) {
                throw new AppException(ErrorCode.FORBIDDEN_STORE_ACCESS, "Tài khoản ngân hàng không hợp lệ hoặc không thuộc cơ sở này");
            }
        } else {
            bankAccount = bankAccountRepository.findByStoreIdAndIsDefaultTrueAndIsDeletedFalse(storeId)
                    .orElseThrow(() -> new AppException(ErrorCode.BAD_REQUEST, "Cơ sở chưa thiết lập tài khoản ngân hàng mặc định"));
        }

        // Persist first so Hibernate generates the UUID and initializes the version.
        // The wallet lock and attribution remain in this transaction and roll back together.
        WithdrawalRequest request = WithdrawalRequest.builder()
                .storeId(storeId)
                .ownerId(ownerId)
                .amount(validAmount)
                .bankName(bankAccount.getBankName())
                .bankCode(bankAccount.getBankCode())
                .accountNumber(bankAccount.getAccountNumber())
                .accountHolderName(bankAccount.getAccountHolderName())
                .idempotencyKey(idempotencyKey)
                .payloadHash(payloadHash)
                .status("REQUESTED")
                .build();

        request = withdrawalRequestRepository.saveAndFlush(request);
        merchantWalletService.lockForWithdrawal(storeId, validAmount, request.getWithdrawalId().toString());
        breakdownService.snapshot(request);
        return request;
    }

    private BigDecimal getMinimumWithdrawalAmount() {
        return configRepository.findByConfigKey(MIN_WITHDRAWAL_CONFIG_KEY)
                .map(config -> new BigDecimal(config.getConfigValue()))
                .orElse(DEFAULT_MIN_WITHDRAWAL_AMOUNT);
    }

    /**
     * Backward-compatible overload without idempotency key
     */
    @Transactional
    public WithdrawalRequest requestWithdrawal(UUID ownerId, UUID storeId, BigDecimal amount, UUID bankAccountId) {
        return requestWithdrawal(ownerId, storeId, amount, bankAccountId, null);
    }

    /**
     * Admin claims a withdrawal request: REQUESTED -> PROCESSING.
     */
    @Transactional
    public WithdrawalRequest claimWithdrawal(UUID adminId, UUID withdrawalId) {
        WithdrawalRequest request = withdrawalRequestRepository.findByIdWithLock(withdrawalId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Không tìm thấy yêu cầu rút tiền"));

        if (!"REQUESTED".equalsIgnoreCase(request.getStatus())) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Chỉ có thể tiếp nhận lệnh ở trạng thái REQUESTED");
        }

        request.setStatus("PROCESSING");
        request.setClaimedBy(adminId);
        request.setClaimedAt(Instant.now());
        request.setAdminId(adminId);
        if ("PAID".equals(request.getStatus()) || "REJECTED".equals(request.getStatus())) notifyOwnerAfterCommit(request);
        return withdrawalRequestRepository.save(request);
    }

    /**
     * Admin confirms payment made via VietQR/bank transfer: PROCESSING -> PAID.
     * Debits locked_balance.
     * Supports idempotent retry if already confirmed with identical reference.
     */
    @Transactional
    public WithdrawalRequest confirmPaid(UUID adminId, UUID withdrawalId, String transactionReference, String evidenceImageUrl) {
        WithdrawalRequest request = withdrawalRequestRepository.findByIdWithLock(withdrawalId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Không tìm thấy yêu cầu rút tiền"));

        // Validate transaction reference
        if (transactionReference == null || transactionReference.trim().isEmpty()) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Mã tham chiếu ngân hàng không được để trống");
        }
        String cleanRef = transactionReference.trim();

        // Idempotency: if already PAID with the same transaction reference, return existing record
        if ("PAID".equalsIgnoreCase(request.getStatus())) {
            if (cleanRef.equalsIgnoreCase(request.getTransactionReference())) {
                log.info("Idempotent confirmPaid replay for withdrawal {} with ref {}", withdrawalId, cleanRef);
                return request;
            } else {
                throw new AppException(ErrorCode.BAD_REQUEST, "Lệnh này đã được thanh toán với mã tham chiếu khác");
            }
        }

        if (!"PROCESSING".equalsIgnoreCase(request.getStatus())) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Lệnh phải được tiếp nhận xử lý (PROCESSING) trước khi xác nhận");
        }

        // Prevent cross-admin interference during bank transfer
        if (request.getClaimedBy() != null && !request.getClaimedBy().equals(adminId)) {
            throw new AppException(ErrorCode.FORBIDDEN, "Lệnh này đang được tiếp nhận bởi một Admin khác");
        }

        // Prevent self-approval (Owner who also has Admin role approving own payout)
        if (request.getOwnerId().equals(adminId)) {
            throw new AppException(ErrorCode.FORBIDDEN, "Không được tự duyệt lệnh rút tiền của chính mình");
        }

        // Enforce uniqueness across all withdrawals
        Optional<WithdrawalRequest> existingByRef = withdrawalRequestRepository.findByTransactionReferenceIgnoreCase(cleanRef);
        if (existingByRef.isPresent() && !existingByRef.get().getWithdrawalId().equals(withdrawalId)) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Mã tham chiếu ngân hàng này đã được sử dụng cho một lệnh khác");
        }

        // Debit locked funds permanently
        merchantWalletService.completeWithdrawal(
                request.getStoreId(),
                request.getAmount(),
                withdrawalId.toString()
        );

        request.setStatus("PAID");
        request.setAdminId(adminId);
        request.setTransactionReference(cleanRef);
        request.setEvidenceImageUrl(evidenceImageUrl);
        request.setProcessedAt(Instant.now());

        if ("PAID".equals(request.getStatus()) || "REJECTED".equals(request.getStatus())) notifyOwnerAfterCommit(request);
        return withdrawalRequestRepository.save(request);
    }

    /**
     * Admin rejects withdrawal:
     * - Only REQUESTED can be rejected straightforwardly.
     * - If PROCESSING: must be marked RECONCILIATION_REQUIRED first to prevent premature refund while money might leave bank.
     * - Unlocks funds from locked_balance -> available_balance.
     */
    @Transactional
    public WithdrawalRequest rejectWithdrawal(UUID adminId, UUID withdrawalId, String reason) {
        WithdrawalRequest request = withdrawalRequestRepository.findByIdWithLock(withdrawalId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Không tìm thấy yêu cầu rút tiền"));

        if ("PAID".equalsIgnoreCase(request.getStatus()) || "REJECTED".equalsIgnoreCase(request.getStatus())) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Không thể từ chối lệnh rút tiền đã đóng");
        }

        if ("RECONCILIATION_REQUIRED".equalsIgnoreCase(request.getStatus())) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Lệnh đang trong trạng thái đối soát, vui lòng sử dụng tính năng giải quyết đối soát");
        }

        if ("PROCESSING".equalsIgnoreCase(request.getStatus())) {
            throw new AppException(ErrorCode.BAD_REQUEST,
                    "Lệnh đang trong quá trình chuyển khoản ngân hàng. Vui lòng chuyển sang đối soát (RECONCILIATION_REQUIRED) trước khi hoàn tiền để tránh mất mát kép.");
        }

        // Return locked funds back to available
        merchantWalletService.unlockWithdrawal(
                request.getStoreId(),
                request.getAmount(),
                withdrawalId.toString(),
                reason
        );

        request.setStatus("REJECTED");
        request.setAdminId(adminId);
        request.setAdminNotes(reason);
        request.setProcessedAt(Instant.now());

        if ("PAID".equals(request.getStatus()) || "REJECTED".equals(request.getStatus())) notifyOwnerAfterCommit(request);
        return withdrawalRequestRepository.save(request);
    }

    /**
     * Flag withdrawal for reconciliation when bank transfer status is ambiguous.
     * Keeps funds in locked_balance.
     */
    @Transactional
    public WithdrawalRequest markForReconciliation(UUID adminId, UUID withdrawalId, String reason) {
        WithdrawalRequest request = withdrawalRequestRepository.findByIdWithLock(withdrawalId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Không tìm thấy yêu cầu rút tiền"));

        if ("PAID".equalsIgnoreCase(request.getStatus()) || "REJECTED".equalsIgnoreCase(request.getStatus())) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Không thể chuyển trạng thái đối soát cho lệnh đã đóng");
        }

        // Prevent cross-admin interference during bank transfer / reconciliation
        if (request.getClaimedBy() != null && !request.getClaimedBy().equals(adminId)) {
            throw new AppException(ErrorCode.FORBIDDEN, "Lệnh này đang được tiếp nhận bởi một Admin khác");
        }

        if (reason == null || reason.trim().isEmpty()) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Lý do chuyển đối soát không được để trống");
        }

        request.setStatus("RECONCILIATION_REQUIRED");
        request.setAdminId(adminId);
        request.setAdminNotes(reason.trim());
        if ("PAID".equals(request.getStatus()) || "REJECTED".equals(request.getStatus())) notifyOwnerAfterCommit(request);
        return withdrawalRequestRepository.save(request);
    }

    /**
     * Resolves an ambiguous payout under reconciliation:
     * - resolutionAction = "PAID": Bank confirmed transfer succeeded. Debits locked balance and marks PAID.
     * - resolutionAction = "REJECTED": Bank confirmed money never left. Returns locked balance to available and marks REJECTED.
     */
    @Transactional
    public WithdrawalRequest resolveReconciliation(
            UUID adminId,
            UUID withdrawalId,
            String resolutionAction,
            String transactionReference,
            String notes
    ) {
        WithdrawalRequest request = withdrawalRequestRepository.findByIdWithLock(withdrawalId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Không tìm thấy yêu cầu rút tiền"));

        if (!"RECONCILIATION_REQUIRED".equalsIgnoreCase(request.getStatus())) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Lệnh không ở trạng thái đối soát (RECONCILIATION_REQUIRED)");
        }

        // Prevent cross-admin interference: only the claiming admin can resolve reconciliation
        if (request.getClaimedBy() != null && !request.getClaimedBy().equals(adminId)) {
            throw new AppException(ErrorCode.FORBIDDEN, "Lệnh này đang được tiếp nhận bởi một Admin khác");
        }

        if (request.getOwnerId().equals(adminId)) {
            throw new AppException(ErrorCode.FORBIDDEN, "Không được tự duyệt lệnh rút tiền của chính mình");
        }

        Instant now = Instant.now();

        if ("PAID".equalsIgnoreCase(resolutionAction)) {
            if (transactionReference == null || transactionReference.trim().isEmpty()) {
                throw new AppException(ErrorCode.BAD_REQUEST, "Mã tham chiếu ngân hàng không được để trống khi xác nhận đã chuyển");
            }
            String cleanRef = transactionReference.trim();
            Optional<WithdrawalRequest> existingByRef = withdrawalRequestRepository.findByTransactionReferenceIgnoreCase(cleanRef);
            if (existingByRef.isPresent() && !existingByRef.get().getWithdrawalId().equals(withdrawalId)) {
                throw new AppException(ErrorCode.BAD_REQUEST, "Mã tham chiếu ngân hàng này đã được sử dụng cho một lệnh khác");
            }

            merchantWalletService.completeWithdrawal(
                    request.getStoreId(),
                    request.getAmount(),
                    withdrawalId.toString()
            );

            request.setStatus("PAID");
            request.setResolutionStatus("CONFIRMED_PAID");
            request.setTransactionReference(cleanRef);
            request.setResolvedBy(adminId);
            request.setResolvedAt(now);
            request.setResolutionNotes(notes != null ? notes.trim() : null);
            request.setProcessedAt(now);

        } else if ("REJECTED".equalsIgnoreCase(resolutionAction)) {
            if (notes == null || notes.trim().isEmpty()) {
                throw new AppException(ErrorCode.BAD_REQUEST,
                        "Biên bản/lý do đối soát xác nhận tiền chưa rời tài khoản ngân hàng không được để trống");
            }

            String auditProofNote = notes.trim();
            merchantWalletService.unlockWithdrawal(
                    request.getStoreId(),
                    request.getAmount(),
                    withdrawalId.toString(),
                    auditProofNote
            );

            request.setStatus("REJECTED");
            request.setResolutionStatus("CONFIRMED_FAILED");
            request.setResolvedBy(adminId);
            request.setResolvedAt(now);
            request.setResolutionNotes(auditProofNote);
            request.setAdminNotes(auditProofNote);
            request.setProcessedAt(now);

        } else {
            throw new AppException(ErrorCode.BAD_REQUEST, "Hành động giải quyết đối soát không hợp lệ (chỉ chấp nhận PAID hoặc REJECTED)");
        }

        if ("PAID".equals(request.getStatus()) || "REJECTED".equals(request.getStatus())) notifyOwnerAfterCommit(request);
        return withdrawalRequestRepository.save(request);
    }

    private void notifyOwnerAfterCommit(WithdrawalRequest request) {
        UUID owner=request.getOwnerId(); String id=request.getWithdrawalId().toString();
        String message="Yêu cầu rút " + request.getAmount().toPlainString() + "đ: " +
                ("PAID".equals(request.getStatus()) ? "Đã chuyển khoản thành công" : "Đã từ chối, tiền đã trả về số dư khả dụng");
        org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
            new org.springframework.transaction.support.TransactionSynchronization() {
                @Override public void afterCommit() {
                    try { notifications.dispatch(owner,"Kết quả yêu cầu rút tiền",message,
                        com.moodcafe.notification.entity.enums.NotificationType.SYSTEM_ANNOUNCEMENT,id,"/merchant/wallet"); }
                    catch(Exception e) { log.warn("Failed to notify payout {}",id,e); }
                }
            });
    }

    private String calculatePayloadHash(UUID storeId, BigDecimal amount, UUID bankAccountId) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            String raw = storeId + ":" + amount.toPlainString() + ":" + (bankAccountId != null ? bankAccountId : "default");
            byte[] digest = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
