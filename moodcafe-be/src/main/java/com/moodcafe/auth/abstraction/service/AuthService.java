package com.moodcafe.auth.abstraction.service;

import com.moodcafe.auth.dto.auth.request.ConfirmOtpRequest;
import com.moodcafe.auth.dto.auth.request.LoginRequest;
import com.moodcafe.auth.dto.auth.request.RefreshTokenRequest;
import com.moodcafe.auth.dto.auth.request.RegisterRequest;
import com.moodcafe.auth.dto.auth.request.ResetPasswordRequest;
import com.moodcafe.auth.dto.auth.request.SendOtpRequest;
import com.moodcafe.auth.dto.auth.request.SetPasswordRequest;
import com.moodcafe.auth.dto.auth.request.SocialLoginRequest;
import com.moodcafe.auth.dto.auth.response.AuthResponse;
import com.moodcafe.auth.dto.auth.response.ConfirmOtpResponse;
import com.moodcafe.auth.dto.auth.response.EmailActionResponse;
import com.moodcafe.auth.dto.user.response.UserResponse;

public interface AuthService {

    EmailActionResponse register(RegisterRequest request);

    ConfirmOtpResponse confirmOtp(ConfirmOtpRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse socialLogin(SocialLoginRequest request);

    AuthResponse refresh(RefreshTokenRequest request);

    void logout(String refreshToken);

    void sendOtp(SendOtpRequest request);

    void resetPassword(ResetPasswordRequest request);

    UserResponse setPassword(SetPasswordRequest request);
}
