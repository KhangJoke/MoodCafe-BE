package com.moodcafe.auth.dto.auth.response;

import com.moodcafe.auth.dto.user.response.UserResponse;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuthResponse {

    private UserResponse user;

    private String accessToken;

    private String refreshToken;

    private String tokenType;

    private long expiresIn;
}