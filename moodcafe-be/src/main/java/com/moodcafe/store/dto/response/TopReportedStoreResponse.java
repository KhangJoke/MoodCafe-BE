package com.moodcafe.store.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TopReportedStoreResponse {

    private UUID storeId;
    private String storeName;
    private long reportCount;
    private Double avgRating;
}
