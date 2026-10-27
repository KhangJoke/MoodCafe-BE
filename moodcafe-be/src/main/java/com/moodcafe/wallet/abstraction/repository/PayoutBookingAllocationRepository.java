package com.moodcafe.wallet.abstraction.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Dedicated repository for payout booking allocations and withdrawal accounting queries.
 * Isolates direct JDBC SQL execution from PayoutBreakdownService.
 */
@Repository
@RequiredArgsConstructor
public class PayoutBookingAllocationRepository {

    private final JdbcTemplate jdbc;

    public BigDecimal findLegacyUsedAmount(UUID storeId, UUID withdrawalId) {
        String sql = """
            SELECT COALESCE(SUM(amount), 0)
            FROM merchant_withdrawals w
            WHERE store_id = ?
              AND withdrawal_id <> ?
              AND status NOT IN ('REJECTED', 'CANCELLED')
              AND NOT EXISTS (
                  SELECT 1 FROM payout_booking_allocations a WHERE a.withdrawal_id = w.withdrawal_id
              )
            """;
        return jdbc.queryForObject(sql, BigDecimal.class, storeId, withdrawalId);
    }

    public List<Map<String, Object>> findEligibleCredits(UUID storeId) {
        String sql = """
            SELECT t.transaction_id,
                   t.reference_type,
                   t.reference_id,
                   b.booking_id,
                   COALESCE(b.booking_code, t.reference_id, 'LEGACY') AS booking_code,
                   t.amount - COALESCE((
                       SELECT SUM(a.amount)
                       FROM payout_booking_allocations a
                       JOIN merchant_withdrawals w ON w.withdrawal_id = a.withdrawal_id
                       WHERE a.source_transaction_id = t.transaction_id
                         AND w.status NOT IN ('REJECTED', 'CANCELLED')
                   ), 0) AS remaining
            FROM merchant_wallet_transactions t
            JOIN merchant_wallets mw ON mw.wallet_id = t.wallet_id
            LEFT JOIN bookings b ON t.reference_type = 'BOOKING' AND t.reference_id = b.booking_id::text
            WHERE mw.store_id = ?
              AND t.tx_type = 'RELEASE_TO_AVAILABLE'
              AND t.amount > 0
            ORDER BY t.created_at, t.transaction_id
            """;
        return jdbc.queryForList(sql, storeId);
    }

    public void insertAllocation(UUID withdrawalId, Object sourceTxId, Object bookingId,
                                 Object bookingCode, Object referenceType, Object referenceId, BigDecimal amount) {
        String sql = """
            INSERT INTO payout_booking_allocations(
                withdrawal_id, source_transaction_id, booking_id, booking_code, source_reference_type, source_reference_id, amount
            ) VALUES (?, ?, ?, ?, ?, ?, ?)
            """;
        jdbc.update(sql, withdrawalId, sourceTxId, bookingId, bookingCode, referenceType, referenceId, amount);
    }

    public boolean existsWithdrawal(UUID withdrawalId) {
        String sql = "SELECT COUNT(*) FROM merchant_withdrawals WHERE withdrawal_id = ?";
        Long count = jdbc.queryForObject(sql, Long.class, withdrawalId);
        return count != null && count > 0;
    }

    public List<Map<String, Object>> findAllocationsByWithdrawalId(UUID withdrawalId) {
        String sql = """
            SELECT source_transaction_id AS "sourceTransactionId",
                   booking_id AS "bookingId",
                   booking_code AS "bookingCode",
                   amount
            FROM payout_booking_allocations
            WHERE withdrawal_id = ?
            ORDER BY booking_code
            """;
        return jdbc.queryForList(sql, withdrawalId);
    }
}
