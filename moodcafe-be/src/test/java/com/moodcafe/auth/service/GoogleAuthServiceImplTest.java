package com.moodcafe.auth.service;

import com.moodcafe.auth.dto.auth.SocialUserInfo;
import com.moodcafe.shared.error.ErrorCode;
import com.moodcafe.shared.exceptions.AppException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GoogleAuthServiceImplTest {

    private GoogleAuthServiceImpl googleAuthService;

    @BeforeEach
    void setUp() {
        googleAuthService = new GoogleAuthServiceImpl();
    }

    @Test
    @DisplayName("supports - returns true for GOOGLE case-insensitive")
    void supports_Google_ReturnsTrue() {
        assertThat(googleAuthService.supports("GOOGLE")).isTrue();
        assertThat(googleAuthService.supports("google")).isTrue();
        assertThat(googleAuthService.supports("FACEBOOK")).isFalse();
    }

    @Test
    @DisplayName("verifyToken - empty token throws TOKEN_INVALID")
    void verifyToken_EmptyToken_ThrowsTokenInvalid() {
        assertThatThrownBy(() -> googleAuthService.verifyToken(""))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TOKEN_INVALID);

        assertThatThrownBy(() -> googleAuthService.verifyToken(null))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TOKEN_INVALID);
    }
}
