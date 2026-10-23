package com.moodcafe.finance.controller;

import com.moodcafe.finance.service.*;
import com.moodcafe.shared.response.ApiResponse;
import com.moodcafe.shared.error.ErrorCode;
import com.moodcafe.shared.exceptions.AppException;
import com.moodcafe.wallet.service.StoreFinancialSecurityService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.*;

@RestController
@RequestMapping("/api/merchant/stores/{storeId}")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class OwnerCommissionController {
    private final StoreFinancialSecurityService security;
    private final CommissionPolicyService policies;
    private final OwnerCommissionQueryService query;
    @GetMapping("/commission-policy")
    public ApiResponse<CommissionPolicyService.EffectivePolicy> effective(@PathVariable UUID storeId) {
        security.requireActiveStoreOwner(storeId);return ApiResponse.success(policies.effective(storeId));
    }
    @GetMapping("/commission-policies")
    @Transactional(readOnly = true)
    public ApiResponse<Map<String,Object>> history(@PathVariable UUID storeId,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="15") int size) {
        security.requireActiveStoreOwner(storeId);
        if(page<0||size<1||size>100)throw new AppException(ErrorCode.BAD_REQUEST,"Phân trang không hợp lệ");
        return ApiResponse.success(policies.history(storeId,page,size));
    }
    @GetMapping("/commission-summary")
    public ApiResponse<Map<String,Object>> summary(@PathVariable UUID storeId,@RequestParam(required=false) Instant from,@RequestParam(required=false) Instant to) {
        security.requireActiveStoreOwner(storeId);return ApiResponse.success(query.summary(storeId,query.period(from,to)));
    }
    @GetMapping("/commission-bookings")
    public ApiResponse<Map<String,Object>> list(@PathVariable UUID storeId,@RequestParam(required=false) Instant from,@RequestParam(required=false) Instant to,
            @RequestParam(required=false) String status,@RequestParam(required=false) String bookingCode,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="15") int size) {
        security.requireActiveStoreOwner(storeId);return ApiResponse.success(query.bookings(storeId,query.period(from,to),status,bookingCode,page,size));
    }
    @GetMapping("/commission-bookings/{bookingId}")
    public ApiResponse<Map<String,Object>> detail(@PathVariable UUID storeId,@PathVariable UUID bookingId) {
        security.requireActiveStoreOwner(storeId);return ApiResponse.success(query.detail(storeId,bookingId));
    }
}
