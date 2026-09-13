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
import org.springframework.web.client.RestClientResponseException;

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

        Map<String, Object> tokenInfo = fetchTokenInfo(token.trim());
        if (tokenInfo == null || tokenInfo.isEmpty()) {
            throw new AppException(ErrorCode.TOKEN_INVALID, "Failed to verify Google token");
        }

        // 1. Verify audience (aud or azp) matches client-id
        String aud = (String) tokenInfo.get("aud");
        String azp = (String) tokenInfo.get("azp");
        if (googleClientId != null && !googleClientId.isBlank()) {
            boolean audMatch = googleClientId.equals(aud) || googleClientId.equals(azp);
            if (!audMatch) {
                log.error("Google token audience mismatch. Expected: {}, Got aud: {}, azp: {}", googleClientId, aud, azp);
                throw new AppException(ErrorCode.TOKEN_INVALID, "Google token audience mismatch");
            }
        }

        // 2. Verify email
        String email = (String) tokenInfo.get("email");
        if (email == null || email.isBlank()) {
            throw new AppException(ErrorCode.TOKEN_INVALID, "Google token does not contain email");
        }

        // 3. Verify email_verified
        Object verifiedObj = tokenInfo.get("email_verified");
        if (verifiedObj == null) {
            verifiedObj = tokenInfo.get("verified_email");
        }
        boolean isEmailVerified = Boolean.parseBoolean(String.valueOf(verifiedObj));
        if (!isEmailVerified) {
            throw new AppException(ErrorCode.TOKEN_INVALID, "Google account email is not verified");
        }

        String name = (String) tokenInfo.get("name");
        String picture = (String) tokenInfo.get("picture");
        String sub = (String) tokenInfo.getOrDefault("sub", tokenInfo.get("user_id"));

        // If name or picture are missing, try fetching from userinfo endpoint
        if ((name == null || picture == null) && !token.contains(".")) {
            try {
                Map<String, Object> userInfo = fetchUserInfoWithBearer(token.trim());
                if (userInfo != null) {
                    if (name == null) {
                        name = (String) userInfo.get("name");
                    }
                    if (picture == null) {
                        picture = (String) userInfo.get("picture");
                    }
                }
            } catch (Exception e) {
                log.warn("Could not fetch additional userinfo from Google with access token: {}", e.getMessage());
            }
        }

        return SocialUserInfo.builder()
                .provider("GOOGLE")
                .providerId(sub != null ? sub : email)
                .email(email.trim().toLowerCase())
                .fullName(name != null && !name.isBlank() ? name.trim() : email.split("@")[0])
                .avatarUrl(picture)
                .build();
    }

    private Map<String, Object> fetchTokenInfo(String token) {
        boolean isJwt = token.contains(".");
        String paramName = isJwt ? "id_token" : "access_token";

        try {
            return restClient.get()
                    .uri("https://oauth2.googleapis.com/tokeninfo?{param}={token}", paramName, token)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});
        } catch (RestClientResponseException e) {
            log.warn("Failed verifying Google token with param {}: status={}, body={}",
                    paramName, e.getStatusCode(), e.getResponseBodyAsString());
            String fallbackParam = isJwt ? "access_token" : "id_token";
            try {
                return restClient.get()
                        .uri("https://oauth2.googleapis.com/tokeninfo?{param}={token}", fallbackParam, token)
                        .accept(MediaType.APPLICATION_JSON)
                        .retrieve()
                        .body(new ParameterizedTypeReference<Map<String, Object>>() {});
            } catch (Exception ex) {
                log.error("Google tokeninfo fallback validation failed: {}", ex.getMessage());
                throw new AppException(ErrorCode.TOKEN_INVALID, "Invalid or expired Google token");
            }
        } catch (AppException ae) {
            throw ae;
        } catch (Exception e) {
            log.error("Error communicating with Google tokeninfo: {}", e.getMessage());
            throw new AppException(ErrorCode.TOKEN_INVALID, "Failed to verify Google token: " + e.getMessage());
        }
    }

    private Map<String, Object> fetchUserInfoWithBearer(String accessToken) {
        return restClient.get()
                .uri("https://www.googleapis.com/oauth2/v3/userinfo")
                .header("Authorization", "Bearer " + accessToken)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, Object>>() {});
    }
}
