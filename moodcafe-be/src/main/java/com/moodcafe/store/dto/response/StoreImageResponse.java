package com.moodcafe.store.dto.response;

import lombok.*;

import java.time.LocalDateTime;
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
    private LocalDateTime createdAt;
}
