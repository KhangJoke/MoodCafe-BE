package com.moodcafe.store.controller;

import com.moodcafe.shared.response.ApiResponse;
import com.moodcafe.store.abstraction.service.IStoreStaffService;
import com.moodcafe.store.dto.request.AddStoreStaffRequest;
import com.moodcafe.store.dto.request.UpdateStoreStaffRequest;
import com.moodcafe.store.dto.response.StoreStaffResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/stores/{storeId}/staff")
@RequiredArgsConstructor
public class StoreStaffController {

    private final IStoreStaffService storeStaffService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<StoreStaffResponse>>> getStoreStaff(
            @PathVariable UUID storeId) {
        List<StoreStaffResponse> staffList = storeStaffService.getStoreStaff(storeId);
        return ResponseEntity.ok(ApiResponse.success(staffList));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<StoreStaffResponse>> addStaff(
            @PathVariable UUID storeId,
            @Valid @RequestBody AddStoreStaffRequest request) {
        StoreStaffResponse staff = storeStaffService.addStaff(storeId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(staff, "Staff added successfully"));
    }

    @PatchMapping("/{userId}")
    public ResponseEntity<ApiResponse<StoreStaffResponse>> updateStaffRole(
            @PathVariable UUID storeId,
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateStoreStaffRequest request) {
        StoreStaffResponse staff = storeStaffService.updateStaffRole(storeId, userId, request);
        return ResponseEntity.ok(ApiResponse.success(staff, "Staff updated successfully"));
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<ApiResponse<Void>> removeStaff(
            @PathVariable UUID storeId,
            @PathVariable UUID userId) {
        storeStaffService.removeStaff(storeId, userId);
        return ResponseEntity.ok(ApiResponse.success(null, "Staff removed successfully"));
    }
}
