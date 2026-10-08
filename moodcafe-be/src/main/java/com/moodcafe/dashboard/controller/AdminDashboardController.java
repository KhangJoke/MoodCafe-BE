package com.moodcafe.dashboard.controller;

import com.moodcafe.dashboard.abstraction.service.AdminDashboardService;
import com.moodcafe.dashboard.dto.response.AdminDashboardStatsResponse;
import com.moodcafe.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Platform Dashboard", description = "Quản trị viên theo dõi dashboard và các chỉ số KPI vận hành toàn hệ thống")
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    @Operation(summary = "Thống kê tổng quan điều hành toàn hệ thống (Gom toàn bộ KPI: Quán, Thẻ, Báo cáo vi phạm, Gói dịch vụ & Doanh thu, Người dùng)")
    @GetMapping({"/dashboard/stats", "/stats"})
    public ResponseEntity<ApiResponse<AdminDashboardStatsResponse>> getAdminDashboardStats() {
        AdminDashboardStatsResponse stats = adminDashboardService.getAdminDashboardStats();
        return ResponseEntity.ok(ApiResponse.success(stats));
    }
}
