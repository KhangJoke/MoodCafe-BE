package com.moodcafe.auth.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
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

import java.util.Collections;
import java.util.Map;

@Slf4j
@Service
public class GoogleAuthServiceImpl implements SocialAuthService {

    @Value("${google.client-id:}")
    private String googleClientId;

    private final RestClient restClient;
    private final GoogleIdTokenVerifier tokenVerifier;

    public GoogleAuthServiceImpl() {
        this(RestClient.create(), null);
    }

    public GoogleAuthServiceImpl(RestClient restClient, String googleClientId) {
        this.restClient = restClient != null ? restClient : RestClient.create();
        this.googleClientId = googleClientId;

        NetHttpTransport transport = new NetHttpTransport();
        GsonFactory jsonFactory = GsonFactory.getDefaultInstance();

        GoogleIdTokenVerifier.Builder verifierBuilder = new GoogleIdTokenVerifier.Builder(transport, jsonFactory);
        if (googleClientId != null && !googleClientId.isBlank()) {
            verifierBuilder.setAudience(Collections.singletonList(googleClientId.trim()));
        }
        this.tokenVerifier = verifierBuilder.build();
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

        String cleanToken = token.trim();

        // 1. Primary official method per Google API Client Library documentation:
        // Use GoogleIdTokenVerifier to cryptographically verify Google ID Token (JWT)
        if (isJwtFormat(cleanToken)) {
            try {
                GoogleIdToken idToken = tokenVerifier.verify(cleanToken);
                if (idToken != null) {
                    Payload payload = idToken.getPayload();
                    return extractSocialUserInfoFromPayload(payload);
                }
                log.warn("GoogleIdTokenVerifier returned null for token, trying fallback verification...");
            } catch (Exception e) {
                log.warn("GoogleIdTokenVerifier verification exception: {}. Trying fallback verification...", e.getMessage());
            }

            // Fallback 1 for ID Token: Google tokeninfo REST endpoint
            try {
                Map<String, Object> tokenInfo = restClient.get()
                        .uri("https://oauth2.googleapis.com/tokeninfo?id_token=" + cleanToken)
                        .accept(MediaType.APPLICATION_JSON)
                        .retrieve()
                        .body(new ParameterizedTypeReference<Map<String, Object>>() {});

                if (tokenInfo != null && tokenInfo.containsKey("email")) {
                    return extractSocialUserInfoFromMap(tokenInfo);
                }
            } catch (Exception e) {
                log.warn("Google tokeninfo endpoint failed: {}", e.getMessage());
            }
        }

        // 2. Fallback for OAuth2 Access Token (e.g., ya29... bearer token from web or manual OAuth):
        // Calls Google UserInfo endpoint per Google OAuth2 documentation
        try {
            Map<String, Object> userInfo = restClient.get()
                    .uri("https://www.googleapis.com/oauth2/v3/userinfo")
                    .header("Authorization", "Bearer " + cleanToken)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});

            if (userInfo != null && userInfo.containsKey("email")) {
                return extractSocialUserInfoFromMap(userInfo);
            }
        } catch (Exception e) {
            log.error("Google userinfo verification failed: {}", e.getMessage());
        }

        throw new AppException(ErrorCode.TOKEN_INVALID, "Invalid or expired Google token");
    }

    private boolean isJwtFormat(String token) {
        return token.chars().filter(ch -> ch == '.').count() == 2;
    }

    private SocialUserInfo extractSocialUserInfoFromPayload(Payload payload) {
        String email = payload.getEmail();
        if (email == null || email.isBlank()) {
            throw new AppException(ErrorCode.TOKEN_INVALID, "Google token does not contain email");
        }

        boolean isEmailVerified = Boolean.TRUE.equals(payload.getEmailVerified());
        if (!isEmailVerified) {
            throw new AppException(ErrorCode.TOKEN_INVALID, "Google account email is not verified");
        }

        String name = (String) payload.get("name");
        String picture = (String) payload.get("picture");
        String sub = payload.getSubject();

        return SocialUserInfo.builder()
                .provider("GOOGLE")
                .providerId(sub != null ? sub : email)
                .email(email.trim().toLowerCase())
                .fullName(name != null && !name.isBlank() ? name.trim() : email.split("@")[0])
                .avatarUrl(picture)
                .build();
    }

    private SocialUserInfo extractSocialUserInfoFromMap(Map<String, Object> map) {
        String email = (String) map.get("email");
        if (email == null || email.isBlank()) {
            throw new AppException(ErrorCode.TOKEN_INVALID, "Google token does not contain email");
        }

        Object verifiedObj = map.get("email_verified");
        if (verifiedObj == null) {
            verifiedObj = map.get("verified_email");
        }
        boolean isEmailVerified = verifiedObj == null || Boolean.parseBoolean(String.valueOf(verifiedObj));
        if (!isEmailVerified) {
            throw new AppException(ErrorCode.TOKEN_INVALID, "Google account email is not verified");
        }

        String name = (String) map.get("name");
        String picture = (String) map.get("picture");
        String sub = (String) map.getOrDefault("sub", map.get("user_id"));

        return SocialUserInfo.builder()
                .provider("GOOGLE")
                .providerId(sub != null ? sub : email)
                .email(email.trim().toLowerCase())
                .fullName(name != null && !name.isBlank() ? name.trim() : email.split("@")[0])
                .avatarUrl(picture)
                .build();
    }
}
