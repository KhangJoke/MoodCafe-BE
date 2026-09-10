package com.moodcafe.notification.abstraction.cache;

public interface RedisRateLimitService {

    boolean isAllowed(String type, String key);
}
