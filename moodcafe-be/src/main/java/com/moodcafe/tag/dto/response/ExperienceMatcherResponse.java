package com.moodcafe.tag.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExperienceMatcherResponse {

    private UUID purposeId;
    private String purposeName;
    private String shortName;
    private String subtitle;
    private String imageUrl;
    private long totalStoreCount;
    private List<ExperienceVibeResponse> vibes;
}
