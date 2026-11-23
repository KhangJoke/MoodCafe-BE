package com.moodcafe.finance.controller;

import com.moodcafe.finance.dto.PlatformFinanceSummaryResponse;
import com.moodcafe.finance.dto.PlatformFinanceTrendItemResponse;
import com.moodcafe.finance.dto.PlatformRevenueEntryResponse;
import com.moodcafe.finance.service.PlatformFinanceService;
import com.moodcafe.shared.response.ApiResponse;
import com.moodcafe.shared.response.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/admin/finance")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Platform Finance & P&L", description = "Thống kê Doanh thu (Subscription + Hoa hồng đặt bàn), Chi phí Voucher và Lợi nhuận ròng (Net Income)")
public class AdminPlatformFinanceController {

    private final PlatformFinanceService platformFinanceService;

    @Operation(summary = "1. Tổng quan Tài chính (Revenue phân tách theo nguồn, Expenses chi phí tạo voucher, Net Income = Revenue - Expenses)")
    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<PlatformFinanceSummaryResponse>> getSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        PlatformFinanceSummaryResponse summary = platformFinanceService.getSummary(from, to);
        return ResponseEntity.ok(ApiResponse.success(summary, "Lấy tổng quan tài chính thành công"));
    }

    @Operation(summary = "2. Biểu đồ xu hướng Doanh thu / Chi phí Voucher / Lợi nhuận ròng theo ngày (DAILY) hoặc tháng (MONTHLY)")
    @GetMapping("/trends")
    public ResponseEntity<ApiResponse<List<PlatformFinanceTrendItemResponse>>> getTrends(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "DAILY") String groupBy
    ) {
        List<PlatformFinanceTrendItemResponse> trends = platformFinanceService.getTrends(from, to, groupBy);
        return ResponseEntity.ok(ApiResponse.success(trends, "Lấy biểu đồ xu hướng tài chính thành công"));
    }

    @Operation(summary = "3. Tra cứu chi tiết các khoản doanh thu (lọc theo nguồn ALL / SUBSCRIPTION_PAYMENT / BOOKING_COMMISSION)")
    @GetMapping("/revenue-entries")
    public ResponseEntity<ApiResponse<PageResponse<PlatformRevenueEntryResponse>>> getRevenueEntries(
            @RequestParam(defaultValue = "ALL") String source,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        PageResponse<PlatformRevenueEntryResponse> entries = platformFinanceService.getRevenueEntries(source, from, to, page, size);
        return ResponseEntity.ok(ApiResponse.success(entries, "Lấy danh sách giao dịch doanh thu thành công"));
    }
}

