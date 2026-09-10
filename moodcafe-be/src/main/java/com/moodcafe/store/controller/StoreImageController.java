package com.moodcafe.store.controller;

import com.moodcafe.shared.response.ApiResponse;
import com.moodcafe.store.abstraction.service.IStoreImageService;
import com.moodcafe.store.dto.request.CreateStoreImageRequest;
import com.moodcafe.store.dto.response.StoreImageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/stores/{storeId}/images")
@RequiredArgsConstructor
public class StoreImageController {

    private final IStoreImageService storeImageService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<StoreImageResponse>>> getStoreImages(
            @PathVariable UUID storeId) {
        List<StoreImageResponse> images = storeImageService.getStoreImages(storeId);
        return ResponseEntity.ok(ApiResponse.success(images));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<StoreImageResponse>> addImage(
            @PathVariable UUID storeId,
            @Valid @RequestBody CreateStoreImageRequest request) {
        StoreImageResponse response = storeImageService.addImage(storeId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Image added successfully"));
    }

    @DeleteMapping("/{imageId}")
    public ResponseEntity<ApiResponse<Void>> removeImage(
            @PathVariable UUID storeId,
            @PathVariable UUID imageId) {
        storeImageService.removeImage(storeId, imageId);
        return ResponseEntity.ok(ApiResponse.success(null, "Image removed successfully"));
    }

    @PatchMapping("/{imageId}/primary")
    public ResponseEntity<ApiResponse<StoreImageResponse>> setPrimaryImage(
            @PathVariable UUID storeId,
            @PathVariable UUID imageId) {
        StoreImageResponse response = storeImageService.setPrimaryImage(storeId, imageId);
        return ResponseEntity.ok(ApiResponse.success(response, "Primary image updated successfully"));
    }
}
