package com.moodcafe.auth.util;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class CookieUtils {

    @Value("${app.security.cookie-secure:true}")
    private boolean cookieSecure;

    @Value("${app.security.cookie-same-site:Lax}")
    private String cookieSameSite;

    @PostConstruct
    void validateCookieConfiguration() {
        cookieSameSite = switch (cookieSameSite.trim().toLowerCase(Locale.ROOT)) {
            case "lax" -> "Lax";
            case "strict" -> "Strict";
            case "none" -> "None";
            default -> throw new IllegalArgumentException("COOKIE_SAME_SITE must be Lax, Strict or None");
        };
        if ("None".equals(cookieSameSite) && !cookieSecure) {
            throw new IllegalArgumentException("COOKIE_SAME_SITE=None requires COOKIE_SECURE=true and HTTPS");
        }
    }

    public void setRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        ResponseCookie cookie = ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .maxAge(7 * 24 * 60 * 60) // 7 days
                .sameSite(cookieSameSite)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public void clearRefreshTokenCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .maxAge(0)
                .sameSite(cookieSameSite)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
