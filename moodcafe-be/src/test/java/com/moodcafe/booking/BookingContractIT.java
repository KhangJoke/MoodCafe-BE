package com.moodcafe.booking;

import com.moodcafe.auth.abstraction.service.CurrentUserService;
import com.moodcafe.auth.entity.User;
import com.moodcafe.booking.abstraction.service.BookingService;
import com.moodcafe.booking.controller.BookingController;
import com.moodcafe.booking.dto.request.BookingQuoteRequest;
import com.moodcafe.booking.dto.request.CreateBookingRequest;
import com.moodcafe.booking.dto.response.BookingCustomerDetailResponse;
import com.moodcafe.booking.dto.response.BookingQuoteResponse;
import com.moodcafe.booking.dto.response.BookingResponse;
import com.moodcafe.booking.dto.response.BookingStaffDetailResponse;
import com.moodcafe.booking.entity.enums.BookingStatus;
import com.moodcafe.shared.response.ApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingContractIT {

    @Mock
    private BookingService bookingService;

    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private BookingController bookingController;

    private UUID storeId;
    private UUID categoryId;
    private UUID bookingId;

    @BeforeEach
    void setUp() {
        storeId = UUID.randomUUID();
        categoryId = UUID.randomUUID();
        bookingId = UUID.randomUUID();
    }

    @Test
    @DisplayName("POST /api/bookings/quote returns 200 with BookingQuoteResponse")
    void testGetBookingQuoteEndpoint() {
        BookingQuoteRequest request = BookingQuoteRequest.builder()
                .storeId(storeId)
                .categoryId(categoryId)
                .startTime(Instant.now().plusSeconds(3600))
                .guestCount(2)
                .build();

        BookingQuoteResponse expectedResponse = BookingQuoteResponse.builder()
                .quoteId(UUID.randomUUID())
                .grossAmount(new BigDecimal("100000"))
                .voucherDiscount(BigDecimal.ZERO)
                .customerPaidAmount(new BigDecimal("100000"))
                .currency("VND")
                .isEligible(true)
                .build();

        when(bookingService.getBookingQuote(any(BookingQuoteRequest.class))).thenReturn(expectedResponse);

        ResponseEntity<ApiResponse<BookingQuoteResponse>> response = bookingController.getBookingQuote(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(new BigDecimal("100000"), response.getBody().getData().getCustomerPaidAmount());
        verify(bookingService).getBookingQuote(any(BookingQuoteRequest.class));
    }

    @Test
    @DisplayName("POST /api/bookings preserves Idempotency-Key header in request")
    void testCreateBookingWithIdempotencyKey() {
        CreateBookingRequest request = CreateBookingRequest.builder()
                .storeId(storeId)
                .categoryId(categoryId)
                .startTime(Instant.now().plusSeconds(3600))
                .guestCount(2)
                .customerName("Nguyen Van A")
                .customerPhone("0901234567")
                .build();

        BookingResponse expectedBooking = BookingResponse.builder()
                .bookingId(bookingId)
                .bookingCode("MC123456")
                .status(BookingStatus.PENDING_PAYMENT)
                .customerPaidAmount(new BigDecimal("90000"))
                .build();

        when(bookingService.createBooking(any(CreateBookingRequest.class))).thenReturn(expectedBooking);

        String intentKey = "intent-key-12345";
        ResponseEntity<ApiResponse<BookingResponse>> response = bookingController.createBooking(intentKey, request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(intentKey, request.getIntentKey());
        verify(bookingService).createBooking(request);
    }

    @Test
    @DisplayName("GET /api/bookings/{id}/customer returns Customer Detail with allowed actions")
    void testGetCustomerBookingDetail() {
        BookingCustomerDetailResponse customerDetail = BookingCustomerDetailResponse.builder()
                .bookingId(bookingId)
                .bookingCode("MC123456")
                .customerName("Nguyen Van A")
                .customerPaidAmount(new BigDecimal("90000"))
                .allowedActions(List.of("PAY", "CANCEL"))
                .build();

        when(bookingService.getCustomerBookingDetail(bookingId)).thenReturn(customerDetail);

        ResponseEntity<ApiResponse<BookingCustomerDetailResponse>> response = bookingController.getCustomerBookingDetail(bookingId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().getData().getAllowedActions().size());
        assertTrue(response.getBody().getData().getAllowedActions().contains("PAY"));
    }

    @Test
    @DisplayName("GET /api/stores/{storeId}/bookings/{bookingId} returns Staff Detail without financial internals")
    void testGetStoreBookingStaffDetail() {
        BookingStaffDetailResponse staffDetail = BookingStaffDetailResponse.builder()
                .bookingId(bookingId)
                .bookingCode("MC123456")
                .customerName("Nguyen Van A")
                .grossAmount(new BigDecimal("100000"))
                .allowedActions(List.of("CHECK_IN", "CANCEL"))
                .build();

        when(bookingService.getStaffBookingDetail(storeId, bookingId)).thenReturn(staffDetail);

        ResponseEntity<ApiResponse<BookingStaffDetailResponse>> response = bookingController.getStoreBookingStaffDetail(storeId, bookingId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(new BigDecimal("100000"), response.getBody().getData().getGrossAmount());
        assertTrue(response.getBody().getData().getAllowedActions().contains("CHECK_IN"));
    }
}
