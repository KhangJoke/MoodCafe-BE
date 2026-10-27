package com.moodcafe.gamification.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuestRewardDto {
    private Long moodPoints;
    private VoucherRewardDto voucher;
    private EquippedTitleDto title;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class VoucherRewardDto {
        private UUID templateId;
        private String title;
        private BigDecimal faceValueVnd;
    }
}
