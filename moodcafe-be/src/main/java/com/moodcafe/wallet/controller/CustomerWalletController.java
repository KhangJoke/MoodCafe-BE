package com.moodcafe.wallet.controller;

import com.moodcafe.auth.abstraction.service.CurrentUserService;
import com.moodcafe.auth.entity.User;
import com.moodcafe.shared.error.ErrorCode;
import com.moodcafe.shared.exceptions.AppException;
import com.moodcafe.shared.response.ApiResponse;
import com.moodcafe.wallet.abstraction.repository.PointTransactionRepository;
import com.moodcafe.wallet.dto.CustomerWalletResponse;
import com.moodcafe.wallet.entity.PointTransaction;
import com.moodcafe.wallet.service.CustomerWalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Customer - MoodPoints Wallet", description = "APIs xem số dư và lịch sử tích/hoàn/đổi điểm MoodPoints")
@RestController
@RequestMapping("/api/me/wallet")
@RequiredArgsConstructor
public class CustomerWalletController {

    private final CustomerWalletService customerWalletService;
    private final PointTransactionRepository pointTransactionRepository;
    private final CurrentUserService currentUserService;

    @Operation(summary = "1. Xem số dư ví MoodPoints của tôi (1 điểm = 1đ)")
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<CustomerWalletResponse>> getMyWallet() {
        User user = currentUserService.getCurrentUser();
        if (user == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED, "Vui lòng đăng nhập");
        }
        CustomerWalletResponse wallet = customerWalletService.getWallet(user.getUserId());
        return ResponseEntity.ok(ApiResponse.success(wallet, "Lấy thông tin ví MoodPoints thành công"));
    }

    @Operation(summary = "2. Lịch sử biến động điểm MoodPoints (hoàn đơn, tích điểm, đổi voucher)")
    @GetMapping("/transactions")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Page<PointTransaction>>> getMyTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size
    ) {
        User user = currentUserService.getCurrentUser();
        if (user == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED, "Vui lòng đăng nhập");
        }
        Pageable pageable = PageRequest.of(page, size);
        Page<PointTransaction> txs = pointTransactionRepository.findByUserIdOrderByCreatedAtDesc(user.getUserId(), pageable);
        return ResponseEntity.ok(ApiResponse.success(txs, "Lấy lịch sử giao dịch thành công"));
    }
}
