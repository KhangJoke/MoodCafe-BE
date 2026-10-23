package com.moodcafe.reservation;

import com.moodcafe.auth.abstraction.service.CurrentUserService;
import com.moodcafe.auth.entity.User;
import com.moodcafe.booking.abstraction.repository.BookingRepository;
import com.moodcafe.booking.abstraction.service.BookingService;
import com.moodcafe.booking.entity.Booking;
import com.moodcafe.booking.entity.enums.BookingStatus;
import com.moodcafe.reservation.abstraction.repository.ReservationRepository;
import com.moodcafe.reservation.controller.MerchantReservationController;
import com.moodcafe.reservation.dto.ReservationResponse;
import com.moodcafe.reservation.dto.ReservationStaffResponse;
import com.moodcafe.reservation.entity.Reservation;
import com.moodcafe.reservation.service.ReservationService;
import com.moodcafe.shared.error.ErrorCode;
import com.moodcafe.shared.exceptions.AppException;
import com.moodcafe.shared.response.ApiResponse;
import com.moodcafe.store.abstraction.service.StoreStaffService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MerchantReservationControllerIT {

    @Mock
    private ReservationRepository reservationRepository;
    @Mock
    private ReservationService reservationService;
    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private BookingService bookingService;
    @Mock
    private StoreStaffService storeStaffService;
    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private MerchantReservationController controller;

    private UUID storeId;
    private UUID staffUserId;
    private User staffUser;

    @BeforeEach
    void setUp() {
        storeId = UUID.randomUUID();
        staffUserId = UUID.randomUUID();
        staffUser = User.builder()
                .userId(staffUserId)
                .email("cashier@moodcafe.com")
                .build();
    }

    @Test
    @DisplayName("Staff hợp lệ xem được danh sách đơn đặt bàn có Gross Amount, không lộ merchantNetAmount/commission")
    void testGetStoreReservations_Success() {
        when(currentUserService.getCurrentUser()).thenReturn(staffUser);
        when(bookingRepository.findAllByStoreStoreIdOrderByCreatedAtDesc(eq(storeId), any(Pageable.class)))
                .thenReturn(Page.empty());

        Reservation r = new Reservation();
        r.setReservationId(UUID.randomUUID());
        r.setStoreId(storeId);
        r.setUserId(UUID.randomUUID());
        r.setGrossAmount(new BigDecimal("100000"));
        r.setMerchantNetAmount(new BigDecimal("85000"));
        r.setCommissionAmount(new BigDecimal("15000"));
        r.setStatus("PAID_CONFIRMED");
        r.setBookingTime(Instant.now());

        Page<Reservation> page = new PageImpl<>(List.of(r));
        when(reservationRepository.findByStoreIdOrderByBookingTimeDesc(eq(storeId), any(Pageable.class)))
                .thenReturn(page);

        ResponseEntity<ApiResponse<Page<ReservationStaffResponse>>> response =
                controller.getStoreReservations(storeId, null, 0, 10);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        Page<ReservationStaffResponse> resultPage = response.getBody().getData();
        assertThat(resultPage.getContent()).hasSize(1);
        ReservationStaffResponse staffDto = resultPage.getContent().get(0);

        // Verification of contract: Gross is visible for drink dispensation
        assertThat(staffDto.getGrossAmount()).isEqualByComparingTo("100000");
        assertThat(staffDto.getStatus()).isEqualTo("PAID_CONFIRMED");

        verify(storeStaffService).requireStoreAccess(storeId, "OWNER", "MANAGER", "CASHIER");
    }

    @Test
    @DisplayName("Staff xem danh sách đơn trực tiếp từ canonical Booking repository")
    void testGetStoreReservations_FromCanonicalBooking() {
        when(currentUserService.getCurrentUser()).thenReturn(staffUser);

        Booking b = Booking.builder()
                .bookingId(UUID.randomUUID())
                .bookingCode("MC123456")
                .customerName("Nguyen Van A")
                .customerPhone("0901234567")
                .guestCount(2)
                .grossAmount(new BigDecimal("120000"))
                .merchantNetAmount(new BigDecimal("102000"))
                .commissionAmount(new BigDecimal("18000"))
                .status(BookingStatus.PAID_CONFIRMED)
                .startTime(Instant.now())
                .endTime(Instant.now().plusSeconds(7200))
                .build();

        Page<Booking> page = new PageImpl<>(List.of(b));
        when(bookingRepository.findAllByStoreStoreIdOrderByCreatedAtDesc(eq(storeId), any(Pageable.class)))
                .thenReturn(page);

        ResponseEntity<ApiResponse<Page<ReservationStaffResponse>>> response =
                controller.getStoreReservations(storeId, null, 0, 10);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        var staffDto = response.getBody().getData().getContent().get(0);
        assertThat(staffDto.getGrossAmount()).isEqualByComparingTo("120000");
        assertThat(staffDto.getStatus()).isEqualTo("PAID_CONFIRMED");
    }

    @Test
    @DisplayName("Từ chối truy cập 403 nếu tài khoản không có quyền trên cơ sở")
    void testGetStoreReservations_ForbiddenIfNotStaff() {
        when(currentUserService.getCurrentUser()).thenReturn(staffUser);
        when(storeStaffService.requireStoreAccess(storeId, "OWNER", "MANAGER", "CASHIER"))
                .thenThrow(new AppException(ErrorCode.FORBIDDEN, "Không có quyền truy cập cơ sở này"));

        assertThatThrownBy(() -> controller.getStoreReservations(storeId, null, 0, 10))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("Không có quyền truy cập cơ sở này");
    }

    @Test
    @DisplayName("Nhân viên tiếp nhận check-in hoặc giao nước trễ thành công")
    void testStaffCheckIn_Success() {
        when(currentUserService.getCurrentUser()).thenReturn(staffUser);

        UUID reservationId = UUID.randomUUID();
        Reservation r = new Reservation();
        r.setReservationId(reservationId);
        r.setStoreId(storeId);
        r.setStatus("PAID_CONFIRMED");

        when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(r));
        when(reservationService.checkIn(reservationId, staffUserId, null))
                .thenReturn(ReservationResponse.builder()
                        .reservationId(reservationId)
                        .status("CHECKED_IN")
                        .build());

        ResponseEntity<ApiResponse<ReservationResponse>> res = controller.staffCheckIn(storeId, reservationId);
        assertThat(res.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(res.getBody().getData().getStatus()).isEqualTo("CHECKED_IN");
    }

    @Test
    @DisplayName("Từ chối nhân viên check-out nếu đơn đặt bàn thuộc cơ sở khác")
    void testStaffCheckOut_WrongStoreForbidden() {
        when(currentUserService.getCurrentUser()).thenReturn(staffUser);

        UUID reservationId = UUID.randomUUID();
        UUID otherStoreId = UUID.randomUUID();
        Reservation r = new Reservation();
        r.setReservationId(reservationId);
        r.setStoreId(otherStoreId);

        when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(r));

        assertThatThrownBy(() -> controller.staffCheckOut(storeId, reservationId))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("không thuộc quyền quản lý");
    }
}
