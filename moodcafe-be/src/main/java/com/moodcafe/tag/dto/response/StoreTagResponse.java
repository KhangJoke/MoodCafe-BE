package com.moodcafe.tag.dto.response;

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
    private String status;
    private String proofImageUrl;
    private String rejectReason;
    private Instant approvedAt;
    private Instant createdAt;
}
