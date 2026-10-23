package com.moodcafe.reservation;

import com.moodcafe.finance.dto.ReservationFinancialSnapshot;
import com.moodcafe.finance.dto.ReservationPricingInput;
import com.moodcafe.finance.service.ReservationPricingService;
import com.moodcafe.payment.service.OrderCodeGeneratorService;
import com.moodcafe.payment.service.PaymentAttemptService;
import com.moodcafe.reservation.abstraction.repository.ReservationRepository;
import com.moodcafe.reservation.abstraction.repository.TableCategoryRepository;
import com.moodcafe.reservation.dto.CreateReservationRequest;
import com.moodcafe.reservation.entity.Reservation;
import com.moodcafe.reservation.entity.TableCategory;
import com.moodcafe.reservation.service.ReservationService;
import com.moodcafe.reservation.service.ServiceDayDeadlineResolver;
import com.moodcafe.shared.error.ErrorCode;
import com.moodcafe.shared.exceptions.AppException;
import com.moodcafe.store.abstraction.repository.StoreRepository;
import com.moodcafe.store.entity.Store;
import com.moodcafe.voucher.abstraction.repository.CustomerVoucherRepository;
import com.moodcafe.voucher.abstraction.repository.VoucherPointAllocationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationCategoryValidationTest {

    @Mock
    private ReservationRepository reservationRepository;
    @Mock
    private TableCategoryRepository tableCategoryRepository;
    @Mock
    private StoreRepository storeRepository;
    @Mock
    private ServiceDayDeadlineResolver serviceDayDeadlineResolver;
    @Mock
    private OrderCodeGeneratorService orderCodeGeneratorService;
    @Mock
    private PaymentAttemptService paymentAttemptService;
    @Mock
    private ReservationPricingService pricingService;
    @Mock
    private CustomerVoucherRepository customerVoucherRepository;
    @Mock
    private VoucherPointAllocationRepository allocationRepository;

    private ReservationService reservationService;

    private final UUID storeId = UUID.randomUUID();
    private final UUID categoryId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        reservationService = new ReservationService(
                reservationRepository,
                tableCategoryRepository,
                storeRepository,
                serviceDayDeadlineResolver,
                orderCodeGeneratorService,
                paymentAttemptService,
                pricingService,
                customerVoucherRepository,
                allocationRepository,
                null, // voucherLifecycleService
                null, // merchantWalletService
                null, // loyaltyRewardService
                null, // storeStaffService
                null  // qrVerificationService
        );
    }

    @Test
    @DisplayName("R11: Should reject booking when table category is soft-deleted")
    void shouldRejectBookingWhenCategoryIsSoftDeleted() {
        CreateReservationRequest request = new CreateReservationRequest();
        request.setStoreId(storeId);
        request.setCategoryId(categoryId);
        request.setBookingTime(Instant.now().plusSeconds(3600));

        TableCategory deletedCategory = TableCategory.builder()
                .categoryId(categoryId)
                .storeId(storeId)
                .name("Khu vực sân thượng")
                .isActive(true)
                .isDeleted(true)
                .minSpendAmount(BigDecimal.valueOf(100000))
                .build();

        when(tableCategoryRepository.findById(categoryId)).thenReturn(Optional.of(deletedCategory));

        AppException ex = assertThrows(AppException.class, () ->
                reservationService.prepareReservation(request, userId, "intent-del-1")
        );

        assertEquals(ErrorCode.BAD_REQUEST, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("đã bị xóa hoặc không còn hoạt động"));
        verify(reservationRepository, never()).save(any());
    }

    @Test
    @DisplayName("R11: Should reject booking when table category is inactive")
    void shouldRejectBookingWhenCategoryIsInactive() {
        CreateReservationRequest request = new CreateReservationRequest();
        request.setStoreId(storeId);
        request.setCategoryId(categoryId);
        request.setBookingTime(Instant.now().plusSeconds(3600));

        TableCategory inactiveCategory = TableCategory.builder()
                .categoryId(categoryId)
                .storeId(storeId)
                .name("Khu vực sân thượng")
                .isActive(false)
                .isDeleted(false)
                .minSpendAmount(BigDecimal.valueOf(100000))
                .build();

        when(tableCategoryRepository.findById(categoryId)).thenReturn(Optional.of(inactiveCategory));

        AppException ex = assertThrows(AppException.class, () ->
                reservationService.prepareReservation(request, userId, "intent-inact-1")
        );

        assertEquals(ErrorCode.BAD_REQUEST, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("đã bị xóa hoặc không còn hoạt động"));
        verify(reservationRepository, never()).save(any());
    }

    @Test
    @DisplayName("R11: Should reject booking when category belongs to another store")
    void shouldRejectBookingWhenCategoryBelongsToAnotherStore() {
        CreateReservationRequest request = new CreateReservationRequest();
        request.setStoreId(storeId);
        request.setCategoryId(categoryId);
        request.setBookingTime(Instant.now().plusSeconds(3600));

        TableCategory otherStoreCategory = TableCategory.builder()
                .categoryId(categoryId)
                .storeId(UUID.randomUUID())
                .name("Khu vực khác")
                .isActive(true)
                .isDeleted(false)
                .minSpendAmount(BigDecimal.valueOf(100000))
                .build();

        when(tableCategoryRepository.findById(categoryId)).thenReturn(Optional.of(otherStoreCategory));

        AppException ex = assertThrows(AppException.class, () ->
                reservationService.prepareReservation(request, userId, "intent-store-1")
        );

        assertEquals(ErrorCode.BAD_REQUEST, ex.getErrorCode());
        verify(reservationRepository, never()).save(any());
    }

    @Test
    @DisplayName("R10 & R11: Should succeed and resolve closing deadline when category is active and not deleted")
    void shouldSucceedWhenCategoryIsValid() {
        CreateReservationRequest request = new CreateReservationRequest();
        request.setStoreId(storeId);
        request.setCategoryId(categoryId);
        request.setBookingTime(Instant.now().plusSeconds(3600));

        TableCategory validCategory = TableCategory.builder()
                .categoryId(categoryId)
                .storeId(storeId)
                .name("Khu vực trong nhà")
                .isActive(true)
                .isDeleted(false)
                .minSpendAmount(BigDecimal.valueOf(100000))
                .build();

        Store store = Store.builder()
                .storeId(storeId)
                .openingTime(LocalTime.of(8, 0))
                .closingTime(LocalTime.of(22, 0))
                .build();

        ReservationFinancialSnapshot snapshot = ReservationFinancialSnapshot.builder()
                .grossAmount(BigDecimal.valueOf(100000))
                .voucherDiscount(BigDecimal.ZERO)
                .customerPaidAmount(BigDecimal.valueOf(100000))
                .commissionAmount(BigDecimal.valueOf(10000))
                .merchantNetAmount(BigDecimal.valueOf(90000))
                .commissionRate(BigDecimal.valueOf(0.10))
                .build();

        when(tableCategoryRepository.findById(categoryId)).thenReturn(Optional.of(validCategory));
        when(pricingService.quote(any(ReservationPricingInput.class))).thenReturn(snapshot);
        when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
        Instant closingDeadline = Instant.now().plusSeconds(7200);
        when(serviceDayDeadlineResolver.resolve(eq(request.getBookingTime()), eq(LocalTime.of(8, 0)), eq(LocalTime.of(22, 0)), any()))
                .thenReturn(closingDeadline);
        when(orderCodeGeneratorService.nextOrderCode()).thenReturn(998877L);
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(inv -> {
            Reservation r = inv.getArgument(0);
            if (r.getReservationId() == null) {
                r.setReservationId(UUID.randomUUID());
            }
            return r;
        });

        Reservation res = reservationService.prepareReservation(request, userId, "intent-valid-1");

        assertNotNull(res);
        assertEquals(storeId, res.getStoreId());
        assertEquals(categoryId, res.getCategoryId());
        assertEquals(closingDeadline, res.getServiceClosingDeadline());
        assertEquals(998877L, res.getPayosOrderCode());
        verify(paymentAttemptService).createAttempt(
                eq("BOOKING"), any(), eq(998877L), eq(BigDecimal.valueOf(100000)), eq("VND"), eq("intent-valid-1"),
                eq(userId), anyString()
        );
    }
}
