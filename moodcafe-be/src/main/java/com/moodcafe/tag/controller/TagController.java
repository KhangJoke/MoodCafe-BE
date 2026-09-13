package com.moodcafe.tag.controller;

import com.moodcafe.shared.response.ApiResponse;
import com.moodcafe.tag.abstraction.service.TagService;
import com.moodcafe.tag.dto.request.CreateTagRequest;
import com.moodcafe.tag.dto.request.UpdateTagRequest;
import com.moodcafe.tag.dto.response.TagResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tags")
@RequiredArgsConstructor
public class TagController {

    private final TagService tagService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<TagResponse>>> getAllTags(
            @RequestParam(required = false) String category
    ) {
        List<TagResponse> tags = tagService.getAllTags(category);
        return ResponseEntity.ok(ApiResponse.success(tags));
    }

    @GetMapping("/{tagId}")
    public ResponseEntity<ApiResponse<TagResponse>> getTagById(@PathVariable UUID tagId) {
        TagResponse tag = tagService.getTagById(tagId);
        return ResponseEntity.ok(ApiResponse.success(tag));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<TagResponse>> createTag(
            @Valid @RequestBody CreateTagRequest request
    ) {
        TagResponse tag = tagService.createTag(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(tag, "Tag created successfully"));
    }

    @PutMapping("/{tagId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<TagResponse>> updateTag(
            @PathVariable UUID tagId,
            @Valid @RequestBody UpdateTagRequest request
    ) {
        TagResponse tag = tagService.updateTag(tagId, request);
        return ResponseEntity.ok(ApiResponse.success(tag, "Tag updated successfully"));
    }

    @DeleteMapping("/{tagId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteTag(@PathVariable UUID tagId) {
        tagService.deleteTag(tagId);
        return ResponseEntity.ok(ApiResponse.success(null, "Tag deleted successfully"));
    }
}
