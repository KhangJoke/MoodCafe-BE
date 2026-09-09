package com.moodcafe.notification.abstraction.cache;

public interface IRedisRateLimitService {

    boolean isAllowed(String type, String key);
}
