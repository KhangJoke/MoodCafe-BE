package com.moodcafe.auth.dto.auth.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SocialLoginRequest {

    @NotBlank(message = "Provider is required (GOOGLE or FACEBOOK)")
    private String provider;

    @NotBlank(message = "Token is required")
    private String token;

    private String email;

    private String fullName;

    private String avatarUrl;
}

