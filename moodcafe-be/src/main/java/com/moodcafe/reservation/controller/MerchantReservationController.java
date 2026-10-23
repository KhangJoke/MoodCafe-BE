package com.moodcafe.reservation.controller;

import com.moodcafe.auth.abstraction.service.CurrentUserService;
import com.moodcafe.auth.entity.User;
import com.moodcafe.booking.abstraction.repository.BookingRepository;
import com.moodcafe.booking.abstraction.service.BookingService;
import com.moodcafe.booking.dto.request.DeskCheckInRequest;
import com.moodcafe.booking.dto.response.BookingResponse;
import com.moodcafe.booking.entity.Booking;
import com.moodcafe.booking.entity.enums.BookingStatus;
import com.moodcafe.reservation.abstraction.repository.ReservationRepository;
import com.moodcafe.reservation.dto.ReservationResponse;
import com.moodcafe.reservation.dto.ReservationStaffResponse;
import com.moodcafe.reservation.entity.Reservation;
import com.moodcafe.reservation.service.ReservationService;
import com.moodcafe.shared.error.ErrorCode;
import com.moodcafe.shared.exceptions.AppException;
import com.moodcafe.shared.response.ApiResponse;
import com.moodcafe.store.abstraction.service.StoreStaffService;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Merchant Reservations", description = "APIs dành cho Nhân viên/Chủ quán xem danh sách và phục vụ đơn bàn")
@RestController
@RequestMapping("/api/merchant/stores/{storeId}/reservations")
@RequiredArgsConstructor
public class MerchantReservationController {

    private final ReservationRepository reservationRepository;
    private final ReservationService reservationService;
    private final BookingRepository bookingRepository;
    private final BookingService bookingService;
    private final StoreStaffService storeStaffService;
    private final CurrentUserService currentUserService;

    @Operation(summary = "1. Xem danh sách đơn đặt bàn của cơ sở (phục vụ theo hạn mức Gross)")
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Page<ReservationStaffResponse>>> getStoreReservations(
            @PathVariable UUID storeId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size
    ) {
        User currentUser = currentUserService.getCurrentUser();
        if (currentUser == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED, "Vui lòng đăng nhập");
        }

