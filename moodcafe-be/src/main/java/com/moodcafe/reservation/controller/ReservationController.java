package com.moodcafe.reservation.controller;

import com.moodcafe.auth.abstraction.service.CurrentUserService;
import com.moodcafe.auth.entity.User;
import com.moodcafe.finance.service.ReservationRefundService;
import com.moodcafe.payment.service.BookingPaymentCoordinator;
import com.moodcafe.reservation.abstraction.repository.ReservationRepository;
import com.moodcafe.reservation.dto.CreateReservationRequest;
import com.moodcafe.reservation.dto.ReservationResponse;
import com.moodcafe.reservation.entity.Reservation;
import com.moodcafe.reservation.service.ReservationService;
import com.moodcafe.shared.error.ErrorCode;
import com.moodcafe.shared.exceptions.AppException;
import com.moodcafe.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Reservation - Booking Lifecycle", description = "APIs tạo đơn đặt bàn, check-in, check-out, và hủy hoàn tiền")
@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;
    private final BookingPaymentCoordinator bookingPaymentCoordinator;
    private final ReservationRefundService refundService;
    private final ReservationRepository reservationRepository;
    private final CurrentUserService currentUserService;

    @Operation(summary = "1. Tạo đơn đặt bàn mới (áp dụng voucher và thanh toán phần còn lại)")
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ReservationResponse>> createReservation(
            @RequestBody CreateReservationRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        User user = currentUserService.getCurrentUser();
        if (user == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED, "Vui lòng đăng nhập");
        }
        ReservationResponse res = bookingPaymentCoordinator.createReservation(request, user.getUserId(), idempotencyKey);
        return ResponseEntity.ok(ApiResponse.success(res, "Tạo đơn đặt bàn thành công"));
    }

    @Operation(summary = "2. Check-in khi khách đến quán (Khách tự quét QR hoặc Thu ngân nhận bàn)")
    @PostMapping("/{reservationId}/check-in")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ReservationResponse>> checkIn(
            @PathVariable UUID reservationId,
            @RequestParam(required = false) String qrToken
    ) {
        User user = currentUserService.getCurrentUser();
        if (user == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED, "Vui lòng đăng nhập");
        }
        ReservationResponse res = reservationService.checkIn(reservationId, user.getUserId(), qrToken);
        return ResponseEntity.ok(ApiResponse.success(res, "Check-in thành công"));
    }

    @Operation(summary = "3. Check-out (Tự quét QR trả bàn hoặc Thu ngân hoàn tất)")
    @PostMapping("/{reservationId}/check-out")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ReservationResponse>> checkOut(
            @PathVariable UUID reservationId,
            @RequestParam(defaultValue = "true") boolean isSelfQr,
            @RequestParam(required = false) String qrToken
    ) {
        User user = currentUserService.getCurrentUser();
        if (user == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED, "Vui lòng đăng nhập");
        }
        ReservationResponse res = reservationService.checkOut(reservationId, user.getUserId(), isSelfQr, qrToken);
        return ResponseEntity.ok(ApiResponse.success(res, "Check-out hoàn tất"));
    }

    @Operation(summary = "4. Hủy đơn và hoàn tiền thành MoodPoints (1:1)")
    @PostMapping("/{reservationId}/cancel")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Reservation>> cancelReservation(
            @PathVariable UUID reservationId,
            @RequestParam(required = false) String reason
    ) {
        User user = currentUserService.getCurrentUser();
        if (user == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED, "Vui lòng đăng nhập");
        }
        Reservation res = refundService.cancelAndRefund(reservationId, user.getUserId(), reason);
        return ResponseEntity.ok(ApiResponse.success(res, "Hủy đơn và hoàn MoodPoints thành công"));
    }

    @Operation(summary = "5. Xem danh sách đơn đặt bàn của tôi")
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Page<Reservation>>> getMyReservations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size
    ) {
        User user = currentUserService.getCurrentUser();
        if (user == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED, "Vui lòng đăng nhập");
        }
        Pageable pageable = PageRequest.of(page, size);
        Page<Reservation> list = reservationRepository.findByUserIdOrderByBookingTimeDesc(user.getUserId(), pageable);
        return ResponseEntity.ok(ApiResponse.success(list, "Lấy danh sách đơn đặt bàn thành công"));
    }
}
