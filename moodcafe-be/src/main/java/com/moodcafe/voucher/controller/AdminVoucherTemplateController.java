package com.moodcafe.voucher.controller;

import com.moodcafe.voucher.entity.VoucherTemplate;
import com.moodcafe.voucher.abstraction.repository.VoucherTemplateRepository;
import com.moodcafe.shared.response.ApiResponse;
import com.moodcafe.shared.error.ErrorCode;
import com.moodcafe.shared.exceptions.AppException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.*;

@RestController
@RequestMapping("/api/admin/voucher-templates")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminVoucherTemplateController {
    private final VoucherTemplateRepository repository;
    public record Terms(@NotBlank @Size(max=150) String title, String description,
                        @NotNull @DecimalMin("1") @Digits(integer=10,fraction=0) BigDecimal faceValueVnd,
                        @NotNull @Min(1) Long pointCost,
                        @NotNull @DecimalMin("0") @Digits(integer=10,fraction=0) BigDecimal minGrossAmount,
                        @NotNull @Min(1) @Max(3650) Integer validDays,
                        @NotNull Boolean isActive, @NotNull @Min(0) Integer displayOrder) {}
    @GetMapping
    public ApiResponse<List<VoucherTemplate>> list() { return ApiResponse.success(repository.findAll()); }
    @PostMapping
    @Transactional
    public ApiResponse<VoucherTemplate> create(@Valid @RequestBody Terms terms) {
        return save(new VoucherTemplate(),terms);
    }
    @PutMapping("/{id}")
    @Transactional
    public ApiResponse<VoucherTemplate> update(@PathVariable UUID id,@Valid @RequestBody Terms terms) {
        return save(repository.findById(id).orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND)),terms);
    }
    private ApiResponse<VoucherTemplate> save(VoucherTemplate value,Terms t) {
        if (t.faceValueVnd().compareTo(BigDecimal.valueOf(t.pointCost())) != 0)
            throw new AppException(ErrorCode.BAD_REQUEST,"1 MoodPoint = 1đ, điểm đổi phải bằng mệnh giá voucher");
        value.setTitle(t.title().trim()); value.setDescription(t.description());
        value.setFaceValueVnd(t.faceValueVnd()); value.setPointCost(t.pointCost());
        value.setMinGrossAmount(t.minGrossAmount()); value.setValidDays(t.validDays());
        value.setIsActive(t.isActive()); value.setDisplayOrder(t.displayOrder());
        return ApiResponse.success(repository.save(value));
    }
}