        // Verify active OWNER, MANAGER, or CASHIER on this store
        storeStaffService.requireStoreAccess(storeId, "OWNER", "MANAGER", "CASHIER");

        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 50));

        // Prioritize canonical bookings table
        Page<Booking> bookingPage;
        if (status != null && !status.trim().isEmpty()) {
            try {
                BookingStatus bStatus = BookingStatus.valueOf(status.trim().toUpperCase());
                bookingPage = bookingRepository.findAllByStoreStoreIdAndStatusOrderByCreatedAtDesc(storeId, bStatus, pageable);
            } catch (IllegalArgumentException e) {
                bookingPage = Page.empty(pageable);
            }
        } else {
            bookingPage = bookingRepository.findAllByStoreStoreIdOrderByCreatedAtDesc(storeId, pageable);
        }

        if (bookingPage != null && bookingPage.hasContent()) {
            Page<ReservationStaffResponse> staffPage = bookingPage.map(ReservationStaffResponse::fromBooking);
            return ResponseEntity.ok(ApiResponse.success(staffPage, "Lấy danh sách đơn đặt bàn thành công"));
        }

        // Fallback to legacy reservation table
        Page<Reservation> reservationPage;
        if (status != null && !status.trim().isEmpty()) {
            reservationPage = reservationRepository.findByStoreIdAndStatusOrderByBookingTimeDesc(storeId, status.trim().toUpperCase(), pageable);
        } else {
            reservationPage = reservationRepository.findByStoreIdOrderByBookingTimeDesc(storeId, pageable);
        }

        Page<ReservationStaffResponse> staffPage = reservationPage.map(ReservationStaffResponse::fromEntity);
        return ResponseEntity.ok(ApiResponse.success(staffPage, "Lấy danh sách đơn đặt bàn thành công"));
    }

    @Operation(summary = "2. Nhân viên tiếp nhận bàn / giao nước cho khách")
    @PostMapping("/{reservationId}/check-in")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ReservationResponse>> staffCheckIn(
            @PathVariable UUID storeId,
            @PathVariable UUID reservationId
    ) {
        User currentUser = currentUserService.getCurrentUser();
        if (currentUser == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED, "Vui lòng đăng nhập");
        }

        storeStaffService.requireStoreAccess(storeId, "OWNER", "MANAGER", "CASHIER");

        // Handle canonical booking
        if (bookingRepository.existsById(reservationId)) {
            Booking booking = bookingRepository.findById(reservationId).orElseThrow();
            if (!storeId.equals(booking.getStore().getStoreId())) {
                throw new AppException(ErrorCode.FORBIDDEN, "Đơn đặt bàn không thuộc quyền quản lý của cơ sở này");
            }
            BookingResponse bRes = bookingService.deskCheckIn(storeId, new DeskCheckInRequest(booking.getBookingCode()));
            ReservationResponse rRes = ReservationResponse.builder()
                    .reservationId(bRes.getBookingId())
                    .storeId(bRes.getStoreId())
                    .status(bRes.getStatus() != null ? bRes.getStatus().name() : "CHECKED_IN")
                    .grossAmount(bRes.getGrossAmount())
                    .build();
            return ResponseEntity.ok(ApiResponse.success(rRes, "Tiếp nhận đơn thành công"));
        }

        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Không tìm thấy đơn đặt bàn"));
        if (!storeId.equals(reservation.getStoreId())) {
            throw new AppException(ErrorCode.FORBIDDEN, "Đơn đặt bàn không thuộc quyền quản lý của cơ sở này");
        }

        ReservationResponse res = reservationService.checkIn(reservationId, currentUser.getUserId(), null);
        return ResponseEntity.ok(ApiResponse.success(res, "Tiếp nhận đơn thành công"));
    }

    @Operation(summary = "3. Nhân viên hoàn tất đơn trả bàn (Check-out)")
    @PostMapping("/{reservationId}/check-out")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ReservationResponse>> staffCheckOut(
            @PathVariable UUID storeId,
            @PathVariable UUID reservationId
    ) {
        User currentUser = currentUserService.getCurrentUser();
        if (currentUser == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED, "Vui lòng đăng nhập");
        }

        storeStaffService.requireStoreAccess(storeId, "OWNER", "MANAGER", "CASHIER");

        // Handle canonical booking
        if (bookingRepository.existsById(reservationId)) {
            Booking booking = bookingRepository.findById(reservationId).orElseThrow();
            if (!storeId.equals(booking.getStore().getStoreId())) {
                throw new AppException(ErrorCode.FORBIDDEN, "Đơn đặt bàn không thuộc quyền quản lý của cơ sở này");
            }
            BookingResponse bRes = bookingService.deskComplete(storeId, reservationId);
            ReservationResponse rRes = ReservationResponse.builder()
                    .reservationId(bRes.getBookingId())
                    .storeId(bRes.getStoreId())
                    .status(bRes.getStatus() != null ? bRes.getStatus().name() : "COMPLETED")
                    .grossAmount(bRes.getGrossAmount())
                    .build();
            return ResponseEntity.ok(ApiResponse.success(rRes, "Hoàn tất trả bàn thành công"));
        }

        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Không tìm thấy đơn đặt bàn"));
        if (!storeId.equals(reservation.getStoreId())) {
            throw new AppException(ErrorCode.FORBIDDEN, "Đơn đặt bàn không thuộc quyền quản lý của cơ sở này");
        }

        ReservationResponse res = reservationService.checkOut(reservationId, currentUser.getUserId(), false, null);
        return ResponseEntity.ok(ApiResponse.success(res, "Hoàn tất trả bàn thành công"));
    }

    @Operation(summary = "4. Nhân viên giao nước trễ trước giờ đóng cửa (Claim late)")
    @PostMapping("/{reservationId}/claim-late")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ReservationResponse>> staffClaimLate(
            @PathVariable UUID storeId,
            @PathVariable UUID reservationId
    ) {
        User currentUser = currentUserService.getCurrentUser();
        if (currentUser == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED, "Vui lòng đăng nhập");
        }

        storeStaffService.requireStoreAccess(storeId, "OWNER", "MANAGER", "CASHIER");

        BookingResponse bRes = bookingService.deskClaimLate(storeId, reservationId);
        ReservationResponse rRes = ReservationResponse.builder()
                .reservationId(bRes.getBookingId())
                .storeId(bRes.getStoreId())
                .status(bRes.getStatus() != null ? bRes.getStatus().name() : "CLAIMED_LATE")
                .grossAmount(bRes.getGrossAmount())
                .build();
        return ResponseEntity.ok(ApiResponse.success(rRes, "Xác nhận khách đã nhận đồ uống trễ thành công"));
    }
}
