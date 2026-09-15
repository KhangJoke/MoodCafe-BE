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
public class FavoriteStoreResponse {

    private UUID favoriteStoreId;
    private UUID userId;
    private UUID storeId;
    private String storeName;
    private String storeAddress;
    private String primaryImageUrl;
    private Instant createdAt;
}
