package com.moodcafe.tag.dto.response;

import com.moodcafe.tag.entity.enums.ApprovalMode;
import com.moodcafe.tag.entity.enums.ControlType;
import com.moodcafe.tag.entity.enums.StoreTagStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoreTagResponse {

    private UUID storeTagId;
    private UUID storeId;
    private String storeName;
    private UUID tagId;
    private String tagName;
    private String category;
    private String categoryCode;
    private ApprovalMode approvalMode;
    private ControlType controlType;
    private StoreTagStatus status;
    @Builder.Default
    private List<String> proofImageUrls = new ArrayList<>();
    private String imageUrl;
    private String rejectReason;
    @Builder.Default
    private boolean allowResubmit = true;
    private Instant approvedAt;
    private Instant createdAt;
    private boolean highlighted;

    @Builder.Default
    private Double averageScore = 0.0;

    @Builder.Default
    private Integer reviewCount = 0;

    public String getProofImageUrl() {
        return (proofImageUrls != null && !proofImageUrls.isEmpty()) ? proofImageUrls.get(0) : null;
    }

    public Double getAvgRating() {
        return averageScore;
    }

    public Double getAvgPoint() {
        return averageScore;
    }
}
