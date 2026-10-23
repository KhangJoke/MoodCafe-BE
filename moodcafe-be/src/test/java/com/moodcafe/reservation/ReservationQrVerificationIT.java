package com.moodcafe.reservation;

import com.moodcafe.loyalty.service.LoyaltyRewardService;
import com.moodcafe.reservation.abstraction.repository.ReservationRepository;
import com.moodcafe.reservation.dto.ReservationResponse;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservationQrVerificationIT {

    @Mock
    private ReservationRepository reservationRepository;
    @Mock
    private MerchantWalletService merchantWalletService;
    @Mock
    private LoyaltyRewardService loyaltyRewardService;
    @Mock
    private StoreStaffService storeStaffService;

    private ReservationQrVerificationService qrVerificationService;
    private ReservationService reservationService;

    private UUID storeId;
    private UUID customerId;
    private UUID staffId;

    @BeforeEach
    void setUp() {
        storeId = UUID.randomUUID();
        customerId = UUID.randomUUID();
        staffId = UUID.randomUUID();

        qrVerificationService = new ReservationQrVerificationService("test-secret-key-1234567890");

        reservationService = new ReservationService(
                reservationRepository,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                merchantWalletService,
                loyaltyRewardService,
                storeStaffService,
                qrVerificationService
        );
    }

    @Test
    @DisplayName("Valid server-generated store QR token succeeds check-in and awards 1000 points")
    void validStoreQrTokenSucceedsCheckIn() {
        UUID resId = UUID.randomUUID();
        Instant now = Instant.now();

        Reservation reservation = Reservation.builder()
                .reservationId(resId)
                .storeId(storeId)
                .userId(customerId)
                .status("PAID_CONFIRMED")
                .bookingTime(now.minus(5, ChronoUnit.MINUTES))
                .graceDeadline(now.plus(10, ChronoUnit.MINUTES))
                .serviceClosingDeadline(now.plus(4, ChronoUnit.HOURS))
                .grossAmount(new BigDecimal("100000"))
                .merchantNetAmount(new BigDecimal("90000"))
                .build();

        when(reservationRepository.findByIdWithLock(resId)).thenReturn(Optional.of(reservation));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(i -> i.getArgument(0));

        String validQrToken = qrVerificationService.generateDailyStoreQrToken(storeId);

        ReservationResponse response = reservationService.checkIn(resId, customerId, validQrToken);

        assertThat(response.getStatus()).isEqualTo("CHECKED_IN");
        verify(loyaltyRewardService).awardOnTimeCheckIn(reservation);
    }

    @Test
    @DisplayName("Forged QR token without valid HMAC fails check-in with 400 Bad Request")
    void forgedQrTokenFailsCheckIn() {
        UUID resId = UUID.randomUUID();
        Instant now = Instant.now();

        Reservation reservation = Reservation.builder()
                .reservationId(resId)
                .storeId(storeId)
                .userId(customerId)
                .status("PAID_CONFIRMED")
                .bookingTime(now.minus(5, ChronoUnit.MINUTES))
                .graceDeadline(now.plus(10, ChronoUnit.MINUTES))
                .serviceClosingDeadline(now.plus(4, ChronoUnit.HOURS))
                .build();

        when(reservationRepository.findByIdWithLock(resId)).thenReturn(Optional.of(reservation));

        String forgedToken = "STORE_QR:" + storeId + ":20261021:fake_forged_hmac_signature";

        assertThatThrownBy(() -> reservationService.checkIn(resId, customerId, forgedToken))
                .isInstanceOf(AppException.class)
                .matches(e -> ((AppException) e).getErrorCode() == ErrorCode.BAD_REQUEST);
    }

    @Test
    @DisplayName("Overdue customer check-in blocked, but store staff accepts as CLAIMED_LATE without QR")
    void overdueCheckInBehavior() {
        UUID resId = UUID.randomUUID();
        Instant now = Instant.now();

        // 20 minutes past bookingTime -> past 15-min graceDeadline
        Reservation reservation = Reservation.builder()
                .reservationId(resId)
                .storeId(storeId)
                .userId(customerId)
                .status("PAID_CONFIRMED")
                .bookingTime(now.minus(20, ChronoUnit.MINUTES))
                .graceDeadline(now.minus(5, ChronoUnit.MINUTES))
                .serviceClosingDeadline(now.plus(4, ChronoUnit.HOURS))
                .grossAmount(new BigDecimal("100000"))
                .merchantNetAmount(new BigDecimal("90000"))
                .build();

        when(reservationRepository.findByIdWithLock(resId)).thenReturn(Optional.of(reservation));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(i -> i.getArgument(0));

        // Customer self check-in overdue -> rejected
        assertThatThrownBy(() -> reservationService.checkIn(resId, customerId, "any_token"))
                .isInstanceOf(AppException.class)
                .matches(e -> ((AppException) e).getErrorCode() == ErrorCode.BAD_REQUEST);

        // Store staff check-in overdue -> accepted as CLAIMED_LATE
        ReservationResponse response = reservationService.checkIn(resId, staffId, null);
        assertThat(response.getStatus()).isEqualTo("CLAIMED_LATE");
        verify(storeStaffService).requireStoreAccess(storeId, "OWNER", "MANAGER", "CASHIER");
        verify(merchantWalletService).settleToAvailable(eq(storeId), eq(new BigDecimal("90000")), eq(resId.toString()), any());
    }
    @Test
    void publicDefaultQrSecretMustFailClosed() {
        var insecure = new ReservationQrVerificationService("moodcafe-super-secret-hmac-key-for-physical-store-qr-validation-2026");
        String forgedWithPublishedSecret = qrVerificationService.generateDailyStoreQrToken(storeId);
        assertThatThrownBy(() -> insecure.validateStoreQrToken(storeId, forgedWithPublishedSecret))
                .isInstanceOf(AppException.class).matches(e -> ((AppException) e).getErrorCode() == ErrorCode.FORBIDDEN_ACTION);
    }

    @Test
    void missingQrSecretMustFailClosed() {
        var unconfigured = new ReservationQrVerificationService("");
        assertThatThrownBy(() -> unconfigured.validateStoreQrToken(storeId, "anything"))
                .isInstanceOf(AppException.class).matches(e -> ((AppException) e).getErrorCode() == ErrorCode.FORBIDDEN_ACTION);
    }

}
