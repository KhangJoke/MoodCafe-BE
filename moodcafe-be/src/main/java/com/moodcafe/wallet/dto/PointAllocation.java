package com.moodcafe.wallet.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PointAllocation {

    @Builder.Default
    private Long totalAllocatedPoints = 0L;

    @Builder.Default
    private Long refundPointsAllocated = 0L;

    @Builder.Default
    private Long rewardPointsAllocated = 0L;

    @Builder.Default
    private List<LotAllocationItem> items = new ArrayList<>();

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LotAllocationItem {
        private UUID lotId;
        private Long allocatedPoints;
        private String fundingType; // REFUND or REWARD
    }
}
