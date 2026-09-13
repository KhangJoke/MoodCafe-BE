package com.moodcafe.notification.service;

import com.moodcafe.notification.abstraction.service.EmailSender;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.time.Year;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailSenderImpl implements EmailSender {

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;

    @Override
    @Async
    public CompletableFuture<Void> sendOtpEmailAsync(
            String toEmail,
            String otp
    ) {

        try {
            Context context = new Context();

            context.setVariable("otp", otp);
            context.setVariable("year", Year.now().getValue());

            String html = templateEngine.process(
                    "email/otp",
                    context
            );

            sendHtmlEmail(
                    toEmail,
                    "MoodCafe - OTP Verification Code",
                    html
            );

            return CompletableFuture.completedFuture(null);

        } catch (Exception e) {

            log.error(
                    "Failed to send OTP email to {}",
                    toEmail,
                    e
            );

            return CompletableFuture.failedFuture(e);
        }
    }

    @Override
    @Async
    public CompletableFuture<Void> sendPasswordResetEmailAsync(
            String toEmail,
            String resetLink
    ) {

        try {
            Context context = new Context();

            context.setVariable("resetLink", resetLink);
            context.setVariable("year", Year.now().getValue());

            String html = templateEngine.process(
                    "email/password-reset",
                    context
            );

            sendHtmlEmail(
                    toEmail,
                    "MoodCafe - Password Reset",
                    html
            );

            return CompletableFuture.completedFuture(null);

        } catch (Exception e) {

            log.error(
                    "Failed to send password reset email to {}",
                    toEmail,
                    e
            );

            return CompletableFuture.failedFuture(e);
        }
    }

    @Override
    @Async
    public CompletableFuture<Void> sendWelcomeEmailAsync(
            String toEmail,
            String fullName
    ) {

        try {
            Context context = new Context();

            context.setVariable("email", toEmail);
            context.setVariable("fullName", fullName);
            context.setVariable("year", Year.now().getValue());

            String html = templateEngine.process(
                    "email/welcome",
                    context
            );

            sendHtmlEmail(
                    toEmail,
                    "Welcome to MoodCafe",
                    html
            );

            return CompletableFuture.completedFuture(null);

        } catch (Exception e) {

            log.error(
                    "Failed to send welcome email to {}",
                    toEmail,
                    e
            );

            return CompletableFuture.failedFuture(e);
        }
    }

    @Override
    @Async
    public CompletableFuture<Void> sendNotificationEmailAsync(
            String toEmail,
            String title,
            String content
    ) {

        try {
            Context context = new Context();

            context.setVariable("title", title);
            context.setVariable("content", content);
            context.setVariable("year", Year.now().getValue());

            String html = templateEngine.process(
                    "email/notification",
                    context
            );

            sendHtmlEmail(
                    toEmail,
                    title,
                    html
            );

            return CompletableFuture.completedFuture(null);

        } catch (Exception e) {

            log.error(
                    "Failed to send notification email to {}",
                    toEmail,
                    e
            );

            return CompletableFuture.failedFuture(e);
        }
    }

    private void sendHtmlEmail(
            String toEmail,
            String subject,
            String html
    ) {

        try {

            MimeMessage message =
                    mailSender.createMimeMessage();

            MimeMessageHelper helper =
                    new MimeMessageHelper(
                            message,
                            true,
                            "UTF-8"
                    );

            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(html, true);

            mailSender.send(message);

            log.info(
                    "Email sent successfully to {}",
                    toEmail
            );

        } catch (Exception e) {

            log.error(
                    "Failed to send email to {}",
                    toEmail,
                    e
            );

            throw new RuntimeException(
                    "Failed to send email",
                    e
            );
        }
    }
}