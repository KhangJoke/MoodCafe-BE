package com.moodcafe.menu.controller;

import com.moodcafe.menu.dto.request.CreateMenuCategoryRequest;
import com.moodcafe.menu.dto.request.CreateMenuItemRequest;
import com.moodcafe.menu.dto.request.UpdateMenuCategoryRequest;
import com.moodcafe.menu.dto.request.UpdateMenuItemRequest;
import com.moodcafe.menu.dto.response.MenuCategoryResponse;
import com.moodcafe.menu.dto.response.MenuItemResponse;
import com.moodcafe.menu.service.MenuService;
import com.moodcafe.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Store Menu", description = "Quản lý thực đơn quán cà phê (Danh mục và Món ăn)")
public class StoreMenuController {

    private final MenuService menuService;

    // --- Categories ---

    @Operation(summary = "Lấy danh sách toàn bộ danh mục thực đơn của quán")
    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<MenuCategoryResponse>>> getCategories(
            @PathVariable UUID storeId) {
        List<MenuCategoryResponse> categories = menuService.getCategories(storeId);
        return ResponseEntity.ok(ApiResponse.success(categories));
    }

    @Operation(summary = "Tạo danh mục thực đơn mới cho quán (Yêu cầu quyền OWNER / MANAGER)")
    @PostMapping("/categories")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<MenuCategoryResponse>> createCategory(
            @PathVariable UUID storeId,
            @Valid @RequestBody CreateMenuCategoryRequest request) {
        MenuCategoryResponse category = menuService.createCategory(storeId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(category, "Tạo danh mục thành công"));
    }

    @Operation(summary = "Cập nhật danh mục thực đơn (Yêu cầu quyền OWNER / MANAGER)")
    @PatchMapping("/categories/{categoryId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<MenuCategoryResponse>> updateCategory(
            @PathVariable UUID storeId,
            @PathVariable UUID categoryId,
            @Valid @RequestBody UpdateMenuCategoryRequest request) {
        MenuCategoryResponse category = menuService.updateCategory(storeId, categoryId, request);
        return ResponseEntity.ok(ApiResponse.success(category, "Cập nhật danh mục thành công"));
    }

    @Operation(summary = "Xóa danh mục thực đơn (Chỉ cho phép khi danh mục trống không có món)")
    @DeleteMapping("/categories/{categoryId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(
            @PathVariable UUID storeId,
            @PathVariable UUID categoryId) {
        menuService.deleteCategory(storeId, categoryId);
        return ResponseEntity.ok(ApiResponse.success(null, "Xóa danh mục thành công"));
    }

    // --- Items ---

    @Operation(
            summary = "Lấy danh sách món ăn trong thực đơn có phân trang",
            description = "Hỗ trợ phân trang qua page (mặc định 0), size (mặc định 20), sort (mặc định createdAt,desc). Hỗ trợ tìm kiếm theo tên món (q), lọc theo danh mục (categoryId), lọc trạng thái còn hàng (isAvailable)."
    )
    @GetMapping("/items")
    public ResponseEntity<ApiResponse<Page<MenuItemResponse>>> getItems(
            @PathVariable UUID storeId,
            @Parameter(description = "Lọc theo ID danh mục") @RequestParam(required = false) UUID categoryId,
            @Parameter(description = "Từ khóa tìm kiếm theo tên món ăn") @RequestParam(required = false) String q,
            @Parameter(description = "Lọc theo trạng thái còn bán (true/false)") @RequestParam(required = false) Boolean isAvailable,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<MenuItemResponse> items = menuService.getItems(storeId, categoryId, q, isAvailable, pageable);
        return ResponseEntity.ok(ApiResponse.success(items));
    }

    @Operation(summary = "Tạo món ăn mới trong thực đơn (Yêu cầu quyền OWNER / MANAGER)")
    @PostMapping("/items")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<MenuItemResponse>> createItem(
            @PathVariable UUID storeId,
            @Valid @RequestBody CreateMenuItemRequest request) {
        MenuItemResponse item = menuService.createItem(storeId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(item, "Tạo món ăn thành công"));
    }

    @Operation(summary = "Cập nhật thông tin món ăn (Yêu cầu quyền OWNER / MANAGER)")
    @PatchMapping("/items/{itemId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<MenuItemResponse>> updateItem(
            @PathVariable UUID storeId,
            @PathVariable UUID itemId,
            @Valid @RequestBody UpdateMenuItemRequest request) {
        MenuItemResponse item = menuService.updateItem(storeId, itemId, request);
        return ResponseEntity.ok(ApiResponse.success(item, "Cập nhật món ăn thành công"));
    }

    @Operation(summary = "Xóa mềm món ăn khỏi thực đơn (Yêu cầu quyền OWNER / MANAGER)")
    @DeleteMapping("/items/{itemId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> deleteItem(
            @PathVariable UUID storeId,
            @PathVariable UUID itemId) {
        menuService.deleteItem(storeId, itemId);
        return ResponseEntity.ok(ApiResponse.success(null, "Xóa món ăn thành công"));
    }
}
