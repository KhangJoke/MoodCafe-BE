package com.moodcafe.store.controller;

import com.moodcafe.shared.response.ApiResponse;
import com.moodcafe.shared.response.PageResponse;
import com.moodcafe.store.abstraction.service.StoreService;
import com.moodcafe.store.dto.request.AdminStoreImportRequest;
import com.moodcafe.store.dto.response.AdminStoreImportResponse;
import com.moodcafe.store.dto.response.StoreResponse;
import com.moodcafe.store.entity.enums.StoreStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/stores")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Store Management", description = "Quản trị viên quản lý danh sách quán toàn hệ thống (Tìm kiếm, phân trang, lọc trạng thái)")
public class AdminStoreController {

    private final StoreService storeService;

    @Operation(summary = "Xem danh sách quán dành cho Admin (Hỗ trợ phân trang, tìm kiếm tên/hotline/email/địa chỉ, lọc trạng thái)")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<StoreResponse>>> getAllStores(
            @RequestParam(required = false) StoreStatus status,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        PageResponse<StoreResponse> response = storeService.getAllStoresAdmin(status, search, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "Import hàng loạt quán cà phê dành cho Admin (Hỗ trợ gắn Tag theo tên, ảnh, lịch mở cửa và chủ quán)")
    @PostMapping("/import")
    public ResponseEntity<ApiResponse<AdminStoreImportResponse>> importStores(
            @Valid @RequestBody AdminStoreImportRequest request
    ) {
        AdminStoreImportResponse response = storeService.importStoresAdmin(request);
        String msg = String.format("Import hoàn tất: %d tạo mới, %d bỏ qua, %d lỗi",
                response.getCreatedCount(), response.getSkippedCount(), response.getFailedCount());
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(response, msg));
    }
}
