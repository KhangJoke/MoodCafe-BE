package com.moodcafe.notification.abstraction.cache;

public interface RedisIdempotencyService {

    boolean isProcessed(String eventId);

    void markProcessed(String eventId);
}
