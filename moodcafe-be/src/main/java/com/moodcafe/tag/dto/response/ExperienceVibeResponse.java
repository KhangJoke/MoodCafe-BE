package com.moodcafe.tag.dto.response;

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
public class ExperienceVibeResponse {

    private UUID vibeId;
    private String vibeName;
    private String shortName;
    private String description;
    private String imageUrl;
    private long storeCount;
}
