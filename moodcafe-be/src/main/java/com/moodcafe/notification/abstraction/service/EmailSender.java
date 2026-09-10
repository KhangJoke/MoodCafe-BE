package com.moodcafe.notification.abstraction.service;

import java.util.concurrent.CompletableFuture;

public interface EmailSender {

    // async functions
    CompletableFuture<Void> sendOtpEmailAsync(String toEmail, String otp);

    CompletableFuture<Void> sendPasswordResetEmailAsync(String toEmail, String resetLink);

    CompletableFuture<Void> sendWelcomeEmailAsync(String toEmail, String userName);

    CompletableFuture<Void> sendNotificationEmailAsync(String toEmail, String title, String content);
}
