package com.moodcafe.store.controller;

import com.moodcafe.shared.response.ApiResponse;
import com.moodcafe.shared.response.PageResponse;
import com.moodcafe.store.abstraction.service.StoreService;
import com.moodcafe.store.abstraction.service.StoreStaffService;
import com.moodcafe.store.dto.request.CreateStoreRequest;
import com.moodcafe.store.dto.request.StoreRegisterRequest;
import com.moodcafe.store.dto.request.StoreResubmitRequest;
import com.moodcafe.store.dto.request.StoreSearchRequest;
import com.moodcafe.store.dto.request.UpdateStoreRequest;
import com.moodcafe.store.dto.request.UpdateStoreStatusRequest;
import com.moodcafe.store.dto.response.FeaturedMoodStoreResponse;
import com.moodcafe.store.dto.response.MerchantDashboardResponse;
import com.moodcafe.store.dto.response.StoreRegistrationStatusResponse;
import com.moodcafe.store.dto.response.StoreResponse;
import com.moodcafe.store.dto.response.StoreSearchItemResponse;
import com.moodcafe.store.dto.response.UserStoreResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/stores")
@RequiredArgsConstructor
public class StoreController {

    private final StoreService storeService;
    private final StoreStaffService storeStaffService;

    @PostMapping
    public ResponseEntity<ApiResponse<StoreResponse>> createStore(@Valid @RequestBody CreateStoreRequest request) {
        StoreResponse response = storeService.createStore(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Store created successfully"));
    }

    @PostMapping("/register")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<StoreRegistrationStatusResponse>> registerStore(
            @Valid @RequestBody StoreRegisterRequest request
    ) {
        StoreRegistrationStatusResponse response = storeService.registerStore(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Đăng ký thông tin quán thành công, hồ sơ đang chờ xét duyệt"));
    }

    @PutMapping("/{storeId}/resubmit")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<StoreRegistrationStatusResponse>> resubmitStore(
            @PathVariable UUID storeId,
            @Valid @RequestBody StoreResubmitRequest request
    ) {
        StoreRegistrationStatusResponse response = storeService.resubmitStore(storeId, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Nộp lại hồ sơ quán thành công"));
    }

    @GetMapping("/my-registration")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<StoreRegistrationStatusResponse>> getMyRegistration() {
        StoreRegistrationStatusResponse response = storeService.getMyRegistration();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{storeId}/registration-status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<StoreRegistrationStatusResponse>> getStoreRegistrationStatus(
            @PathVariable UUID storeId
    ) {
        StoreRegistrationStatusResponse response = storeService.getStoreRegistrationStatus(storeId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{storeId}/merchant/dashboard")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<MerchantDashboardResponse>> getMerchantDashboard(
            @PathVariable UUID storeId
    ) {
        MerchantDashboardResponse response = storeService.getMerchantDashboard(storeId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PageResponse<StoreSearchItemResponse>>> searchStores(
            StoreSearchRequest request) {
        PageResponse<StoreSearchItemResponse> response = storeService.searchStores(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/featured-moods")
    public ResponseEntity<ApiResponse<List<FeaturedMoodStoreResponse>>> getFeaturedMoodStores() {
        List<FeaturedMoodStoreResponse> response = storeService.getFeaturedMoodStores();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/districts")
    public ResponseEntity<ApiResponse<List<String>>> getActiveDistricts() {
        List<String> districts = storeService.getActiveDistricts();
        return ResponseEntity.ok(ApiResponse.success(districts));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<StoreResponse>>> getAllStores(
            @RequestParam(required = false) String status) {
        List<StoreResponse> stores = storeService.getAllStores(status);
        return ResponseEntity.ok(ApiResponse.success(stores));
    }

    @GetMapping("/my-stores")
    public ResponseEntity<ApiResponse<List<UserStoreResponse>>> getMyStores() {
        List<UserStoreResponse> stores = storeStaffService.getUserStores();
        return ResponseEntity.ok(ApiResponse.success(stores));
    }

    @GetMapping("/{storeId}")
    public ResponseEntity<ApiResponse<StoreResponse>> getStoreById(@PathVariable UUID storeId) {
        StoreResponse store = storeService.getStoreById(storeId);
        return ResponseEntity.ok(ApiResponse.success(store));
    }

    @PutMapping("/{storeId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<StoreResponse>> updateStore(
            @PathVariable UUID storeId,
            @Valid @RequestBody UpdateStoreRequest request) {
        StoreResponse updatedStore = storeService.updateStore(storeId, request);
        return ResponseEntity.ok(ApiResponse.success(updatedStore, "Store updated successfully"));
    }

    @PatchMapping("/{storeId}/status")
    public ResponseEntity<ApiResponse<StoreResponse>> changeStoreStatus(
            @PathVariable UUID storeId,
            @Valid @RequestBody UpdateStoreStatusRequest request) {
        StoreResponse updatedStore = storeService.changeStoreStatus(storeId, request);
        return ResponseEntity.ok(ApiResponse.success(updatedStore, "Store status updated successfully"));
    }
}
