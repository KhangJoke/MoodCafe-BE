package com.moodcafe.sponsored.dto.request;

import com.moodcafe.sponsored.entity.enums.SponsoredDurationType;
import com.moodcafe.sponsored.entity.enums.SponsoredPlacement;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateSponsoredListingRequest {

    @NotNull(message = "storeId không được để trống")
    private UUID storeId;

    /**
     * ID của gói tài trợ do Admin thiết lập giá và cấu hình (Khuyên dùng)
     */
    private UUID planId;

    /**
     * Vị trí quảng bá (Dùng cho fallback nếu không truyền planId)
     */
    private SponsoredPlacement placement;

    /**
     * Thời hạn đăng ký (Dùng cho fallback nếu không truyền planId)
     */
    private SponsoredDurationType durationType;

    private Instant startDate;

    private String customBannerUrl;

    private String title;

    @Builder.Default
    private String paymentMethod = "PAYOS";

    @Builder.Default
    private boolean useFreeQuota = false;
}
