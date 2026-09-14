package com.moodcafe.auth.dto.auth.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.moodcafe.auth.dto.user.response.UserResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ConfirmOtpResponse {
    private boolean verified;
    private String message;
    private String resetToken;
    private UserResponse user;
    private String accessToken;
    private String refreshToken;
}
