package com.moodcafe.auth.dto.role.response;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class RoleResponse {

    private UUID roleId;
    private String name;
}