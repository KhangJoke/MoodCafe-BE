package com.moodcafe.voucher.dto;

import com.moodcafe.voucher.entity.enums.RevenueCampaignStatus;
import jakarta.validation.constraints.NotNull;
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
public class UpdateRevenueCampaignStatusRequest {

    @NotNull(message = "Trạng thái không được để trống")
    private RevenueCampaignStatus status;
}

