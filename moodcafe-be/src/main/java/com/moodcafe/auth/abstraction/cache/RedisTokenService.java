package com.moodcafe.auth.abstraction.cache;

public interface RedisTokenService {

    void blacklistToken(String jti, long ttlMs);

    boolean isBlacklisted(String jti);
}
