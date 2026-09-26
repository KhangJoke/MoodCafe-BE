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

    @NotNull(message = "Vị trí quảng bá không được để trống")
    private SponsoredPlacement placement;

    @NotNull(message = "Thời hạn đăng ký không được để trống")
    private SponsoredDurationType durationType;

    private Instant startDate;

    private String customBannerUrl;

    private String title;

    @Builder.Default
    private String paymentMethod = "VNPAY";

    @Builder.Default
    private boolean useFreeQuota = false;
}
