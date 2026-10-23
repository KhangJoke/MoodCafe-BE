package com.moodcafe.finance.controller;

import com.moodcafe.finance.abstraction.repository.CommissionPolicyRepository;
import com.moodcafe.finance.entity.CommissionPolicy;
import com.moodcafe.shared.error.ErrorCode;
import com.moodcafe.shared.exceptions.AppException;
import com.moodcafe.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Tag(name = "Admin - Commission Policies", description = "APIs quản lý chính sách hoa hồng sàn & theo cơ sở")
@RestController
@RequestMapping("/api/admin/commission-policies")
@RequiredArgsConstructor
public class AdminCommissionPolicyController {

    private final CommissionPolicyRepository commissionPolicyRepository;

    @Operation(summary = "1. Lấy danh sách chính sách hoa hồng (Global & Store)")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<CommissionPolicy>>> getPolicies(
            @RequestParam(required = false) UUID storeId
    ) {
        List<CommissionPolicy> policies;
        if (storeId != null) {
            policies = commissionPolicyRepository.findByStoreId(storeId);
        } else {
            policies = commissionPolicyRepository.findAll();
        }
        return ResponseEntity.ok(ApiResponse.success(policies, "Lấy danh sách chính sách hoa hồng thành công"));
    }

    @Operation(summary = "2. Tạo mới chính sách hoa hồng (storeId = null là Global)")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CommissionPolicy>> createPolicy(
            @RequestBody CreateCommissionPolicyRequest request
    ) {
        if (request.getCommissionRate() == null ||
                request.getCommissionRate().compareTo(BigDecimal.ZERO) < 0 ||
                request.getCommissionRate().compareTo(BigDecimal.ONE) > 0) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Tỷ lệ hoa hồng phải từ 0.00 đến 1.00 (0% - 100%)");
        }

        CommissionPolicy policy = CommissionPolicy.builder()
                .storeId(request.getStoreId())
                .commissionRate(request.getCommissionRate())
                .effectiveFrom(request.getEffectiveFrom() != null ? request.getEffectiveFrom() : Instant.now())
                .effectiveTo(request.getEffectiveTo())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();

        CommissionPolicy saved = commissionPolicyRepository.save(policy);
        return ResponseEntity.ok(ApiResponse.success(saved, "Tạo chính sách hoa hồng thành công"));
    }

    @Operation(summary = "3. Cập nhật trạng thái chính sách hoa hồng")
    @PutMapping("/{policyId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CommissionPolicy>> updatePolicyStatus(
            @PathVariable UUID policyId,
            @RequestParam boolean isActive
    ) {
        CommissionPolicy policy = commissionPolicyRepository.findById(policyId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Không tìm thấy chính sách hoa hồng"));

        policy.setIsActive(isActive);
        CommissionPolicy updated = commissionPolicyRepository.save(policy);
        return ResponseEntity.ok(ApiResponse.success(updated, "Cập nhật trạng thái thành công"));
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateCommissionPolicyRequest {
        private UUID storeId;
        private BigDecimal commissionRate;
        private Instant effectiveFrom;
        private Instant effectiveTo;
        private Boolean isActive;
    }
}
