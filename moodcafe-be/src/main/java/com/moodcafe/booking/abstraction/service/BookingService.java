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
import com.moodcafe.shared.response.PageResponse;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface BookingService {

    StoreDailyScheduleResponse getStoreDailySchedule(UUID storeId, LocalDate date);

    SlotAvailabilityResponse checkSlotAvailability(UUID storeId, UUID categoryId, Instant startTime);

    BookingResponse createBooking(CreateBookingRequest request);

    void confirmBookingPayment(Long paymentOrderCode);

    void confirmBookingPayment(Long paymentOrderCode, Long paidAmountVnd);

    BookingResponse getBookingDetail(UUID bookingId);

    BookingResponse getBookingByCode(String bookingCode);

    BookingResponse cancelBooking(UUID bookingId, CancelBookingRequest request);

    BookingResponse getBookingPaymentLink(UUID bookingId);

    void timeoutPendingBooking(UUID bookingId);

    List<BookingResponse> getMyBookings();

    PageResponse<BookingResponse> getMyBookings(Pageable pageable);

    PageResponse<BookingResponse> getMyBookings(String status, Pageable pageable);

    List<BookingResponse> getStoreDeskBookings(UUID storeId, LocalDate date, Instant from, Instant to);

    PageResponse<BookingResponse> getStoreDeskBookings(UUID storeId, LocalDate date, Instant from, Instant to, Pageable pageable);

    BookingDeskSummaryResponse getStoreDeskSummary(UUID storeId, LocalDate date);

    BookingResponse deskCheckIn(UUID storeId, DeskCheckInRequest request);

    BookingResponse deskClaimLate(UUID storeId, UUID bookingId);

    BookingResponse deskComplete(UUID storeId, UUID bookingId);

    BookingResponse requestCheckout(UUID bookingId);

    BookingResponse cancelCheckoutRequest(UUID bookingId);

    BookingResponse undoComplete(UUID storeId, UUID bookingId);

    BookingResponse updatePhysicalTable(UUID storeId, UUID bookingId, String physicalTableNumber);

    BookingResponse createWalkInHold(UUID storeId, UUID categoryId, Integer durationMinutes, String physicalTableNumber);

    BookingResponse releaseWalkInHold(UUID storeId, UUID bookingId);

    void dispatchBookingNotification(Booking booking, BookingEventType eventType, String customMsg);
}
