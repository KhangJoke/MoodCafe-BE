package com.moodcafe.finance.service;

import com.moodcafe.booking.abstraction.repository.BookingRepository;
import com.moodcafe.booking.entity.Booking;
import com.moodcafe.finance.abstraction.repository.BookingCommissionEntryRepository;
import com.moodcafe.shared.error.ErrorCode;
import com.moodcafe.shared.exceptions.AppException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Service orchestrating booking commission lifecycle events.
 * Database access is delegated to repositories; no direct JDBC inside this service.
 */
@Service
@RequiredArgsConstructor
public class BookingCommissionService {

    private final BookingCommissionEntryRepository commissionEntryRepository;
    private final BookingRepository bookings;

    @Transactional(propagation = Propagation.MANDATORY)
    public boolean record(Booking booking, String event) {
        bookings.flush();
        bookings.findByIdForUpdate(booking.getBookingId()).orElseThrow();

        List<String> events = commissionEntryRepository.findEventTypesByBookingId(booking.getBookingId());
        if (events.contains(event)) {
            return false;
        }

        UUID original = null;
        if (!"HOLD".equals(event)) {
            if (!events.contains("HOLD") || events.contains("SETTLE") || events.contains("REVERSE")) {
                throw new AppException(ErrorCode.CONFLICT, "Đơn cần đối soát hoa hồng trước khi giải ngân hoặc hoàn tiền");
            }
            original = commissionEntryRepository.findHoldEntryIdByBookingId(booking.getBookingId());
        }

        return commissionEntryRepository.insertEntry(
                booking.getBookingId(),
                booking.getStore().getStoreId(),
                event,
                booking.getCommissionAmount(),
                "BOOKING:" + booking.getBookingId() + ":" + event,
                "BOOKING_LIFECYCLE",
                original,
                Instant.now()
        );
    }
}
