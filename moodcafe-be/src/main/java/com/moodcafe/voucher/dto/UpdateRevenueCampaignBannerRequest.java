package com.moodcafe.voucher.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateRevenueCampaignBannerRequest {

    @Size(max = 500, message = "URL ảnh banner tối đa 500 ký tự")
    private String bannerUrl;
}
