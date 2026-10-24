package com.moodcafe.wallet.controller;

import com.moodcafe.auth.entity.User;
import com.moodcafe.shared.response.ApiResponse;
import com.moodcafe.wallet.abstraction.repository.StoreBankAccountRepository;
import com.moodcafe.wallet.abstraction.repository.WithdrawalRequestRepository;
import com.moodcafe.wallet.entity.StoreBankAccount;
import com.moodcafe.wallet.entity.WithdrawalRequest;
import com.moodcafe.wallet.service.StoreFinancialSecurityService;
import com.moodcafe.wallet.service.WithdrawalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Tag(name = "Merchant - Store Withdrawals & Bank Accounts", description = "APIs tạo lệnh rút tiền và quản lý tài khoản ngân hàng cơ sở")
@RestController
@RequestMapping("/api/merchant/stores/{storeId}")
@RequiredArgsConstructor
public class MerchantWithdrawalController {

    private final WithdrawalService withdrawalService;
    private final WithdrawalRequestRepository withdrawalRequestRepository;
    private final StoreBankAccountRepository bankAccountRepository;
    private final StoreFinancialSecurityService financialSecurityService;

    @Operation(summary = "1. Tạo yêu cầu rút tiền về tài khoản ngân hàng (theo hạn mức Admin cấu hình)")
    @PostMapping("/withdrawals")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<WithdrawalRequest>> requestWithdrawal(
            @PathVariable UUID storeId,
            @RequestBody RequestWithdrawalPayload payload
    ) {
        User user = financialSecurityService.requireActiveStoreOwner(storeId);

        WithdrawalRequest req = withdrawalService.requestWithdrawal(
                user.getUserId(),
                storeId,
                payload.getAmount(),
                payload.getBankAccountId(),
                payload.getIdempotencyKey()
        );
        return ResponseEntity.ok(ApiResponse.success(req, "Tạo yêu cầu rút tiền thành công"));
    }

    @Operation(summary = "2. Xem lịch sử yêu cầu rút tiền của quán")
    @GetMapping("/withdrawals")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Page<WithdrawalRequest>>> getWithdrawals(
            @PathVariable UUID storeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size
    ) {
        financialSecurityService.requireActiveStoreOwner(storeId);
        Pageable pageable = PageRequest.of(page, size);
        Page<WithdrawalRequest> requests = withdrawalRequestRepository.findByStoreIdOrderByCreatedAtDesc(storeId, pageable);
        return ResponseEntity.ok(ApiResponse.success(requests, "Lấy danh sách lệnh rút tiền thành công"));
    }

    @Operation(summary = "3. Lấy danh sách tài khoản ngân hàng của cơ sở")
    @GetMapping("/bank-accounts")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<StoreBankAccount>>> getBankAccounts(@PathVariable UUID storeId) {
        financialSecurityService.requireActiveStoreOwner(storeId);
        List<StoreBankAccount> accounts = bankAccountRepository.findByStoreIdAndIsDeletedFalse(storeId);
        return ResponseEntity.ok(ApiResponse.success(accounts, "Lấy danh sách tài khoản ngân hàng thành công"));
    }

    @Operation(summary = "4. Thêm tài khoản ngân hàng cho cơ sở")
    @PostMapping("/bank-accounts")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<StoreBankAccount>> addBankAccount(
            @PathVariable UUID storeId,
            @RequestBody CreateBankAccountPayload payload
    ) {
        financialSecurityService.requireActiveStoreOwner(storeId);

        // If setting as default, unset other defaults
        if (Boolean.TRUE.equals(payload.getIsDefault())) {
            List<StoreBankAccount> existing = bankAccountRepository.findByStoreId(storeId);
            for (StoreBankAccount acc : existing) {
                if (Boolean.TRUE.equals(acc.getIsDefault())) {
                    acc.setIsDefault(false);
                    bankAccountRepository.save(acc);
                }
            }
        }

        StoreBankAccount account = StoreBankAccount.builder()
                .storeId(storeId)
                .bankName(payload.getBankName())
                .bankCode(payload.getBankCode())
                .accountNumber(payload.getAccountNumber())
                .accountHolderName(payload.getAccountHolderName())
                .isDefault(payload.getIsDefault() != null ? payload.getIsDefault() : true)
                .isDeleted(false)
                .build();

        StoreBankAccount saved = bankAccountRepository.save(account);
        return ResponseEntity.ok(ApiResponse.success(saved, "Thêm tài khoản ngân hàng thành công"));
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RequestWithdrawalPayload {
        private BigDecimal amount;
        private UUID bankAccountId;
        private String idempotencyKey;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateBankAccountPayload {
        private String bankName;
        private String bankCode;
        private String accountNumber;
        private String accountHolderName;
        private Boolean isDefault;
    }
}
