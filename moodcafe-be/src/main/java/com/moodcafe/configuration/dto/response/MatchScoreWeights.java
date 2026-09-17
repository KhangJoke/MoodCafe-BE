package com.moodcafe.configuration.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MatchScoreWeights {

    @Builder.Default
    private double vibeWeight = 0.30;

    @Builder.Default
    private double purposeWeight = 0.30;

    @Builder.Default
    private double noiseWeight = 0.15;

    @Builder.Default
    private double amenityWeight = 0.15;

    @Builder.Default
    private double ratingWeight = 0.10;
}
