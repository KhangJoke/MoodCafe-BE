package com.moodcafe.tag.dto.response;

import com.moodcafe.tag.entity.enums.StoreTagStatus;
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
public class StoreTagResponse {

    private UUID storeTagId;
    private UUID tagId;
    private String tagName;
    private String category;
    private String categoryCode;
    private StoreTagStatus status;
    private String proofImageUrl;
    private String rejectReason;
    private Instant approvedAt;
    private Instant createdAt;
    private boolean highlighted;

    @Builder.Default
    private Double averageScore = 0.0;

    @Builder.Default
    private Integer reviewCount = 0;

    public Double getAvgRating() {
        return averageScore;
    }

    public Double getAvgPoint() {
        return averageScore;
    }
}
