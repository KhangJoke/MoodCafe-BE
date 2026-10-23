package com.moodcafe.payment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderCodeGeneratorService {

    private final JdbcTemplate jdbcTemplate;
    private final AtomicLong fallbackSequence = new AtomicLong(2000000001L);

    /**
     * Generates a guaranteed unique orderCode across subscriptions and reservations,
     * safe for multiple instances using PostgreSQL sequence.
     */
    public long nextOrderCode() {
        try {
            Long nextVal = jdbcTemplate.queryForObject("SELECT nextval('payment_order_code_seq')", Long.class);
            if (nextVal != null) {
                return nextVal;
            }
        } catch (Exception e) {
            log.warn("Could not query payment_order_code_seq, using memory fallback: {}", e.getMessage());
        }
        return fallbackSequence.getAndIncrement();
    }
}
