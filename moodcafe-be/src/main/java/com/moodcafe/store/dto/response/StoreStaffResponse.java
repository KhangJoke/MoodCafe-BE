package com.moodcafe.store.dto.response;

import com.moodcafe.store.entity.enums.StoreStaffStatus;
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
public class StoreStaffResponse {

    private UUID storeStaffId;
    private UUID storeId;
    private UUID userId;
    private String fullName;
    private String email;
    private String storeRole;
    private StoreStaffStatus status;
    private Instant joinedAt;
}
