package com.moodcafe.gamification.dto.response;

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
public class TitleAdminResponse {
    private UUID titleId;
    private String code;
    private String name;
    private String description;
    private String tier;
    private String colorHex;
    private String iconUrl;
    private Boolean isActive;
    private Boolean isDeleted;
    private Instant createdAt;
    private Instant updatedAt;
}

