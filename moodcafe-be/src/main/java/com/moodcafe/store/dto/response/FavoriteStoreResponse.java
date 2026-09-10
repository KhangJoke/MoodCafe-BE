package com.moodcafe.store.dto.response;

import lombok.*;

import java.time.LocalDateTime;
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
    private LocalDateTime createdAt;
}
