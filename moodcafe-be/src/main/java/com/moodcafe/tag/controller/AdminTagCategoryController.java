package com.moodcafe.tag.controller;

import com.moodcafe.shared.response.ApiResponse;
import com.moodcafe.tag.abstraction.service.TagCategoryService;
import com.moodcafe.tag.dto.request.CreateTagCategoryRequest;
import com.moodcafe.tag.dto.request.UpdateTagCategoryRequest;
import com.moodcafe.tag.dto.response.TagCategoryResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/tag-categories")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminTagCategoryController {

    private final TagCategoryService tagCategoryService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<TagCategoryResponse>>> getAllTagCategories() {
        List<TagCategoryResponse> tagCategories = tagCategoryService.getAllTagCategories();
        return ResponseEntity.ok(ApiResponse.success(tagCategories));
    }

    @GetMapping("/{tagCategoryId}")
    public ResponseEntity<ApiResponse<TagCategoryResponse>> getTagCategoryById(@PathVariable UUID tagCategoryId) {
        TagCategoryResponse tagCategory = tagCategoryService.getTagCategoryById(tagCategoryId);
        return ResponseEntity.ok(ApiResponse.success(tagCategory));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TagCategoryResponse>> createTagCategory(
            @Valid @RequestBody CreateTagCategoryRequest request
    ) {
        TagCategoryResponse tagCategory = tagCategoryService.createTagCategory(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(tagCategory, "Tag category created successfully"));
    }

    @PutMapping("/{tagCategoryId}")
    public ResponseEntity<ApiResponse<TagCategoryResponse>> updateTagCategory(
            @PathVariable UUID tagCategoryId,
            @Valid @RequestBody UpdateTagCategoryRequest request
    ) {
        TagCategoryResponse tagCategory = tagCategoryService.updateTagCategory(tagCategoryId, request);
        return ResponseEntity.ok(ApiResponse.success(tagCategory, "Tag category updated successfully"));
    }

    @DeleteMapping("/{tagCategoryId}")
    public ResponseEntity<ApiResponse<Void>> deleteTagCategory(@PathVariable UUID tagCategoryId) {
        tagCategoryService.deleteTagCategory(tagCategoryId);
        return ResponseEntity.ok(ApiResponse.success(null, "Tag category deleted successfully"));
    }
}
