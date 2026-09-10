package com.moodcafe.store.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoreStaffResponse {

    private UUID storeStaffId;
    private UUID storeId;
    private UUID userId;
    private String userName;
    private String email;
    private String storeRole;
    private String status;
    private LocalDateTime joinedAt;
}
