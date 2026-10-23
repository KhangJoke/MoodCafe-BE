package com.moodcafe.finance.service;

import com.moodcafe.booking.abstraction.repository.BookingRepository;
import com.moodcafe.booking.entity.Booking;
import com.moodcafe.shared.error.ErrorCode;
import com.moodcafe.shared.exceptions.AppException;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;

/** Recorded in the same transaction as booking state and the merchant wallet. */
@Service
@RequiredArgsConstructor
public class BookingCommissionService {
    private final JdbcTemplate jdbc;
    private final BookingRepository bookings;

    @Transactional(propagation = Propagation.MANDATORY)
    public boolean record(Booking booking, String event) {
        bookings.flush();
        bookings.findByIdForUpdate(booking.getBookingId()).orElseThrow();
        List<String> events = jdbc.query("select event_type from booking_commission_entries where booking_id = ?",
                (rs, n) -> rs.getString(1), booking.getBookingId());
        if (events.contains(event)) return false;
        UUID original = null;
        if (!"HOLD".equals(event)) {
            if (!events.contains("HOLD") || events.contains("SETTLE") || events.contains("REVERSE"))
                throw new AppException(ErrorCode.CONFLICT, "Đơn cần đối soát hoa hồng trước khi giải ngân hoặc hoàn tiền");
            original = jdbc.queryForObject("select entry_id from booking_commission_entries where booking_id = ? and event_type = 'HOLD'", UUID.class, booking.getBookingId());
        }
        return jdbc.update("""
                insert into booking_commission_entries(booking_id,store_id,event_type,amount,business_key,source,original_entry_id,occurred_at)
                values (?,?,?,?,?,'BOOKING_LIFECYCLE',?,?) on conflict do nothing
                """, booking.getBookingId(), booking.getStore().getStoreId(), event, booking.getCommissionAmount(),
                "BOOKING:" + booking.getBookingId() + ":" + event, original, Timestamp.from(Instant.now())) == 1;
    }
}
