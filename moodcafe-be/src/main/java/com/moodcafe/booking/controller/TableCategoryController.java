package com.moodcafe.booking.controller;

import com.moodcafe.booking.abstraction.service.TableCategoryService;
import com.moodcafe.booking.dto.request.CreateTableCategoryRequest;
import com.moodcafe.booking.dto.request.UpdateTableCategoryRequest;
import com.moodcafe.booking.dto.response.TableCategoryResponse;
import com.moodcafe.shared.response.ApiResponse;
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
@RequestMapping("/api/stores/{storeId}/table-categories")
@RequiredArgsConstructor
public class TableCategoryController {

    private final TableCategoryService tableCategoryService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<TableCategoryResponse>>> getTableCategories(
            @PathVariable UUID storeId) {
        List<TableCategoryResponse> list = tableCategoryService.getTableCategories(storeId);
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<TableCategoryResponse>>> getActiveTableCategories(
            @PathVariable UUID storeId) {
        List<TableCategoryResponse> list = tableCategoryService.getActiveTableCategories(storeId);
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @GetMapping("/{categoryId}")
    public ResponseEntity<ApiResponse<TableCategoryResponse>> getTableCategory(
            @PathVariable UUID storeId,
            @PathVariable UUID categoryId) {
        TableCategoryResponse response = tableCategoryService.getTableCategory(storeId, categoryId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<TableCategoryResponse>> createTableCategory(
            @PathVariable UUID storeId,
            @Valid @RequestBody CreateTableCategoryRequest request) {
        TableCategoryResponse response = tableCategoryService.createTableCategory(storeId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Thêm loại bàn mới thành công"));
    }

    @PutMapping("/{categoryId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<TableCategoryResponse>> updateTableCategory(
            @PathVariable UUID storeId,
            @PathVariable UUID categoryId,
            @Valid @RequestBody UpdateTableCategoryRequest request) {
        TableCategoryResponse response = tableCategoryService.updateTableCategory(storeId, categoryId, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Cập nhật loại bàn thành công"));
    }

    @DeleteMapping("/{categoryId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> deleteTableCategory(
            @PathVariable UUID storeId,
            @PathVariable UUID categoryId) {
        tableCategoryService.deleteTableCategory(storeId, categoryId);
        return ResponseEntity.ok(ApiResponse.success(null, "Xóa loại bàn thành công"));
    }
}
