package com.moodcafe.voucher.controller;

import com.moodcafe.shared.response.ApiResponse;
import com.moodcafe.voucher.abstraction.repository.VoucherTemplateRepository;
import com.moodcafe.voucher.entity.VoucherTemplate;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Customer - Voucher Catalog", description = "APIs xem danh mục các mệnh giá voucher có thể đổi bằng MoodPoints")
@RestController
@RequestMapping("/api/voucher-catalog")
@RequiredArgsConstructor
public class VoucherCatalogController {

    private final VoucherTemplateRepository voucherTemplateRepository;

    @Operation(summary = "1. Xem danh mục voucher đổi điểm (1 điểm = 1đ)")
    @GetMapping
    public ResponseEntity<ApiResponse<List<VoucherTemplate>>> getCatalog() {
        List<VoucherTemplate> templates = voucherTemplateRepository.findByIsActiveTrueOrderByDisplayOrderAsc();
        return ResponseEntity.ok(ApiResponse.success(templates, "Lấy danh mục voucher thành công"));
    }
}
