package com.moodcafe.reservation;

import com.moodcafe.loyalty.service.LoyaltyRewardService;
import com.moodcafe.reservation.abstraction.repository.ReservationRepository;
import com.moodcafe.reservation.entity.Reservation;
import com.moodcafe.reservation.service.ReservationQrVerificationService;
import com.moodcafe.reservation.service.ReservationService;
import com.moodcafe.shared.error.ErrorCode;
import com.moodcafe.shared.exceptions.AppException;
import com.moodcafe.store.abstraction.service.StoreStaffService;
import com.moodcafe.wallet.service.MerchantWalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationAuthorizationIT {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private StoreStaffService storeStaffService;

    @Mock
    private MerchantWalletService merchantWalletService;

    @Mock
    private LoyaltyRewardService loyaltyRewardService;

    @Mock
    private ReservationQrVerificationService qrVerificationService;

    @InjectMocks
    private ReservationService reservationService;

    private UUID storeId;
    private UUID customerId;
    private UUID strangerId;
    private UUID reservationId;
    private Reservation paidReservation;

    @BeforeEach
    void setUp() {
        storeId = UUID.randomUUID();
        customerId = UUID.randomUUID();
        strangerId = UUID.randomUUID();
        reservationId = UUID.randomUUID();

        Instant now = Instant.now();
        paidReservation = Reservation.builder()
                .reservationId(reservationId)
                .storeId(storeId)
                .userId(customerId)
                .bookingTime(now.plus(10, ChronoUnit.MINUTES))
                .graceDeadline(now.plus(25, ChronoUnit.MINUTES))
                .serviceClosingDeadline(now.plus(6, ChronoUnit.HOURS))
                .status("PAID_CONFIRMED")
                .grossAmount(new BigDecimal("100000.00"))
                .merchantNetAmount(new BigDecimal("85000.00"))
                .customerPaidAmount(new BigDecimal("100000.00"))
                .build();
    }

    @Test
    @DisplayName("1. Stranger (neither booking customer nor store staff) receives 403 on check-in")
    void strangerCannotCheckIn() {
        when(reservationRepository.findByIdWithLock(reservationId)).thenReturn(Optional.of(paidReservation));
        doThrow(new AppException(ErrorCode.FORBIDDEN_STORE_ACCESS))
                .when(storeStaffService).requireStoreAccess(eq(storeId), eq("OWNER"), eq("MANAGER"), eq("CASHIER"));

        AppException ex = assertThrows(AppException.class, () ->
                reservationService.checkIn(reservationId, strangerId, "STORE:" + storeId));
        assertEquals(ErrorCode.FORBIDDEN_STORE_ACCESS, ex.getErrorCode());
    }

    @Test
    @DisplayName("2. Early check-in protection: customer cannot check in earlier than 30 minutes before booking")
    void earlyCheckInBlocked() {
        // Booking is 2 hours in the future
        paidReservation.setBookingTime(Instant.now().plus(2, ChronoUnit.HOURS));
        paidReservation.setGraceDeadline(Instant.now().plus(2, ChronoUnit.HOURS).plus(15, ChronoUnit.MINUTES));
        when(reservationRepository.findByIdWithLock(reservationId)).thenReturn(Optional.of(paidReservation));

        AppException ex = assertThrows(AppException.class, () ->
                reservationService.checkIn(reservationId, customerId, "STORE:" + storeId));
        assertTrue(ex.getMessage().contains("Chưa đến giờ nhận bàn"));
    }

    @Test
    @DisplayName("3. Customer self check-in after grace deadline is blocked (must contact staff)")
    void customerCheckInAfterGraceDeadlineBlocked() {
        // Booking was 20 minutes ago (grace deadline passed 5 minutes ago)
        paidReservation.setBookingTime(Instant.now().minus(20, ChronoUnit.MINUTES));
        paidReservation.setGraceDeadline(Instant.now().minus(5, ChronoUnit.MINUTES));
        when(reservationRepository.findByIdWithLock(reservationId)).thenReturn(Optional.of(paidReservation));

        AppException ex = assertThrows(AppException.class, () ->
                reservationService.checkIn(reservationId, customerId, "STORE:" + storeId));
        assertTrue(ex.getMessage().contains("Đã quá thời gian nhận bàn"));
    }

    @Test
    @DisplayName("4. Store staff can accept overdue customer as CLAIMED_LATE and settle funds")
    void staffCanClaimLate() {
        paidReservation.setBookingTime(Instant.now().minus(20, ChronoUnit.MINUTES));
        paidReservation.setGraceDeadline(Instant.now().minus(5, ChronoUnit.MINUTES));
        when(reservationRepository.findByIdWithLock(reservationId)).thenReturn(Optional.of(paidReservation));
        when(reservationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UUID cashierId = UUID.randomUUID();
        // Staff of storeId succeeds requireStoreAccess
        doReturn(null).when(storeStaffService).requireStoreAccess(eq(storeId), eq("OWNER"), eq("MANAGER"), eq("CASHIER"));

        var resp = reservationService.checkIn(reservationId, cashierId, null);
        assertEquals("CLAIMED_LATE", resp.getStatus());

        verify(merchantWalletService).settleToAvailable(eq(storeId), eq(new BigDecimal("85000.00")), eq(reservationId.toString()), any());
        verify(loyaltyRewardService).awardSpendReward(any());
        verify(loyaltyRewardService, never()).awardOnTimeCheckIn(any());
    }

    @Test
    @DisplayName("5. Customer self checkout with duration >= 30 mins awards SELF_CHECK_OUT bonus")
    void customerSelfCheckOutAwardsBonus() {
        paidReservation.setStatus("CHECKED_IN");
        paidReservation.setCheckInTime(Instant.now().minus(40, ChronoUnit.MINUTES)); // Stayed 40 mins
        when(reservationRepository.findByIdWithLock(reservationId)).thenReturn(Optional.of(paidReservation));
        when(reservationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var resp = reservationService.checkOut(reservationId, customerId, true);
        assertEquals("COMPLETED", resp.getStatus());

        verify(loyaltyRewardService).awardSelfCheckOut(any(), anyLong());
    }

    @Test
    @DisplayName("6. Staff checkout completes order but does NOT award SELF_CHECK_OUT bonus")
    void staffCheckOutDoesNotAwardSelfBonus() {
        paidReservation.setStatus("CHECKED_IN");
        paidReservation.setCheckInTime(Instant.now().minus(40, ChronoUnit.MINUTES));
        when(reservationRepository.findByIdWithLock(reservationId)).thenReturn(Optional.of(paidReservation));
        when(reservationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UUID cashierId = UUID.randomUUID();
        doReturn(null).when(storeStaffService).requireStoreAccess(eq(storeId), eq("OWNER"), eq("MANAGER"), eq("CASHIER"));

        var resp = reservationService.checkOut(reservationId, cashierId, false);
        assertEquals("COMPLETED", resp.getStatus());

        verify(loyaltyRewardService, never()).awardSelfCheckOut(any(), anyLong());
    }
    @Test
    void customerCannotBypassQrBySettingSelfFlagFalseOrStayingUnderThirtyMinutes() {
        paidReservation.setStatus("CHECKED_IN");
        paidReservation.setCheckInTime(Instant.now().minusSeconds(60));
        when(reservationRepository.findByIdWithLock(reservationId)).thenReturn(Optional.of(paidReservation));
        doThrow(new AppException(ErrorCode.BAD_REQUEST)).when(qrVerificationService).validateStoreQrToken(storeId, null);
        assertThrows(AppException.class, () -> reservationService.checkOut(reservationId, customerId, false));
        assertEquals("CHECKED_IN", paidReservation.getStatus());
        verifyNoInteractions(merchantWalletService, loyaltyRewardService);
    }

}
