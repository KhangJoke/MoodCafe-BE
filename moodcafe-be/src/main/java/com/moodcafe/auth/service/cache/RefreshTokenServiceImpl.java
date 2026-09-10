package com.moodcafe.auth.service.cache;

import com.moodcafe.auth.abstraction.repository.RefreshTokenRepository;
import com.moodcafe.auth.abstraction.service.RefreshTokenService;
import com.moodcafe.auth.entity.RefreshToken;
import com.moodcafe.auth.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${jwt.refresh-token-expiration-days:7}")
    private long refreshTokenExpirationDays;

    @Override
    public String createRefreshToken(User user) {

        RefreshToken refreshToken = RefreshToken.builder()
                .token(UUID.randomUUID().toString())
                .user(user)
                .expiresAt(
                        LocalDateTime.now()
                                .plusDays(refreshTokenExpirationDays)
                )
                .revoked(false)
                .createdAt(LocalDateTime.now())
                .build();

        refreshTokenRepository.save(refreshToken);

        return refreshToken.getToken();
    }

    @Override
    public RefreshToken validateRefreshToken(String token) {

        RefreshToken refreshToken =
                refreshTokenRepository.findByToken(token)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Invalid refresh token"
                                )
                        );

        // Refresh token reuse detection
        if (Boolean.TRUE.equals(refreshToken.getRevoked())) {

            revokeAllUserTokens(refreshToken.getUser());

            throw new RuntimeException(
                    "Refresh token revoked"
            );
        }

        // Expiration check
        if (refreshToken.getExpiresAt()
                .isBefore(LocalDateTime.now())) {

            throw new RuntimeException(
                    "Refresh token expired"
            );
        }

        return refreshToken;
    }

    @Override
    public void revokeToken(String token) {

        RefreshToken refreshToken =
                refreshTokenRepository.findByToken(token)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Refresh token not found"
                                )
                        );

        if (Boolean.TRUE.equals(refreshToken.getRevoked())) {
            return;
        }

        refreshToken.setRevoked(true);

        refreshTokenRepository.save(refreshToken);
    }

    @Override
    public void revokeAllUserTokens(User user) {

        List<RefreshToken> validTokens =
                refreshTokenRepository
                        .findByUserAndRevokedFalse(user);

        if (validTokens.isEmpty()) {
            return;
        }

        validTokens.forEach(
                token -> token.setRevoked(true)
        );

        refreshTokenRepository.saveAll(validTokens);
    }
}