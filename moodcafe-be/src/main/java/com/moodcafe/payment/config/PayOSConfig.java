package com.moodcafe.payment.config;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import vn.payos.PayOS;

@Slf4j
@Getter
@Configuration
public class PayOSConfig {

    @Value("${payos.client-id:}")
    private String clientId;

    @Value("${payos.api-key:}")
    private String apiKey;

    @Value("${payos.checksum-key:}")
    private String checksumKey;

    @Value("${payos.return-url:https://moodcafe-be-production.up.railway.app/payment/success}")
    private String returnUrl;

    @Value("${payos.cancel-url:https://moodcafe-be-production.up.railway.app/payment/cancel}")
    private String cancelUrl;

    @Bean
    public PayOS payOS() {
        if (isConfigured()) {
            log.info("PayOS initialized successfully.");
            return new PayOS(clientId.trim(), apiKey.trim(), checksumKey.trim());
        }
        log.warn("PayOS credentials are not configured or empty.");
        return null;
    }

    public boolean isConfigured() {
        return clientId != null && !clientId.trim().isEmpty()
                && apiKey != null && !apiKey.trim().isEmpty()
                && checksumKey != null && !checksumKey.trim().isEmpty();
    }
}
