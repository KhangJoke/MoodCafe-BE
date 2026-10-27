package com.moodcafe.finance.abstraction.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Dedicated repository for booking commission entries table.
 * Isolates direct JDBC SQL execution from the service layer.
 */
@Repository
@RequiredArgsConstructor
public class BookingCommissionEntryRepository {

    private final JdbcTemplate jdbc;

    public List<String> findEventTypesByBookingId(UUID bookingId) {
        String sql = "SELECT event_type FROM booking_commission_entries WHERE booking_id = ?";
        return jdbc.query(sql, (rs, n) -> rs.getString(1), bookingId);
    }

    public UUID findHoldEntryIdByBookingId(UUID bookingId) {
        String sql = "SELECT entry_id FROM booking_commission_entries WHERE booking_id = ? AND event_type = 'HOLD'";
        try {
            return jdbc.queryForObject(sql, UUID.class, bookingId);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    public boolean insertEntry(UUID bookingId, UUID storeId, String eventType, BigDecimal amount,
                               String businessKey, String source, UUID originalEntryId, Instant occurredAt) {
        String sql = """
                INSERT INTO booking_commission_entries(
                    booking_id, store_id, event_type, amount, business_key, source, original_entry_id, occurred_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT DO NOTHING
                """;
        return jdbc.update(
                sql,
                bookingId,
                storeId,
                eventType,
                amount,
                businessKey,
                source,
                originalEntryId,
                Timestamp.from(occurredAt)
        ) == 1;
    }
}
