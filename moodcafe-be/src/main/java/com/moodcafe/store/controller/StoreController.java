package com.moodcafe.store.controller;

import com.moodcafe.shared.response.ApiResponse;
import com.moodcafe.store.abstraction.service.IStoreService;
import com.moodcafe.store.abstraction.service.IStoreStaffService;
import com.moodcafe.store.dto.request.CreateStoreRequest;
import com.moodcafe.store.dto.request.UpdateStoreRequest;
import com.moodcafe.store.dto.request.UpdateStoreStatusRequest;
import com.moodcafe.store.dto.response.StoreResponse;
import com.moodcafe.store.dto.response.UserStoreResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/stores")
@RequiredArgsConstructor
public class StoreController {

    private final IStoreService storeService;
    private final IStoreStaffService storeStaffService;

    @PostMapping
    public ResponseEntity<ApiResponse<StoreResponse>> createStore(@Valid @RequestBody CreateStoreRequest request) {
        StoreResponse response = storeService.createStore(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Store created successfully"));
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
