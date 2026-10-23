package com.moodcafe.reservation;

import com.moodcafe.reservation.service.ServiceDayDeadlineResolver;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ServiceDayDeadlineResolverTest {

    private final ServiceDayDeadlineResolver resolver = new ServiceDayDeadlineResolver();
    private final ZoneId zone = ZoneId.of("Asia/Ho_Chi_Minh");

    @Test
    @DisplayName("R10: Standard shift 07:00-21:00 with booking at 20:00 resolves to 21:00 same day")
    void testStandardShift() {
        // 2026-10-21 20:00:00 ICT
        Instant bookingTime = ZonedDateTime.of(2026, 10, 21, 20, 0, 0, 0, zone).toInstant();
        LocalTime opening = LocalTime.of(7, 0);
        LocalTime closing = LocalTime.of(21, 0);

        Instant deadline = resolver.resolve(bookingTime, opening, closing, zone);

        Instant expected = ZonedDateTime.of(2026, 10, 21, 21, 0, 0, 0, zone).toInstant();
        assertEquals(expected, deadline);
    }

    @Test
    @DisplayName("R10: Overnight shift 18:00-02:00 with booking at 23:30 resolves to 02:00 next day")
    void testOvernightShift_BeforeMidnight() {
        // 2026-10-21 23:30:00 ICT
        Instant bookingTime = ZonedDateTime.of(2026, 10, 21, 23, 30, 0, 0, zone).toInstant();
        LocalTime opening = LocalTime.of(18, 0);
        LocalTime closing = LocalTime.of(2, 0);

        Instant deadline = resolver.resolve(bookingTime, opening, closing, zone);

        Instant expected = ZonedDateTime.of(2026, 10, 22, 2, 0, 0, 0, zone).toInstant();
        assertEquals(expected, deadline);
    }

    @Test
    @DisplayName("R10: Overnight shift 18:00-02:00 with booking at 00:30 resolves to 02:00 same day (end of previous night's service)")
    void testOvernightShift_AfterMidnight() {
        // 2026-10-22 00:30:00 ICT
        Instant bookingTime = ZonedDateTime.of(2026, 10, 22, 0, 30, 0, 0, zone).toInstant();
        LocalTime opening = LocalTime.of(18, 0);
        LocalTime closing = LocalTime.of(2, 0);

        Instant deadline = resolver.resolve(bookingTime, opening, closing, zone);

        Instant expected = ZonedDateTime.of(2026, 10, 22, 2, 0, 0, 0, zone).toInstant();
        assertEquals(expected, deadline);
    }
}
