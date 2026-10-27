package com.moodcafe.gamification.dto.response;

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
public class EquippedTitleDto {
    private UUID titleId;
    private String code;
    private String name;
    private String tier;
    private String colorHex;
    private String iconUrl;
}
