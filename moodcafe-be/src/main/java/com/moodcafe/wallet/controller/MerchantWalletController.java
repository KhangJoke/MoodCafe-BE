package com.moodcafe.wallet.controller;

import com.moodcafe.auth.entity.User;
import com.moodcafe.shared.response.ApiResponse;
import com.moodcafe.wallet.abstraction.repository.MerchantWalletTransactionRepository;
import com.moodcafe.wallet.entity.MerchantWallet;
import com.moodcafe.wallet.entity.MerchantWalletTransaction;
import com.moodcafe.wallet.service.MerchantWalletService;
import com.moodcafe.wallet.service.StoreFinancialSecurityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Merchant - Store Wallet", description = "APIs xem ví doanh thu của từng cơ sở (Chỉ dành cho OWNER)")
@RestController
@RequestMapping("/api/merchant/stores/{storeId}/wallet")
@RequiredArgsConstructor
public class MerchantWalletController {

    private final MerchantWalletService merchantWalletService;
    private final MerchantWalletTransactionRepository transactionRepository;
    private final StoreFinancialSecurityService financialSecurityService;

    @Operation(summary = "1. Xem 3 số dư của ví quán (pending, available, locked)")
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<MerchantWallet>> getStoreWallet(@PathVariable UUID storeId) {
        financialSecurityService.requireActiveStoreOwner(storeId);
        MerchantWallet wallet = merchantWalletService.getWallet(storeId);
        return ResponseEntity.ok(ApiResponse.success(wallet, "Lấy thông tin ví cửa hàng thành công"));
    }

    @Operation(summary = "2. Xem lịch sử biến động số dư ví quán")
    @GetMapping("/transactions")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Page<MerchantWalletTransaction>>> getStoreTransactions(
            @PathVariable UUID storeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size
    ) {
        financialSecurityService.requireActiveStoreOwner(storeId);
        MerchantWallet wallet = merchantWalletService.getWallet(storeId);
        Pageable pageable = PageRequest.of(page, size);
        Page<MerchantWalletTransaction> txs = transactionRepository.findByWalletIdOrderByCreatedAtDesc(wallet.getWalletId(), pageable);
        return ResponseEntity.ok(ApiResponse.success(txs, "Lấy lịch sử giao dịch ví quán thành công"));
    }
}
