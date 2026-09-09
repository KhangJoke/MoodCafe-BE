package com.moodcafe.notification.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.RedisScript;

@Configuration
public class RedisLuaConfig {

    @Bean
    public RedisScript<Long> saveOtpScript() {
        return RedisScript.of(
                new ClassPathResource("LuaScripts/save_otp.lua"),
                Long.class
        );
    }

    @Bean
    public RedisScript<Long> validateOtpScript() {
        return RedisScript.of(
                new ClassPathResource("LuaScripts/validate_otp.lua"),
                Long.class
        );
    }

    @Bean
    public RedisScript<Long> rateLimitScript() {
        return RedisScript.of(
                new ClassPathResource("LuaScripts/rate_limit.lua"),
                Long.class
        );
    }
}