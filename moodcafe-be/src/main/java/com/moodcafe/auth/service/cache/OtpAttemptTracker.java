package com.moodcafe.auth.service.cache;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class OtpAttemptTracker {

    private final RedisTemplate<String, String> redisTemplate;

    private static final String ATTEMPT_KEY_PREFIX = "OTP_ATTEMPT:";
    private static final String LOCKOUT_KEY_PREFIX = "OTP_LOCKOUT:";
    private static final int MAX_ATTEMPTS = 5;
    private static final long ATTEMPT_WINDOW = 10; // minutes
    private static final long LOCKOUT_DURATION = 15; // minutes

    /**
     * Check if email is locked out
     */
    public boolean isLockedOut(String email) {
        String lockoutKey = LOCKOUT_KEY_PREFIX + email;
        return redisTemplate.hasKey(lockoutKey);
    }

    /**
     * Record a failed OTP attempt
     * @return current attempt count
     */
    public int recordFailedAttempt(String email) {
        String attemptKey = ATTEMPT_KEY_PREFIX + email;
        
        Long attempts = redisTemplate.opsForValue().increment(attemptKey);
        if (attempts == null) {
            attempts = 1L;
        }
        
        // Set expiry on first attempt
        if (attempts.equals(1L)) {
            redisTemplate.expire(attemptKey, ATTEMPT_WINDOW, TimeUnit.MINUTES);
        }
        
        // Lock out if max attempts exceeded
        if (attempts > MAX_ATTEMPTS) {
            lockoutEmail(email);
        }
        int ans = MAX_ATTEMPTS - attempts.intValue();
        return ans;
    }


    /**
     * Reset attempts on successful OTP validation
     */
    public void resetAttempts(String email) {
        String attemptKey = ATTEMPT_KEY_PREFIX + email;
        redisTemplate.delete(attemptKey);
    }

    /**
     * Get lockout remaining time in seconds
     */
    public long getLockoutRemainingTime(String email) {
        String lockoutKey = LOCKOUT_KEY_PREFIX + email;
        Long ttl = redisTemplate.getExpire(lockoutKey, TimeUnit.SECONDS);
        return ttl > 0 ? ttl : 0;
    }

    /**
     * Lock out an email
     */
    private void lockoutEmail(String email) {
        String lockoutKey = LOCKOUT_KEY_PREFIX + email;
        redisTemplate.opsForValue().set(lockoutKey, "locked", LOCKOUT_DURATION, TimeUnit.MINUTES);
        // Delete attempts counter when locked out
        redisTemplate.delete(ATTEMPT_KEY_PREFIX + email);
    }
}

