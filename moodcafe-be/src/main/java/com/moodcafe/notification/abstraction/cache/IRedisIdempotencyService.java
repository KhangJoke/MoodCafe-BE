package com.moodcafe.notification.abstraction.cache;

public interface IRedisIdempotencyService {

    boolean isProcessed(String eventId);

    void markProcessed(String eventId);
}
