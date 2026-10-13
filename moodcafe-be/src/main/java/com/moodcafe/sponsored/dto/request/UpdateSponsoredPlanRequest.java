package com.moodcafe.sponsored.dto.request;

import com.moodcafe.sponsored.entity.enums.SponsoredPlacement;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
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
public class UpdateSponsoredPlanRequest {

    private String name;

    private String displayName;

    private SponsoredPlacement placement;

    @Min(value = 1, message = "Số ngày áp dụng tối thiểu là 1")
    private Integer durationDays;

    @DecimalMin(value = "0.0", inclusive = true, message = "Giá gói phải lớn hơn hoặc bằng 0")
    private BigDecimal price;

    private String description;

    private String badgeText;

    private Boolean active;
}
