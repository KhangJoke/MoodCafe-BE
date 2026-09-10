package com.moodcafe.store.controller;

import com.moodcafe.shared.response.ApiResponse;
import com.moodcafe.store.abstraction.service.IAmenityService;
import com.moodcafe.store.dto.request.CreateAmenityRequest;
import com.moodcafe.store.dto.request.UpdateAmenityRequest;
import com.moodcafe.store.dto.response.AmenityResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class AmenityController {

    private final IAmenityService amenityService;

    // --- Global Amenities ---

    @GetMapping("/api/amenities")
    public ResponseEntity<ApiResponse<List<AmenityResponse>>> getAllAmenities() {
        List<AmenityResponse> amenities = amenityService.getAllAmenities();
        return ResponseEntity.ok(ApiResponse.success(amenities));
    }

    @PostMapping("/api/amenities")
    public ResponseEntity<ApiResponse<AmenityResponse>> createAmenity(
            @Valid @RequestBody CreateAmenityRequest request) {
        AmenityResponse amenity = amenityService.createAmenity(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(amenity, "Amenity created successfully"));
    }

    @PutMapping("/api/amenities/{amenityId}")
    public ResponseEntity<ApiResponse<AmenityResponse>> updateAmenity(
            @PathVariable UUID amenityId,
            @Valid @RequestBody UpdateAmenityRequest request) {
        AmenityResponse amenity = amenityService.updateAmenity(amenityId, request);
        return ResponseEntity.ok(ApiResponse.success(amenity, "Amenity updated successfully"));
    }

    // --- Store-specific Amenities ---

    @GetMapping("/api/stores/{storeId}/amenities")
    public ResponseEntity<ApiResponse<List<AmenityResponse>>> getStoreAmenities(
            @PathVariable UUID storeId) {
        List<AmenityResponse> amenities = amenityService.getStoreAmenities(storeId);
        return ResponseEntity.ok(ApiResponse.success(amenities));
    }

    @PostMapping("/api/stores/{storeId}/amenities/{amenityId}")
    public ResponseEntity<ApiResponse<Void>> addAmenityToStore(
            @PathVariable UUID storeId,
            @PathVariable UUID amenityId) {
        amenityService.addAmenityToStore(storeId, amenityId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(null, "Amenity added to store successfully"));
    }

    @DeleteMapping("/api/stores/{storeId}/amenities/{amenityId}")
    public ResponseEntity<ApiResponse<Void>> removeAmenityFromStore(
            @PathVariable UUID storeId,
            @PathVariable UUID amenityId) {
        amenityService.removeAmenityFromStore(storeId, amenityId);
        return ResponseEntity.ok(ApiResponse.success(null, "Amenity removed from store successfully"));
    }
}
