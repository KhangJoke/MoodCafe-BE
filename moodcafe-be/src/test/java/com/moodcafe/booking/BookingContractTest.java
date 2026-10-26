package com.moodcafe.booking;

import com.moodcafe.auth.abstraction.service.CurrentUserService;
import com.moodcafe.booking.abstraction.service.BookingService;
import com.moodcafe.booking.controller.BookingController;
import com.moodcafe.booking.dto.request.CreateBookingRequest;
import com.moodcafe.booking.dto.response.BookingResponse;
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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingContractTest {

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
    @DisplayName("POST /api/bookings returns 201 with BookingResponse")
    void testCreateBookingEndpoint() {
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

        ResponseEntity<ApiResponse<BookingResponse>> response = bookingController.createBooking(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("MC123456", response.getBody().getData().getBookingCode());
        verify(bookingService).createBooking(request);
    }

    @Test
    @DisplayName("POST /api/bookings/{id}/request-checkout initiates two-step checkout")
    void testRequestCheckoutEndpoint() {
        BookingResponse expectedBooking = BookingResponse.builder()
                .bookingId(bookingId)
                .bookingCode("MC123456")
                .status(BookingStatus.CHECK_OUT_PENDING)
                .build();

        when(bookingService.requestCheckout(bookingId)).thenReturn(expectedBooking);

        ResponseEntity<ApiResponse<BookingResponse>> response = bookingController.requestCheckout(bookingId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(BookingStatus.CHECK_OUT_PENDING, response.getBody().getData().getStatus());
        verify(bookingService).requestCheckout(bookingId);
    }

    @Test
    @DisplayName("POST /api/bookings/{id}/cancel-checkout-request cancels checkout request")
    void testCancelCheckoutRequestEndpoint() {
        BookingResponse expectedBooking = BookingResponse.builder()
                .bookingId(bookingId)
                .bookingCode("MC123456")
                .status(BookingStatus.CHECKED_IN)
                .build();

        when(bookingService.cancelCheckoutRequest(bookingId)).thenReturn(expectedBooking);

        ResponseEntity<ApiResponse<BookingResponse>> response = bookingController.cancelCheckoutRequest(bookingId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(BookingStatus.CHECKED_IN, response.getBody().getData().getStatus());
        verify(bookingService).cancelCheckoutRequest(bookingId);
    }

    @Test
    @DisplayName("POST /api/stores/{storeId}/bookings/{bookingId}/undo-complete reverts to checked-in")
    void testUndoCompleteEndpoint() {
        BookingResponse expectedBooking = BookingResponse.builder()
                .bookingId(bookingId)
                .bookingCode("MC123456")
                .status(BookingStatus.CHECKED_IN)
                .build();

        when(bookingService.undoComplete(storeId, bookingId)).thenReturn(expectedBooking);

        ResponseEntity<ApiResponse<BookingResponse>> response = bookingController.undoComplete(storeId, bookingId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(BookingStatus.CHECKED_IN, response.getBody().getData().getStatus());
        verify(bookingService).undoComplete(storeId, bookingId);
    }
}
