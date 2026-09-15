package com.moodcafe.auth.dto.user.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.moodcafe.auth.dto.role.response.RoleResponse;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class UserResponse {

    private UUID userId;
    private String email;
    private String fullName;
    private String avatarUrl;

    private RoleResponse role;

    private Boolean active;
    private Boolean emailVerified;
    private Boolean requirePasswordChange;
    private String noiseTolerance;

    @JsonProperty("isFirstLogin")
    private Boolean firstLogin;

    private Instant createdAt;
    private Instant updatedAt;
}
