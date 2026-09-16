package com.moodcafe.auth.dto.user.request;

import lombok.*;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OnboardingRequest {

    private List<UUID> purposeTagIds;

    private List<UUID> vibeTagIds;
}
