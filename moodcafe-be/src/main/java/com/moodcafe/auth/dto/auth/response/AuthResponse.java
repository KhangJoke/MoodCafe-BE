package com.moodcafe.auth.dto.auth.response;

import com.moodcafe.auth.dto.user.response.UserResponse;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class AuthResponse {

    private UserResponse user;

    private String accessToken;

    private String refreshToken;

    private String tokenType;

    private long expiresIn;

    private boolean needsPasswordSetup;

    private String setupToken;
}