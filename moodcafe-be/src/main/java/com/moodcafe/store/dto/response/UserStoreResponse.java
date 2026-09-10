package com.moodcafe.store.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserStoreResponse {

    private UUID storeId;
    private String storeName;
    private String storeAddress;
    private String storeRole;
    private String status;
    private LocalDateTime joinedAt;
}
