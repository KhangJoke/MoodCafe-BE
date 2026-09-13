package com.moodcafe.auth.dto.user.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OnboardingRequest {

    @NotBlank(message = "Noise tolerance is required (LOW, MEDIUM, HIGH)")
    private String noiseTolerance;

    private List<UUID> purposeTagIds;

    private List<UUID> vibeTagIds;
}
