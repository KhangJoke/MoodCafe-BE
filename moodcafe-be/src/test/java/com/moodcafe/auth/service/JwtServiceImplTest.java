package com.moodcafe.auth.service;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceImplTest {

    private JwtServiceImpl jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtServiceImpl();
        ReflectionTestUtils.setField(jwtService, "secretKey", "your_super_secret_jwt_key_please_uong_ca_phe_di_ban");
        ReflectionTestUtils.setField(jwtService, "accessTokenExpirationMinutes", 15L);
        ReflectionTestUtils.setField(jwtService, "issuer", "mood_cafe");
        ReflectionTestUtils.setField(jwtService, "audience", "mood_cafe");
    }

    @Test
    void testTokenExpirationAndClaims() {
        UserDetails userDetails = new User(
                "test@example.com",
                "password",
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        long beforeGen = System.currentTimeMillis();
        String token = jwtService.generateToken(userDetails);
        long afterGen = System.currentTimeMillis();

        assertThat(token).isNotBlank();

        String username = jwtService.extractUsername(token);
        assertThat(username).isEqualTo("test@example.com");

        Date expiration = jwtService.extractExpiration(token);
        assertThat(expiration).isNotNull();

        // 15 minutes = 15 * 60 * 1000 = 900,000 ms
        long expTime = expiration.getTime();
        long diffMs = expTime - beforeGen;

        System.out.println("Current time ms: " + beforeGen);
        System.out.println("Expiration time ms: " + expTime);
        System.out.println("Difference in ms: " + diffMs);
        System.out.println("Difference in minutes: " + (diffMs / 60000.0));

        assertThat(diffMs).isGreaterThanOrEqualTo(15 * 60 * 1000 - 1000);
        assertThat(diffMs).isLessThanOrEqualTo(15 * 60 * 1000 + 5000);

        boolean isValid = jwtService.isTokenValid(token, userDetails);
        assertThat(isValid).isTrue();
    }

    @Test
    void testStompHeaderCaseSensitivity() {
        org.springframework.messaging.simp.stomp.StompHeaderAccessor accessor =
                org.springframework.messaging.simp.stomp.StompHeaderAccessor.create(org.springframework.messaging.simp.stomp.StompCommand.CONNECT);
        accessor.addNativeHeader("authorization", "Bearer test_token");

        String upperVal = accessor.getFirstNativeHeader("Authorization");
        String lowerVal = accessor.getFirstNativeHeader("authorization");

        // Native headers are case-sensitive by default in Spring StompHeaderAccessor
        assertThat(upperVal).isNull();
        assertThat(lowerVal).isEqualTo("Bearer test_token");
    }

    @Test
    void testApiResponseSerialization() throws Exception {
        com.moodcafe.auth.dto.auth.response.AuthResponse authResponse =
                com.moodcafe.auth.dto.auth.response.AuthResponse.builder()
                        .accessToken("test_access_token")
                        .refreshToken("test_refresh_token")
                        .tokenType("Bearer")
                        .expiresIn(900)
                        .build();

        com.moodcafe.shared.response.ApiResponse<com.moodcafe.auth.dto.auth.response.AuthResponse> apiResponse =
                com.moodcafe.shared.response.ApiResponse.success(authResponse, "Refresh token successfully");

        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        String json = mapper.writeValueAsString(apiResponse);

        assertThat(json).contains("\"code\":200");
        assertThat(json).contains("\"accessToken\":\"test_access_token\"");
        assertThat(json).contains("\"refreshToken\":\"test_refresh_token\"");
        assertThat(json).contains("\"tokenType\":\"Bearer\"");
    }
}


