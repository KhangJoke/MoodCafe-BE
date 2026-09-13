package com.moodcafe.auth.service;

import com.moodcafe.auth.abstraction.service.SocialAuthService;
import com.moodcafe.auth.dto.auth.SocialUserInfo;
import com.moodcafe.shared.error.ErrorCode;
import com.moodcafe.shared.exceptions.AppException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Slf4j
@Service
public class GoogleAuthServiceImpl implements SocialAuthService {

    @Value("${google.client-id:}")
    private String googleClientId;

    private final RestClient restClient;

    public GoogleAuthServiceImpl() {
        this.restClient = RestClient.create();
    }

    // Constructor for testing / injection
    public GoogleAuthServiceImpl(RestClient restClient, String googleClientId) {
        this.restClient = restClient;
        this.googleClientId = googleClientId;
    }

    @Override
    public boolean supports(String provider) {
        return "GOOGLE".equalsIgnoreCase(provider);
    }

    @Override
    public SocialUserInfo verifyToken(String token) {
        if (token == null || token.isBlank()) {
            throw new AppException(ErrorCode.TOKEN_INVALID, "Google token is required");
        }

        Map<String, Object> userInfo;
        try {
            userInfo = restClient.get()
                    .uri("https://www.googleapis.com/oauth2/v3/userinfo")
                    .header("Authorization", "Bearer " + token.trim())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.error("Google userinfo verification failed: {}", e.getMessage());
            throw new AppException(ErrorCode.TOKEN_INVALID, "Invalid or expired Google token");
        }

        if (userInfo == null || !userInfo.containsKey("email")) {
            throw new AppException(ErrorCode.TOKEN_INVALID, "Invalid Google token");
        }

        // Verify email
        String email = (String) userInfo.get("email");
        if (email == null || email.isBlank()) {
            throw new AppException(ErrorCode.TOKEN_INVALID, "Google token does not contain email");
        }

        // Verify email_verified
        Object verifiedObj = userInfo.get("email_verified");
        if (verifiedObj == null) {
            verifiedObj = userInfo.get("verified_email");
        }
        boolean isEmailVerified = verifiedObj == null || Boolean.parseBoolean(String.valueOf(verifiedObj));
        if (!isEmailVerified) {
            throw new AppException(ErrorCode.TOKEN_INVALID, "Google account email is not verified");
        }

        String name = (String) userInfo.get("name");
        String picture = (String) userInfo.get("picture");
        String sub = (String) userInfo.getOrDefault("sub", userInfo.get("user_id"));

        return SocialUserInfo.builder()
                .provider("GOOGLE")
                .providerId(sub != null ? sub : email)
                .email(email.trim().toLowerCase())
                .fullName(name != null && !name.isBlank() ? name.trim() : email.split("@")[0])
                .avatarUrl(picture)
                .build();
    }
}
