package com.moodcafe.auth.dto.user.response;

import com.moodcafe.auth.dto.role.response.RoleResponse;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class UserResponse {

    private UUID userId;
    private String email;
    private String userName ;
    private String avatarUrl;

    private RoleResponse role;

    private Boolean active;
    private Boolean emailVerified;
    private Boolean requirePasswordChange;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
