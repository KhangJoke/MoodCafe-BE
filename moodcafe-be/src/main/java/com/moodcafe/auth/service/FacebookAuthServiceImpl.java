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
public class FacebookAuthServiceImpl implements SocialAuthService {

    @Value("${facebook.app-id:}")
    private String facebookAppId;

    @Value("${facebook.app-secret:}")
    private String facebookAppSecret;

    private final RestClient restClient;

    public FacebookAuthServiceImpl() {
        this.restClient = RestClient.create();
    }

    public FacebookAuthServiceImpl(RestClient restClient, String facebookAppId, String facebookAppSecret) {
        this.restClient = restClient;
        this.facebookAppId = facebookAppId;
        this.facebookAppSecret = facebookAppSecret;
    }

    @Override
    public boolean supports(String provider) {
        return "FACEBOOK".equalsIgnoreCase(provider);
    }

    @Override
    public SocialUserInfo verifyToken(String token) {
        if (token == null || token.isBlank()) {
            throw new AppException(ErrorCode.TOKEN_INVALID, "Facebook token is required");
        }

        try {
            Map<String, Object> response = restClient.get()
                    .uri("https://graph.facebook.com/me?fields=id,name,email,picture.type(large)&access_token={token}", token.trim())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});

            if (response == null || response.isEmpty()) {
                throw new AppException(ErrorCode.TOKEN_INVALID, "Invalid Facebook token response");
            }

            String email = (String) response.get("email");
            String id = (String) response.get("id");
            String name = (String) response.get("name");

            String avatarUrl = null;
            if (response.get("picture") instanceof Map<?, ?> pictureMap) {
                if (pictureMap.get("data") instanceof Map<?, ?> dataMap) {
                    avatarUrl = (String) dataMap.get("url");
                }
            }

            if (email == null || email.isBlank()) {
                if (id != null) {
                    email = id + "@facebook.moodcafe.vn";
                } else {
                    throw new AppException(ErrorCode.TOKEN_INVALID, "Facebook account does not provide email or id");
                }
            }

            return SocialUserInfo.builder()
                    .provider("FACEBOOK")
                    .providerId(id != null ? id : email)
                    .email(email.trim().toLowerCase())
                    .fullName(name != null && !name.isBlank() ? name.trim() : email.split("@")[0])
                    .avatarUrl(avatarUrl)
                    .build();

        } catch (RestClientResponseException e) {
            log.error("Facebook Graph API verification failed: status={}, body={}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            throw new AppException(ErrorCode.TOKEN_INVALID, "Invalid or expired Facebook token");
        } catch (AppException ae) {
            throw ae;
        } catch (Exception e) {
            log.error("Error communicating with Facebook Graph API: {}", e.getMessage());
            throw new AppException(ErrorCode.TOKEN_INVALID, "Failed to verify Facebook token: " + e.getMessage());
        }
    }
}
