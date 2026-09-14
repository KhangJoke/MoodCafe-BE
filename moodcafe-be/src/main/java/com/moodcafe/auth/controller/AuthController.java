package com.moodcafe.auth.controller;

import com.moodcafe.auth.abstraction.service.AuthService;
import com.moodcafe.auth.dto.auth.request.ConfirmOtpRequest;
import com.moodcafe.auth.dto.auth.request.LoginRequest;
import com.moodcafe.auth.dto.auth.request.RefreshTokenRequest;
import com.moodcafe.auth.dto.auth.request.RegisterRequest;
import com.moodcafe.auth.dto.auth.request.ResetPasswordRequest;
import com.moodcafe.auth.dto.auth.request.SendOtpRequest;
import com.moodcafe.auth.dto.auth.request.SetPasswordRequest;
import com.moodcafe.auth.dto.auth.request.SetupPasswordRequest;
import com.moodcafe.auth.dto.auth.request.SocialLoginRequest;
import com.moodcafe.auth.dto.auth.response.AuthResponse;
import com.moodcafe.auth.dto.auth.response.ConfirmOtpResponse;
import com.moodcafe.auth.dto.auth.response.EmailActionResponse;
import com.moodcafe.auth.dto.user.response.UserResponse;
import com.moodcafe.auth.util.CookieUtils;
import com.moodcafe.shared.error.ErrorCode;
import com.moodcafe.shared.exceptions.AppException;
import com.moodcafe.shared.response.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final CookieUtils cookieUtils;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<EmailActionResponse>> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        EmailActionResponse response = authService.register(request);

        return ResponseEntity.ok(
                ApiResponse.success(
                        response,
                        response.getMessage()
                )
        );
    }

    @PostMapping("/confirm-otp")
    public ResponseEntity<ApiResponse<ConfirmOtpResponse>> confirmOtp(
            @Valid @RequestBody ConfirmOtpRequest request,
            @RequestHeader(value = "X-Cookies-Enabled", defaultValue = "true") String cookiesEnabled,
            HttpServletResponse response
    ) {
        ConfirmOtpResponse confirmResponse = authService.confirmOtp(request);
        handleRefreshTokenCookie(response, confirmResponse, cookiesEnabled);

        return ResponseEntity.ok(
                ApiResponse.success(
                        confirmResponse,
                        confirmResponse.getMessage()
                )
        );
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request,
            @RequestHeader(value = "X-Cookies-Enabled", defaultValue = "true") String cookiesEnabled,
            HttpServletResponse response
    ) {
        AuthResponse authResponse = authService.login(request);
        handleRefreshTokenCookie(response, authResponse, cookiesEnabled);

        return ResponseEntity.ok(
                ApiResponse.success(
                        authResponse,
                        "Login successfully"
                )
        );
    }

    @PostMapping("/social-login")
    public ResponseEntity<ApiResponse<AuthResponse>> socialLogin(
            @Valid @RequestBody SocialLoginRequest request,
            @RequestHeader(value = "X-Cookies-Enabled", defaultValue = "true") String cookiesEnabled,
            HttpServletResponse response
    ) {
        AuthResponse authResponse = authService.socialLogin(request);
        handleRefreshTokenCookie(response, authResponse, cookiesEnabled);

        return ResponseEntity.ok(
                ApiResponse.success(
                        authResponse,
                        "Social login successfully"
                )
        );
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            @CookieValue(value = "refreshToken", required = false) String cookieRefreshToken,
            @RequestBody(required = false) RefreshTokenRequest request,
            @RequestHeader(value = "X-Cookies-Enabled", defaultValue = "true") String cookiesEnabled,
            HttpServletResponse response
    ) {
        String tokenStr = (cookieRefreshToken != null && !cookieRefreshToken.isBlank())
                ? cookieRefreshToken
                : (request != null ? request.getRefreshToken() : null);

        if (tokenStr == null || tokenStr.isBlank()) {
            throw new AppException(ErrorCode.INVALID_INPUT, "Refresh token is required");
        }

        RefreshTokenRequest refreshRequest = new RefreshTokenRequest();
        refreshRequest.setRefreshToken(tokenStr);

        AuthResponse authResponse = authService.refresh(refreshRequest);
        handleRefreshTokenCookie(response, authResponse, cookiesEnabled);

        return ResponseEntity.ok(
                ApiResponse.success(
                        authResponse,
                        "Refresh token successfully"
                )
        );
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @CookieValue(value = "refreshToken", required = false) String cookieRefreshToken,
            @RequestBody(required = false) RefreshTokenRequest request,
            HttpServletResponse response
    ) {
        String tokenStr = (cookieRefreshToken != null && !cookieRefreshToken.isBlank())
                ? cookieRefreshToken
                : (request != null ? request.getRefreshToken() : null);

        if (tokenStr != null && !tokenStr.isBlank()) {
            authService.logout(tokenStr);
        }

        cookieUtils.clearRefreshTokenCookie(response);

        return ResponseEntity.ok(
                ApiResponse.success(
                        null,
                        "Logout successfully"
                )
        );
    }

    private void handleRefreshTokenCookie(HttpServletResponse response, AuthResponse authResponse, String cookiesEnabled) {
        if ("true".equalsIgnoreCase(cookiesEnabled)) {
            if (authResponse != null && authResponse.getRefreshToken() != null) {
                cookieUtils.setRefreshTokenCookie(response, authResponse.getRefreshToken());
                authResponse.setRefreshToken(null);
            }
        }
    }

    private void handleRefreshTokenCookie(HttpServletResponse response, ConfirmOtpResponse confirmResponse, String cookiesEnabled) {
        if ("true".equalsIgnoreCase(cookiesEnabled)) {
            if (confirmResponse != null && confirmResponse.getRefreshToken() != null) {
                cookieUtils.setRefreshTokenCookie(response, confirmResponse.getRefreshToken());
                confirmResponse.setRefreshToken(null);
            }
        }
    }

    @PostMapping("/send-otp")
    public ResponseEntity<ApiResponse<Void>> sendOtp(
            @Valid @RequestBody SendOtpRequest request
    ) {
        authService.sendOtp(request);
        return ResponseEntity.ok(
                ApiResponse.success(
                        null,
                        "OTP has been sent to " + request.getEmail()
                )
        );
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request
    ) {
        authService.resetPassword(request);
        return ResponseEntity.ok(
                ApiResponse.success(
                        null,
                        "Password has been reset successfully"
                )
        );
    }

    @PostMapping("/set-password")
    public ResponseEntity<ApiResponse<UserResponse>> setPassword(
            @Valid @RequestBody SetPasswordRequest request
    ) {
        UserResponse response = authService.setPassword(request);
        return ResponseEntity.ok(
                ApiResponse.success(
                        response,
                        "Thiết lập mật khẩu thành công"
                )
        );
    }

    @PostMapping("/setup-password")
    public ResponseEntity<ApiResponse<AuthResponse>> setupPassword(
            @Valid @RequestBody SetupPasswordRequest request,
            @RequestHeader(value = "X-Cookies-Enabled", defaultValue = "true") String cookiesEnabled,
            HttpServletResponse response
    ) {
        AuthResponse authResponse = authService.setupPassword(request);
        handleRefreshTokenCookie(response, authResponse, cookiesEnabled);

        return ResponseEntity.ok(
                ApiResponse.success(
                        authResponse,
                        "Thiết lập mật khẩu thành công"
                )
        );
    }
}