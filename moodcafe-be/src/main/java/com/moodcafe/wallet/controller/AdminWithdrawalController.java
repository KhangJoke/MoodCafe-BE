package com.moodcafe.wallet.controller;

import com.moodcafe.auth.abstraction.service.CurrentUserService;
import com.moodcafe.auth.entity.User;
import com.moodcafe.shared.error.ErrorCode;
import com.moodcafe.shared.exceptions.AppException;
import com.moodcafe.shared.response.ApiResponse;
import com.moodcafe.wallet.abstraction.repository.WithdrawalRequestRepository;
import com.moodcafe.wallet.entity.WithdrawalRequest;
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

import java.util.UUID;

@Tag(name = "Admin - Merchant Payout Processing", description = "APIs quản lý và phê duyệt chuyển tiền rút cho chủ quán")
@RestController
@RequestMapping("/api/admin/withdrawals")
@RequiredArgsConstructor
public class AdminWithdrawalController {

    private final WithdrawalService withdrawalService;
    private final WithdrawalRequestRepository withdrawalRequestRepository;
    private final CurrentUserService currentUserService;

    @Operation(summary = "1. Xem danh sách tất cả yêu cầu rút tiền")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<WithdrawalRequest>>> getAllWithdrawals(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<WithdrawalRequest> requests;
        if (status != null && !status.isBlank()) {
            requests = withdrawalRequestRepository.findByStatusOrderByCreatedAtDesc(status.toUpperCase(), pageable);
        } else {
            requests = withdrawalRequestRepository.findAllByOrderByCreatedAtDesc(pageable);
        }
        return ResponseEntity.ok(ApiResponse.success(requests, "Lấy danh sách yêu cầu rút tiền thành công"));
    }

    @Operation(summary = "2. Admin tiếp nhận xử lý yêu cầu rút tiền (REQUESTED -> PROCESSING)")
    @PostMapping("/{id}/process")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<WithdrawalRequest>> processWithdrawal(@PathVariable UUID id) {
        User admin = currentUserService.getCurrentUser();
        if (admin == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED, "Vui lòng đăng nhập");
        }
        WithdrawalRequest req = withdrawalService.claimWithdrawal(admin.getUserId(), id);
        return ResponseEntity.ok(ApiResponse.success(req, "Tiếp nhận xử lý lệnh rút thành công"));
    }

    @Operation(summary = "3. Admin từ chối yêu cầu rút tiền và hoàn tiền về available_balance")
    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<WithdrawalRequest>> rejectWithdrawal(
            @PathVariable UUID id,
            @RequestParam String reason
    ) {
        User admin = currentUserService.getCurrentUser();
        if (admin == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED, "Vui lòng đăng nhập");
        }
        WithdrawalRequest req = withdrawalService.rejectWithdrawal(admin.getUserId(), id, reason);
        return ResponseEntity.ok(ApiResponse.success(req, "Đã từ chối lệnh rút tiền và hoàn số dư cho quán"));
    }

    @Operation(summary = "4. Admin xác nhận đã chuyển khoản thành công (PROCESSING -> PAID)")
    @PostMapping("/{id}/confirm-paid")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<WithdrawalRequest>> confirmPaid(
            @PathVariable UUID id,
            @RequestBody ConfirmPaidPayload payload
    ) {
        User admin = currentUserService.getCurrentUser();
        if (admin == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED, "Vui lòng đăng nhập");
        }
        WithdrawalRequest req = withdrawalService.confirmPaid(
                admin.getUserId(),
                id,
                payload.getTransactionReference(),
                payload.getEvidenceImageUrl()
        );
        return ResponseEntity.ok(ApiResponse.success(req, "Xác nhận chuyển khoản thành công"));
    }

    @Operation(summary = "5. Admin đánh dấu lệnh rút cần đối soát ngân hàng (PROCESSING -> RECONCILIATION_REQUIRED)")
    @PostMapping("/{id}/reconcile")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<WithdrawalRequest>> markForReconciliation(
            @PathVariable UUID id,
            @RequestParam String reason
    ) {
        User admin = currentUserService.getCurrentUser();
        if (admin == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED, "Vui lòng đăng nhập");
        }
        WithdrawalRequest req = withdrawalService.markForReconciliation(admin.getUserId(), id, reason);
        return ResponseEntity.ok(ApiResponse.success(req, "Đã chuyển lệnh sang trạng thái đối soát"));
    }

    @Operation(summary = "6. Admin giải quyết đối soát lệnh rút (RECONCILIATION_REQUIRED -> PAID hoặc REJECTED)")
    @PostMapping("/{id}/resolve-reconciliation")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<WithdrawalRequest>> resolveReconciliation(
            @PathVariable UUID id,
            @RequestBody ResolveReconciliationPayload payload
    ) {
        User admin = currentUserService.getCurrentUser();
        if (admin == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED, "Vui lòng đăng nhập");
        }
        WithdrawalRequest req = withdrawalService.resolveReconciliation(
                admin.getUserId(),
                id,
                payload.getAction(),
                payload.getTransactionReference(),
                payload.getNotes()
        );
        return ResponseEntity.ok(ApiResponse.success(req, "Đã giải quyết đối soát lệnh rút tiền thành công"));
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResolveReconciliationPayload {
        private String action; // PAID or REJECTED
        private String transactionReference;
        private String notes;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConfirmPaidPayload {
        private String transactionReference;
        private String evidenceImageUrl;
    }
}
