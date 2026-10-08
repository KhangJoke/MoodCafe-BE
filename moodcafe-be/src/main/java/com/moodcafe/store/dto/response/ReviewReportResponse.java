package com.moodcafe.store.dto.response;

import com.moodcafe.store.entity.enums.ReviewReportReason;
import com.moodcafe.store.entity.enums.ReviewReportStatus;
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
public class ReviewReportResponse {

    private UUID reportId;
    private UUID reviewId;
    private UUID reporterUserId;
    private String reporterFullName;
    private UUID storeId;
    private String storeName;
    private ReviewReportReason reason;
    private String details;
    private ReviewReportStatus status;
    private String adminNote;
    private String reviewContent;
    private java.math.BigDecimal reviewOverallRating;
    private String reviewAuthorFullName;
    private UUID reviewAuthorUserId;
    private Instant createdAt;
    private Instant resolvedAt;
}
