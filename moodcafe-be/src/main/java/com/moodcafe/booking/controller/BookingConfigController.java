package com.moodcafe.booking.controller;

import com.moodcafe.booking.abstraction.service.BookingConfigService;
import com.moodcafe.booking.dto.request.UpdateStoreBookingConfigRequest;
import com.moodcafe.booking.dto.response.StoreBookingConfigResponse;
import com.moodcafe.shared.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/stores/{storeId}/booking-config")
@RequiredArgsConstructor
public class BookingConfigController {

    private final BookingConfigService bookingConfigService;

    @GetMapping
    public ResponseEntity<ApiResponse<StoreBookingConfigResponse>> getStoreBookingConfig(
            @PathVariable UUID storeId) {
        StoreBookingConfigResponse response = bookingConfigService.getStoreBookingConfig(storeId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<StoreBookingConfigResponse>> updateStoreBookingConfig(
            @PathVariable UUID storeId,
            @Valid @RequestBody UpdateStoreBookingConfigRequest request) {
        StoreBookingConfigResponse response = bookingConfigService.updateStoreBookingConfig(storeId, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Cập nhật cấu hình đặt chỗ thành công"));
    }
}
