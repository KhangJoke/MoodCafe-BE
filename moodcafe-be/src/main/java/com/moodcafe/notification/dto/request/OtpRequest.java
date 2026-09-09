package com.moodcafe.notification.dto.request;

public record OtpRequest(
        String email,
        String otp
) {}