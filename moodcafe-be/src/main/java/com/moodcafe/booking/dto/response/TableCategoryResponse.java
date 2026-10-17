package com.moodcafe.booking.dto.response;

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
public class TableCategoryResponse {

    private UUID categoryId;
    private UUID storeId;
    private String name;
    private String description;
    private Integer maxCapacity;
    private Integer appQuota;
    private Long minSpendAmount;
    private String imageUrl;
    private Boolean isActive;
    private Instant createdAt;
    private Instant updatedAt;
}
