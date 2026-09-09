package com.moodcafe.auth.dto.auth;

public record PendingUser(
        String email,
        String userName,
        String password
) {
}
