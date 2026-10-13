package com.moodcafe.sponsored.dto.request;

import com.moodcafe.sponsored.entity.enums.SponsoredPlacement;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateSponsoredPlanRequest {

    @NotBlank(message = "Mã định danh gói không được để trống")
    private String name;

    @NotBlank(message = "Tên hiển thị gói không được để trống")
    private String displayName;

    @NotNull(message = "Vị trí quảng bá không được để trống")
    private SponsoredPlacement placement;

    @NotNull(message = "Số ngày áp dụng không được để trống")
    @Min(value = 1, message = "Số ngày áp dụng tối thiểu là 1")
    private Integer durationDays;

    @NotNull(message = "Giá gói không được để trống")
    @DecimalMin(value = "0.0", inclusive = true, message = "Giá gói phải lớn hơn hoặc bằng 0")
    private BigDecimal price;

    private String description;

    private String badgeText;

    @Builder.Default
    private Boolean active = true;
}
