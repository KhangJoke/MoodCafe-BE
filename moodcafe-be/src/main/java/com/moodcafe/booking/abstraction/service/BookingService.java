package com.moodcafe.booking.abstraction.service;

import com.moodcafe.booking.dto.request.CancelBookingRequest;
import com.moodcafe.booking.dto.request.CreateBookingRequest;
import com.moodcafe.booking.dto.request.DeskCheckInRequest;
import com.moodcafe.booking.dto.response.BookingDeskSummaryResponse;
import com.moodcafe.booking.dto.response.BookingResponse;
import com.moodcafe.booking.dto.response.SlotAvailabilityResponse;
import com.moodcafe.booking.dto.response.StoreDailyScheduleResponse;
import com.moodcafe.booking.entity.Booking;
import com.moodcafe.booking.entity.enums.BookingEventType;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface BookingService {

    StoreDailyScheduleResponse getStoreDailySchedule(UUID storeId, LocalDate date);

    SlotAvailabilityResponse checkSlotAvailability(UUID storeId, UUID categoryId, Instant startTime);

    BookingResponse createBooking(CreateBookingRequest request);

    void confirmBookingPayment(Long paymentOrderCode);

    BookingResponse getBookingDetail(UUID bookingId);

    BookingResponse getBookingByCode(String bookingCode);

    BookingResponse cancelBooking(UUID bookingId, CancelBookingRequest request);

    BookingResponse getBookingPaymentLink(UUID bookingId);

    List<BookingResponse> getMyBookings();

    List<BookingResponse> getStoreDeskBookings(UUID storeId, LocalDate date, Instant from, Instant to);

    BookingDeskSummaryResponse getStoreDeskSummary(UUID storeId, LocalDate date);

    BookingResponse deskCheckIn(UUID storeId, DeskCheckInRequest request);

    BookingResponse deskClaimLate(UUID storeId, UUID bookingId);

    BookingResponse deskComplete(UUID storeId, UUID bookingId);

    void dispatchBookingNotification(Booking booking, BookingEventType eventType, String customMsg);
}
