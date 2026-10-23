package com.moodcafe.reservation.service;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@Component
public class ServiceDayDeadlineResolver {

    public static final ZoneId DEFAULT_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    /**
     * Resolves the service closing deadline for a reservation based on store schedule and booking time.
     * Handles normal shifts (e.g. 07:00 - 21:00) and overnight shifts (e.g. 18:00 - 02:00).
     */
    public Instant resolve(Instant bookingTime, LocalTime openingTime, LocalTime closingTime, ZoneId zoneId) {
        if (bookingTime == null) {
            throw new IllegalArgumentException("bookingTime cannot be null");
        }
        ZoneId zone = zoneId != null ? zoneId : DEFAULT_ZONE;
        ZonedDateTime zdt = bookingTime.atZone(zone);
        LocalDate bookingDate = zdt.toLocalDate();
        LocalTime bookingLocalTime = zdt.toLocalTime();

        // Default fallback if store schedule is not configured
        if (closingTime == null) {
            closingTime = LocalTime.of(22, 0);
        }
        if (openingTime == null) {
            openingTime = LocalTime.of(7, 0);
        }

        LocalDate serviceEndDate;
        if (!closingTime.isBefore(openingTime)) {
            // Case 1: Standard same-day shift (e.g. 07:00 to 21:00)
            serviceEndDate = bookingDate;
        } else {
            // Case 2: Overnight shift (e.g. 18:00 to 02:00)
            if (!bookingLocalTime.isBefore(openingTime)) {
                serviceEndDate = bookingDate.plusDays(1);
            } else {
                serviceEndDate = bookingDate;
            }
        }

        ZonedDateTime closingZdt = ZonedDateTime.of(serviceEndDate, closingTime, zone);
        return closingZdt.toInstant();
    }
}
