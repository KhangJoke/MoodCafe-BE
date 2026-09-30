package com.moodcafe.menu.controller;

import com.moodcafe.menu.dto.request.CreateMenuCategoryRequest;
import com.moodcafe.menu.dto.request.CreateMenuItemRequest;
import com.moodcafe.menu.dto.request.UpdateMenuCategoryRequest;
import com.moodcafe.menu.dto.request.UpdateMenuItemRequest;
import com.moodcafe.menu.dto.response.MenuCategoryResponse;
import com.moodcafe.menu.dto.response.MenuItemResponse;
import com.moodcafe.menu.service.MenuService;
import com.moodcafe.shared.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/stores/{storeId}/menu")
@RequiredArgsConstructor
public class StoreMenuController {

    private final MenuService menuService;

    // --- Categories ---

    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<MenuCategoryResponse>>> getCategories(
            @PathVariable UUID storeId) {
        List<MenuCategoryResponse> categories = menuService.getCategories(storeId);
        return ResponseEntity.ok(ApiResponse.success(categories));
    }

    @PostMapping("/categories")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<MenuCategoryResponse>> createCategory(
            @PathVariable UUID storeId,
            @Valid @RequestBody CreateMenuCategoryRequest request) {
        MenuCategoryResponse category = menuService.createCategory(storeId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(category, "Tạo danh mục thành công"));
    }

    @PatchMapping("/categories/{categoryId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<MenuCategoryResponse>> updateCategory(
            @PathVariable UUID storeId,
            @PathVariable UUID categoryId,
            @Valid @RequestBody UpdateMenuCategoryRequest request) {
        MenuCategoryResponse category = menuService.updateCategory(storeId, categoryId, request);
        return ResponseEntity.ok(ApiResponse.success(category, "Cập nhật danh mục thành công"));
    }

    @DeleteMapping("/categories/{categoryId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(
            @PathVariable UUID storeId,
            @PathVariable UUID categoryId) {
        menuService.deleteCategory(storeId, categoryId);
        return ResponseEntity.ok(ApiResponse.success(null, "Xóa danh mục thành công"));
    }

    // --- Items ---

    @GetMapping("/items")
    public ResponseEntity<ApiResponse<Page<MenuItemResponse>>> getItems(
            @PathVariable UUID storeId,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Boolean isAvailable,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<MenuItemResponse> items = menuService.getItems(storeId, categoryId, q, isAvailable, pageable);
        return ResponseEntity.ok(ApiResponse.success(items));
    }

    @PostMapping("/items")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<MenuItemResponse>> createItem(
            @PathVariable UUID storeId,
            @Valid @RequestBody CreateMenuItemRequest request) {
        MenuItemResponse item = menuService.createItem(storeId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(item, "Tạo món ăn thành công"));
    }

    @PatchMapping("/items/{itemId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<MenuItemResponse>> updateItem(
            @PathVariable UUID storeId,
            @PathVariable UUID itemId,
            @Valid @RequestBody UpdateMenuItemRequest request) {
        MenuItemResponse item = menuService.updateItem(storeId, itemId, request);
        return ResponseEntity.ok(ApiResponse.success(item, "Cập nhật món ăn thành công"));
    }

    @DeleteMapping("/items/{itemId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> deleteItem(
            @PathVariable UUID storeId,
            @PathVariable UUID itemId) {
        menuService.deleteItem(storeId, itemId);
        return ResponseEntity.ok(ApiResponse.success(null, "Xóa món ăn thành công"));
    }
}
