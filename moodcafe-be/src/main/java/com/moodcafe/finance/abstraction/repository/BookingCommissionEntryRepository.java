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

    public BigDecimal sumSettledCommissionAllTime() {
        String sql = "SELECT COALESCE(SUM(amount), 0) FROM booking_commission_entries WHERE event_type = 'SETTLE'";
        BigDecimal val = jdbc.queryForObject(sql, BigDecimal.class);
        return val != null ? val : BigDecimal.ZERO;
    }

    public BigDecimal sumSettledCommissionBetween(Instant from, Instant to) {
        String sql = "SELECT COALESCE(SUM(amount), 0) FROM booking_commission_entries WHERE event_type = 'SETTLE' AND occurred_at >= ? AND occurred_at < ?";
        BigDecimal val = jdbc.queryForObject(sql, BigDecimal.class, Timestamp.from(from), Timestamp.from(to));
        return val != null ? val : BigDecimal.ZERO;
    }

    public long countSettledBookingsAllTime() {
        String sql = "SELECT COUNT(*) FROM booking_commission_entries WHERE event_type = 'SETTLE'";
        Long val = jdbc.queryForObject(sql, Long.class);
        return val != null ? val : 0L;
    }

    public long countSettledBookingsBetween(Instant from, Instant to) {
        String sql = "SELECT COUNT(*) FROM booking_commission_entries WHERE event_type = 'SETTLE' AND occurred_at >= ? AND occurred_at < ?";
        Long val = jdbc.queryForObject(sql, Long.class, Timestamp.from(from), Timestamp.from(to));
        return val != null ? val : 0L;
    }

    public BigDecimal sumPendingCommissionAllTime() {
        String sql = """
                SELECT COALESCE(SUM(
                    CASE WHEN event_type = 'HOLD' THEN amount
                         WHEN event_type IN ('SETTLE', 'REVERSE') THEN -amount
                         ELSE 0 END
                ), 0)
                FROM booking_commission_entries
                """;
        BigDecimal val = jdbc.queryForObject(sql, BigDecimal.class);
        return val != null ? val.max(BigDecimal.ZERO) : BigDecimal.ZERO;
    }

    public BigDecimal sumReversedCommissionAllTime() {
        String sql = "SELECT COALESCE(SUM(amount), 0) FROM booking_commission_entries WHERE event_type = 'REVERSE'";
        BigDecimal val = jdbc.queryForObject(sql, BigDecimal.class);
        return val != null ? val : BigDecimal.ZERO;
    }

    public BigDecimal sumReversedCommissionBetween(Instant from, Instant to) {
        String sql = "SELECT COALESCE(SUM(amount), 0) FROM booking_commission_entries WHERE event_type = 'REVERSE' AND occurred_at >= ? AND occurred_at < ?";
        BigDecimal val = jdbc.queryForObject(sql, BigDecimal.class, Timestamp.from(from), Timestamp.from(to));
        return val != null ? val : BigDecimal.ZERO;
    }

    public List<SettledCommissionPoint> findSettledPointsBetween(Instant from, Instant to) {
        String sql = """
                SELECT amount, occurred_at
                FROM booking_commission_entries
                WHERE event_type = 'SETTLE'
                  AND occurred_at >= ? AND occurred_at < ?
                ORDER BY occurred_at ASC
                """;
        return jdbc.query(sql, (rs, rowNum) -> new SettledCommissionPoint(
                rs.getBigDecimal("amount"),
                rs.getTimestamp("occurred_at").toInstant()
        ), Timestamp.from(from), Timestamp.from(to));
    }

    public List<SettledCommissionLedgerRow> findSettledLedgerRowsBetween(Instant from, Instant to) {
        String sql = """
                SELECT e.entry_id, e.booking_id, b.booking_code, s.name AS store_name,
                       b.customer_name, e.amount, e.occurred_at
                FROM booking_commission_entries e
                LEFT JOIN bookings b ON b.booking_id = e.booking_id
                LEFT JOIN stores s ON s.store_id = e.store_id
                WHERE e.event_type = 'SETTLE'
                  AND e.occurred_at >= ? AND e.occurred_at < ?
                ORDER BY e.occurred_at DESC
                """;
        return jdbc.query(sql, (rs, rowNum) -> new SettledCommissionLedgerRow(
                rs.getObject("entry_id", UUID.class),
                rs.getObject("booking_id", UUID.class),
                rs.getString("booking_code"),
                rs.getString("store_name"),
                rs.getString("customer_name"),
                rs.getBigDecimal("amount"),
                rs.getTimestamp("occurred_at").toInstant()
        ), Timestamp.from(from), Timestamp.from(to));
    }

    public record SettledCommissionPoint(BigDecimal amount, Instant occurredAt) {}

    public record SettledCommissionLedgerRow(
            UUID entryId,
            UUID bookingId,
            String bookingCode,
            String storeName,
            String customerName,
            BigDecimal amount,
            Instant occurredAt
    ) {}
}
