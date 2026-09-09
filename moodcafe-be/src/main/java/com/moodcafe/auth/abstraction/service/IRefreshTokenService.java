package com.moodcafe.auth.abstraction.service;

import com.moodcafe.auth.entity.RefreshToken;
import com.moodcafe.auth.entity.User;

public interface IRefreshTokenService {
    String createRefreshToken(User user);

    RefreshToken validateRefreshToken(String token);

    void revokeToken(String token);

    void revokeAllUserTokens(User user);
}
