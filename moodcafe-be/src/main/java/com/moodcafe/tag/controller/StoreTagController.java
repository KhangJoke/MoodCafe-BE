package com.moodcafe.tag.controller;

import com.moodcafe.shared.response.ApiResponse;
import com.moodcafe.tag.abstraction.service.StoreTagService;
import com.moodcafe.tag.dto.request.ReviewStoreTagRequest;
import com.moodcafe.tag.dto.request.SubmitStoreTagRequest;
import com.moodcafe.tag.dto.request.UpdateStoreHighlightTagsRequest;
import com.moodcafe.tag.dto.response.StoreAttributesResponse;
import com.moodcafe.tag.dto.response.StoreTagResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class StoreTagController {

    private final StoreTagService storeTagService;

    @GetMapping("/api/stores/{storeId}/attributes")
    public ResponseEntity<ApiResponse<StoreAttributesResponse>> getStoreAttributes(@PathVariable UUID storeId) {
        StoreAttributesResponse attributes = storeTagService.getStoreAttributes(storeId);
        return ResponseEntity.ok(ApiResponse.success(attributes));
    }

    @PostMapping("/api/stores/{storeId}/tag-requests")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<StoreTagResponse>> submitTagRequest(
            @PathVariable UUID storeId,
            @Valid @RequestBody SubmitStoreTagRequest request
    ) {
        StoreTagResponse response = storeTagService.requestStoreTag(storeId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Store tag request submitted successfully"));
    }

    @PutMapping("/api/stores/{storeId}/highlight-tags")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<StoreTagResponse>>> updateHighlightTags(
            @PathVariable UUID storeId,
            @Valid @RequestBody UpdateStoreHighlightTagsRequest request
    ) {
        List<StoreTagResponse> response = storeTagService.updateStoreHighlightTags(storeId, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Cập nhật thẻ nổi bật thành công"));
    }

    @GetMapping("/api/admin/store-tags/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<StoreTagResponse>>> getPendingRequests() {
        List<StoreTagResponse> pending = storeTagService.getPendingStoreTagRequests();
        return ResponseEntity.ok(ApiResponse.success(pending));
    }

    @PutMapping("/api/admin/store-tags/{storeTagId}/review")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<StoreTagResponse>> reviewTagRequest(
            @PathVariable UUID storeTagId,
            @Valid @RequestBody ReviewStoreTagRequest request
    ) {
        StoreTagResponse response = storeTagService.reviewStoreTagRequest(storeTagId, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Store tag review completed successfully"));
    }
}
