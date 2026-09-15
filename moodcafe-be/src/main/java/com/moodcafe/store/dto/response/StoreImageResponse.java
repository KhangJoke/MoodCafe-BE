package com.moodcafe.store.dto.response;

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
public class StoreImageResponse {

    private UUID storeImageId;
    private UUID storeId;
    private String imageUrl;
    private boolean isPrimary;
    private Instant createdAt;
}
