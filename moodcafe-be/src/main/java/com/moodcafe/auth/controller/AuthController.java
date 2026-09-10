package com.moodcafe.auth.controller;

import com.moodcafe.auth.abstraction.service.AuthService;
import com.moodcafe.auth.dto.auth.request.ConfirmOtpRequest;
import com.moodcafe.auth.dto.auth.request.LoginRequest;
import com.moodcafe.auth.dto.auth.request.RefreshTokenRequest;
import com.moodcafe.auth.dto.auth.request.RegisterRequest;
import com.moodcafe.auth.dto.auth.request.ResetPasswordRequest;
import com.moodcafe.auth.dto.auth.request.SendOtpRequest;
import com.moodcafe.auth.dto.auth.response.AuthResponse;
import com.moodcafe.auth.dto.auth.response.ConfirmOtpResponse;
import com.moodcafe.auth.dto.auth.response.EmailActionResponse;
import com.moodcafe.shared.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

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
            @Valid @RequestBody ConfirmOtpRequest request
    ) {
        ConfirmOtpResponse response = authService.confirmOtp(request);

        return ResponseEntity.ok(
                ApiResponse.success(
                        response,
                        response.getMessage()
                )
        );
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request
    ) {
        AuthResponse response = authService.login(request);

        return ResponseEntity.ok(
                ApiResponse.success(
                        response,
                        "Login successfully"
                )
        );
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            @Valid @RequestBody RefreshTokenRequest request
    ) {
        AuthResponse response = authService.refresh(request);

        return ResponseEntity.ok(
                ApiResponse.success(
                        response,
                        "Refresh token successfully"
                )
        );
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @Valid @RequestBody RefreshTokenRequest request
    ) {
        authService.logout(request.getRefreshToken());

        return ResponseEntity.ok(
                ApiResponse.success(
                        null,
                        "Logout successfully"
                )
        );
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
}