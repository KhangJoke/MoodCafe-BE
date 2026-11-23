package com.moodcafe.voucher.dto;

import com.moodcafe.voucher.entity.enums.RevenueFundingSource;
import com.moodcafe.voucher.entity.enums.RevenueVoucherDistributionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateRevenueVoucherCampaignRequest {

    @NotBlank(message = "Tiêu đề chiến dịch voucher không được để trống")
    @Size(max = 150, message = "Tiêu đề tối đa 150 ký tự")
    private String title;

    private String description;

    @NotNull(message = "Loại phân phối (DIRECT_GRANT hoặc CLAIMABLE_POOL) không được để trống")
    private RevenueVoucherDistributionType distributionType;

    @Builder.Default
    private RevenueFundingSource fundingSource = RevenueFundingSource.ALL;

    @NotNull(message = "Mệnh giá voucher không được để trống")
    @DecimalMin(value = "1000", message = "Mệnh giá voucher tối thiểu 1.000đ")
    @Digits(integer = 10, fraction = 0, message = "Mệnh giá voucher phải là số nguyên VND")
    private BigDecimal faceValueVnd;

    @Builder.Default
    @DecimalMin(value = "0", message = "Giá trị đơn tối thiểu không được âm")
    @Digits(integer = 10, fraction = 0, message = "Giá trị đơn tối thiểu phải là số nguyên VND")
    private BigDecimal minGrossAmount = BigDecimal.ZERO;

    @Builder.Default
    @Min(value = 1, message = "Số ngày hiệu lực tối thiểu 1 ngày")
    @Max(value = 3650, message = "Số ngày hiệu lực tối đa 3650 ngày")
    private Integer validDays = 30;

    // Required for CLAIMABLE_POOL
    @Min(value = 1, message = "Tổng số lượng voucher tối thiểu 1")
    private Integer totalQuantity;

    @Builder.Default
    @Min(value = 1, message = "Số lần nhận tối đa mỗi user phải >= 1")
    private Integer maxClaimsPerUser = 1;

    private Instant startsAt;
    private Instant endsAt;

    // Used for DIRECT_GRANT
    private List<UUID> targetUserIds;

    @Builder.Default
    private Boolean grantToAllActiveCustomers = false;
}

