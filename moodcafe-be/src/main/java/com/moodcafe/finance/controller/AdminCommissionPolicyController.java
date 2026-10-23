package com.moodcafe.finance.controller;

import com.moodcafe.finance.service.CommissionPolicyService;
import com.moodcafe.shared.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@RestController
@RequestMapping("/api/admin/commission-policies")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminCommissionPolicyController {
    private final CommissionPolicyService service;
    public record PolicyRequest(UUID storeId, BigDecimal commissionRate, Instant effectiveFrom, Instant effectiveTo, Boolean isActive) {}
    public record RevisionRequest(BigDecimal commissionRate, Instant effectiveFrom) {}
    @GetMapping
    public ApiResponse<List<CommissionPolicyService.PolicyView>> list(@RequestParam(required = false) UUID storeId) {
        return ApiResponse.success(service.list(storeId));
    }
    @GetMapping("/{policyId}/audit")
    public ApiResponse<List<Map<String,Object>>> audit(@PathVariable UUID policyId) {
        return ApiResponse.success(service.audit(policyId));
    }
    @PostMapping
    public ApiResponse<CommissionPolicyService.PolicyView> create(@RequestBody PolicyRequest request) {
        return ApiResponse.success(service.create(request.storeId(), request.commissionRate(), request.effectiveFrom(), request.effectiveTo(), request.isActive()));
    }
    @PostMapping("/{policyId}/revisions")
    public ApiResponse<CommissionPolicyService.PolicyView> revise(@PathVariable UUID policyId, @RequestBody RevisionRequest request) {
        return ApiResponse.success(service.revise(policyId, request.commissionRate(), request.effectiveFrom()));
    }
    @PutMapping("/{policyId}/status")
    public ApiResponse<CommissionPolicyService.PolicyView> toggle(@PathVariable UUID policyId, @RequestParam boolean isActive) {
        return ApiResponse.success(service.toggle(policyId, isActive));
    }
}
