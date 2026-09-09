package com.moodcafe.auth.dto.user.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class ChangePasswordRequest {
    @NotBlank(message = "Id is required")
    private UUID id;
    @NotBlank(message = "Old Password is required")
    private String oldPassword;
    @NotBlank(message = "New Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String newPassword;
}
