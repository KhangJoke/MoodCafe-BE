package com.moodcafe.booking.controller;

import com.moodcafe.booking.abstraction.service.BookingService;
import com.moodcafe.booking.dto.request.CancelBookingRequest;
import com.moodcafe.booking.dto.request.CreateBookingRequest;
import com.moodcafe.booking.dto.request.CreateWalkInHoldRequest;
import com.moodcafe.booking.dto.request.DeskCheckInRequest;
import com.moodcafe.booking.dto.request.UpdateTableNumberRequest;
import com.moodcafe.booking.dto.response.BookingDeskSummaryResponse;
import com.moodcafe.booking.dto.response.BookingResponse;
import com.moodcafe.booking.dto.response.SlotAvailabilityResponse;
import com.moodcafe.booking.dto.response.StoreDailyScheduleResponse;
import com.moodcafe.shared.response.ApiResponse;
import com.moodcafe.shared.response.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    // ==================== Customer Booking Flow ====================

    @GetMapping("/stores/{storeId}/schedule")
    public ResponseEntity<ApiResponse<StoreDailyScheduleResponse>> getStoreDailySchedule(
            @PathVariable UUID storeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        StoreDailyScheduleResponse response = bookingService.getStoreDailySchedule(storeId, date);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/stores/{storeId}/booking-availability")
    public ResponseEntity<ApiResponse<SlotAvailabilityResponse>> checkSlotAvailability(
            @PathVariable UUID storeId,
            @RequestParam UUID categoryId,
            @RequestParam Instant startTime) {
        SlotAvailabilityResponse response = bookingService.checkSlotAvailability(storeId, categoryId, startTime);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/bookings")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<BookingResponse>> createBooking(
            @Valid @RequestBody CreateBookingRequest request) {
        BookingResponse response = bookingService.createBooking(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Khởi tạo đơn đặt bàn thành công, vui lòng hoàn tất thanh toán"));
    }

    @GetMapping("/bookings/{bookingId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<BookingResponse>> getBookingDetail(
            @PathVariable UUID bookingId) {
        BookingResponse response = bookingService.getBookingDetail(bookingId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/bookings/code/{bookingCode}")
    public ResponseEntity<ApiResponse<BookingResponse>> getBookingByCode(
            @PathVariable String bookingCode) {
        BookingResponse response = bookingService.getBookingByCode(bookingCode);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/bookings/{bookingId}/cancel")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<BookingResponse>> cancelBooking(
            @PathVariable UUID bookingId,
            @Valid @RequestBody CancelBookingRequest request) {
        BookingResponse response = bookingService.cancelBooking(bookingId, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Hủy đơn đặt bàn thành công. Hệ thống đã xử lý hoàn điểm & giải phóng slot."));
    }

    @PostMapping("/bookings/{bookingId}/payment-link")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<BookingResponse>> getBookingPaymentLink(
            @PathVariable UUID bookingId) {
        BookingResponse response = bookingService.getBookingPaymentLink(bookingId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/bookings/my")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PageResponse<BookingResponse>>> getMyBookings(
            @RequestParam(required = false) String status,
            @PageableDefault(size = 15, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PageResponse<BookingResponse> page = bookingService.getMyBookings(status, pageable);
        return ResponseEntity.ok(ApiResponse.success(page));
    }

    @PostMapping("/bookings/{bookingId}/request-checkout")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<BookingResponse>> requestCheckout(
            @PathVariable UUID bookingId) {
        BookingResponse response = bookingService.requestCheckout(bookingId);
        return ResponseEntity.ok(ApiResponse.success(response, "Đã gửi thông báo báo trả bàn đến quầy thu ngân"));
    }

    @PostMapping("/bookings/{bookingId}/cancel-checkout-request")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<BookingResponse>> cancelCheckoutRequest(
            @PathVariable UUID bookingId) {
        BookingResponse response = bookingService.cancelCheckoutRequest(bookingId);
        return ResponseEntity.ok(ApiResponse.success(response, "Đã hủy yêu cầu trả bàn"));
    }

    // ==================== Merchant Desk Operations ====================

    @GetMapping("/stores/{storeId}/bookings")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PageResponse<BookingResponse>>> getStoreDeskBookings(
            @PathVariable UUID storeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @PageableDefault(size = 15, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PageResponse<BookingResponse> page = bookingService.getStoreDeskBookings(storeId, date, from, to, pageable);
        return ResponseEntity.ok(ApiResponse.success(page));
    }

    @GetMapping("/stores/{storeId}/bookings/summary")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<BookingDeskSummaryResponse>> getStoreDeskSummary(
            @PathVariable UUID storeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        BookingDeskSummaryResponse summary = bookingService.getStoreDeskSummary(storeId, date);
        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    @PostMapping("/stores/{storeId}/bookings/check-in")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<BookingResponse>> deskCheckIn(
            @PathVariable UUID storeId,
            @Valid @RequestBody DeskCheckInRequest request) {
        BookingResponse response = bookingService.deskCheckIn(storeId, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Xác nhận khách nhận bàn thành công"));
    }

    @PostMapping("/stores/{storeId}/bookings/{bookingId}/claim-late")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<BookingResponse>> deskClaimLate(
            @PathVariable UUID storeId,
            @PathVariable UUID bookingId) {
        BookingResponse response = bookingService.deskClaimLate(storeId, bookingId);
        return ResponseEntity.ok(ApiResponse.success(response, "Xác nhận khách đã nhận đồ uống trễ thành công"));
    }

    @PostMapping("/stores/{storeId}/bookings/{bookingId}/complete")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<BookingResponse>> deskComplete(
            @PathVariable UUID storeId,
            @PathVariable UUID bookingId) {
        BookingResponse response = bookingService.deskComplete(storeId, bookingId);
        return ResponseEntity.ok(ApiResponse.success(response, "Hoàn tất trả bàn và giải phóng slot thành công"));
    }

    @PostMapping("/stores/{storeId}/bookings/{bookingId}/undo-complete")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<BookingResponse>> undoComplete(
            @PathVariable UUID storeId,
            @PathVariable UUID bookingId) {
        BookingResponse response = bookingService.undoComplete(storeId, bookingId);
        return ResponseEntity.ok(ApiResponse.success(response, "Đã hoàn tác đơn về trạng thái đang ngồi"));
    }

    @PutMapping("/stores/{storeId}/bookings/{bookingId}/table-number")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<BookingResponse>> updatePhysicalTable(
            @PathVariable UUID storeId,
            @PathVariable UUID bookingId,
            @Valid @RequestBody UpdateTableNumberRequest request) {
        BookingResponse response = bookingService.updatePhysicalTable(storeId, bookingId, request.getPhysicalTableNumber());
        return ResponseEntity.ok(ApiResponse.success(response, "Cập nhật số bàn thành công"));
    }

    @PostMapping("/stores/{storeId}/bookings/walk-in-hold")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<BookingResponse>> createWalkInHold(
            @PathVariable UUID storeId,
            @Valid @RequestBody CreateWalkInHoldRequest request) {
        BookingResponse response = bookingService.createWalkInHold(
                storeId,
                request.getCategoryId(),
                request.getDurationMinutes(),
                request.getPhysicalTableNumber()
        );
        return ResponseEntity.ok(ApiResponse.success(response, "Đã giữ tạm 1 slot bàn cho khách vãng lai"));
    }

    @PostMapping("/stores/{storeId}/bookings/{bookingId}/walk-in-release")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<BookingResponse>> releaseWalkInHold(
            @PathVariable UUID storeId,
            @PathVariable UUID bookingId) {
        BookingResponse response = bookingService.releaseWalkInHold(storeId, bookingId);
        return ResponseEntity.ok(ApiResponse.success(response, "Đã giải phóng bàn khách vãng lai"));
    }
}
