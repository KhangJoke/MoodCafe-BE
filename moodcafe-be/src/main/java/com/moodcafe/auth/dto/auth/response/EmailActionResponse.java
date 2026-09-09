package com.moodcafe.auth.dto.auth.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailActionResponse {
    private String email;
    private long expiresInSeconds;
    private String message;
}
