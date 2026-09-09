package com.moodcafe.notification.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "smtp")
public class SmtpSettings {

    private String host;
    private int port;
    private String username;
    private String password;
    private boolean enableSsl;
    private String fromEmail;
    private String fromName;
}