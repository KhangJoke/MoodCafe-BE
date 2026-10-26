package com.moodcafe.booking;

import com.moodcafe.auth.abstraction.service.CurrentUserService;
import com.moodcafe.auth.entity.User;
import com.moodcafe.booking.abstraction.repository.BookingRepository;
import com.moodcafe.booking.abstraction.repository.StoreBookingConfigRepository;
import com.moodcafe.booking.abstraction.repository.TableCategoryRepository;
import com.moodcafe.booking.dto.request.CancelBookingRequest;
import com.moodcafe.booking.dto.request.DeskCheckInRequest;
import com.moodcafe.booking.dto.response.BookingResponse;
import com.moodcafe.booking.entity.Booking;
import com.moodcafe.booking.entity.StoreBookingConfig;
import com.moodcafe.booking.entity.TableCategory;
import com.moodcafe.booking.entity.enums.BookingStatus;
import com.moodcafe.booking.mapper.BookingMapper;
import com.moodcafe.booking.service.BookingServiceImpl;
import com.moodcafe.finance.service.BookingCommissionService;
import com.moodcafe.finance.service.ReservationPricingService;
import com.moodcafe.loyalty.service.LoyaltyRewardService;
import com.moodcafe.notification.abstraction.service.NotificationDispatcherService;
import com.moodcafe.payment.abstraction.service.PaymentService;
import com.moodcafe.shared.exceptions.AppException;
import com.moodcafe.store.abstraction.repository.StoreImageRepository;
import com.moodcafe.store.abstraction.repository.StoreRepository;
import com.moodcafe.store.abstraction.service.StoreStaffService;
import com.moodcafe.store.entity.Store;
import com.moodcafe.store.entity.enums.StoreStatus;
import com.moodcafe.voucher.abstraction.repository.CustomerVoucherRepository;
import com.moodcafe.voucher.abstraction.repository.VoucherPointAllocationRepository;
import com.moodcafe.voucher.entity.CustomerVoucher;
import com.moodcafe.voucher.service.VoucherLifecycleService;
import com.moodcafe.wallet.dto.RefundPointCommand;
import com.moodcafe.wallet.service.CustomerWalletService;
import com.moodcafe.wallet.service.MerchantWalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingMoneyLifecycleTest {

    @Mock
    private BookingCommissionService commissionService;
    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private TableCategoryRepository tableCategoryRepository;
    @Mock
    private StoreBookingConfigRepository configRepository;
    @Mock
    private StoreRepository storeRepository;
    @Mock
    private StoreStaffService storeStaffService;
    @Mock
    private CurrentUserService currentUserService;
    @Mock
    private PaymentService paymentService;
    @Spy
    private BookingMapper bookingMapper = Mappers.getMapper(BookingMapper.class);
    @Mock
    private StoreImageRepository storeImageRepository;
    @Mock
    private NotificationDispatcherService notificationDispatcherService;
    @Mock
    private CustomerVoucherRepository customerVoucherRepository;
    @Mock
    private VoucherPointAllocationRepository voucherPointAllocationRepository;
    @Mock
    private ReservationPricingService reservationPricingService;
    @Mock
    private MerchantWalletService merchantWalletService;
    @Mock
    private CustomerWalletService customerWalletService;
    @Mock
    private LoyaltyRewardService loyaltyRewardService;
    @Mock
    private VoucherLifecycleService voucherLifecycleService;
    @Mock
    private PlatformTransactionManager transactionManager;

    private BookingServiceImpl bookingService;

    private UUID storeId;
    private UUID categoryId;
    private UUID userId;
    private Store store;
    private TableCategory category;
    private StoreBookingConfig config;
    private User customer;

    @BeforeEach
    void setUp() {
        storeId = UUID.randomUUID();
        categoryId = UUID.randomUUID();
        userId = UUID.randomUUID();

        customer = User.builder().userId(userId).fullName("Customer Test").email("cust@test.com").build();
        store = Store.builder()
                .storeId(storeId)
                .name("Mood Cafe Nha Trang")
                .status(StoreStatus.ACTIVE)
                .openingTime(LocalTime.of(7, 0))
                .closingTime(LocalTime.of(22, 0))
                .build();

        category = TableCategory.builder()
                .categoryId(categoryId)
                .store(store)
                .name("Ban Vip")
                .maxCapacity(4)
                .appQuota(5)
                .minSpendAmount(200000L)
                .isActive(true)
                .build();

        config = StoreBookingConfig.builder()
                .store(store)
                .isBookingEnabled(true)
                .minAdvanceMins(30)
                .gracePeriodMins(30)
                .freeCancelHours(2)
                .sittingDurationMins(120)
                .build();

        bookingService = new BookingServiceImpl(
                commissionService,
                bookingRepository,
                tableCategoryRepository,
                configRepository,
                storeRepository,
                storeStaffService,
                currentUserService,
                paymentService,
                bookingMapper,
                storeImageRepository,
                notificationDispatcherService,
                null,
                null,
                customerVoucherRepository,
                voucherPointAllocationRepository,
                reservationPricingService,
                merchantWalletService,
                customerWalletService,
                loyaltyRewardService,
                voucherLifecycleService,
                transactionManager
        );

        lenient().when(commissionService.record(any(), any())).thenReturn(true);
        lenient().when(storeStaffService.requireStoreAccess(any(), any())).thenReturn(null);
    }

    @Test
    @DisplayName("Check-In: On-time awards 1,000 MoodPoints and settles pending revenue to available")
    void testDeskCheckIn_OnTimeAwardsPoints_SettlesToAvailable() {
        when(currentUserService.getCurrentUser()).thenReturn(customer);

        UUID bookingId = UUID.randomUUID();
        Booking booking = Booking.builder()
                .bookingId(bookingId)
                .bookingCode("MC-NT-101")
                .store(store)
                .tableCategory(category)
                .user(customer)
                .status(BookingStatus.PAID_CONFIRMED)
                .qrCodeToken("QR_TOKEN_101")
                .startTime(Instant.now().minus(Duration.ofMinutes(5)))
                .endTime(Instant.now().plus(Duration.ofHours(2)))
                .customerPaidAmount(new BigDecimal("150000.00"))
                .merchantNetAmount(new BigDecimal("170000.00"))
                .graceDeadline(Instant.now().plus(Duration.ofMinutes(15))) // On time
                .build();

        when(bookingRepository.findAllByStoreStoreIdOrderByCreatedAtDesc(storeId))
                .thenReturn(Collections.singletonList(booking));
        when(bookingRepository.findByIdForUpdate(bookingId)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        DeskCheckInRequest req = DeskCheckInRequest.builder()
                .codeOrToken("QR_TOKEN_101")
                .build();

        // Act
        BookingResponse res = bookingService.deskCheckIn(storeId, req);

        // Assert
        assertEquals(BookingStatus.CHECKED_IN, res.getStatus());
        verify(loyaltyRewardService).awardBookingOnTimeCheckIn(booking);
        verify(merchantWalletService).settleToAvailable(
                eq(storeId),
                eq(new BigDecimal("170000.00")),
                eq("BOOKING"),
                eq(bookingId.toString()),
                contains("MC-NT-101")
        );
    }

    @Test
    @DisplayName("Complete: Staff completes checkout, transitions status to COMPLETED")
    void testDeskComplete_CompletesBooking() {
        when(currentUserService.getCurrentUser()).thenReturn(customer);

        UUID bookingId = UUID.randomUUID();
        Booking booking = Booking.builder()
                .bookingId(bookingId)
                .bookingCode("MC-NT-102")
                .store(store)
                .tableCategory(category)
                .user(customer)
                .status(BookingStatus.CHECKED_IN)
                .grossAmount(new BigDecimal("200000.00"))
                .customerPaidAmount(new BigDecimal("150000.00"))
                .merchantNetAmount(new BigDecimal("170000.00"))
                .build();

        lenient().when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        lenient().when(bookingRepository.findByIdForUpdate(bookingId)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        BookingResponse res = bookingService.deskComplete(storeId, bookingId);

        // Assert
        assertEquals(BookingStatus.COMPLETED, res.getStatus());
    }

    @Test
    @DisplayName("Request Checkout: Customer transitions CHECKED_IN booking to CHECK_OUT_PENDING")
    void testRequestCheckout_Success() {
        when(currentUserService.getCurrentUser()).thenReturn(customer);
        UUID id = UUID.randomUUID();
        Booking booking = Booking.builder().bookingId(id).user(customer).store(store)
                .status(BookingStatus.CHECKED_IN).checkedInAt(Instant.now().minusSeconds(2700)).build();
        lenient().when(bookingRepository.findById(id)).thenReturn(Optional.of(booking));
        lenient().when(bookingRepository.findByIdForUpdate(id)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        BookingResponse res = bookingService.requestCheckout(id);

        assertEquals(BookingStatus.CHECK_OUT_PENDING, res.getStatus());
    }

    @Test
    @DisplayName("Cancellation before deadline: Full refund in MoodPoints (1 pt = 1 VND), reverts merchant pending, restores voucher")
    void testCancelBooking_FreeCancel_FullPointsRefundAndReversal() {
        when(currentUserService.getCurrentUser()).thenReturn(customer);

        UUID bookingId = UUID.randomUUID();
        UUID voucherId = UUID.randomUUID();
        CustomerVoucher voucher = CustomerVoucher.builder()
                .voucherId(voucherId)
                .status("USED")
                .lockedBookingId(bookingId)
                .build();

        Booking booking = Booking.builder()
                .bookingId(bookingId)
                .bookingCode("MC-NT-104")
                .store(store)
                .tableCategory(category)
                .user(customer)
                .status(BookingStatus.PAID_CONFIRMED)
                .startTime(Instant.now().plus(Duration.ofHours(5)))
                .freeCancelDeadline(Instant.now().plus(Duration.ofHours(3))) // Within deadline
                .customerPaidAmount(new BigDecimal("150000.00"))
                .actualPaidAmount(new BigDecimal("150000.00"))
                .merchantNetAmount(new BigDecimal("170000.00"))
                .voucher(voucher)
                .build();

        lenient().when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        lenient().when(bookingRepository.findByIdForUpdate(bookingId)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        CancelBookingRequest req = CancelBookingRequest.builder().reason("Change of plans").build();

        // Act
        BookingResponse res = bookingService.cancelBooking(bookingId, req);

        // Assert
        assertEquals(BookingStatus.CANCELLED_REFUNDED, res.getStatus());
        verify(voucherLifecycleService).restoreVoucherForRefund(bookingId, voucherId);

        // Revert merchant pending
        verify(merchantWalletService).revertPending(
                eq(storeId),
                eq(new BigDecimal("170000.00")),
                eq("BOOKING"),
                eq(bookingId.toString()),
                contains("MC-NT-104")
        );

        // Credit customer MoodPoints 1:1
        ArgumentCaptor<RefundPointCommand> cmdCaptor = ArgumentCaptor.forClass(RefundPointCommand.class);
        verify(customerWalletService).creditRefundPoints(cmdCaptor.capture());
        RefundPointCommand capturedCmd = cmdCaptor.getValue();
        assertEquals(userId, capturedCmd.getUserId());
        assertEquals(150000L, capturedCmd.getPoints());
        assertEquals("REFUND_BOOKING_" + bookingId, capturedCmd.getBusinessKey());
    }

    @Test
    @DisplayName("Cancellation past deadline: Rejected with CANNOT_CANCEL_BOOKING")
    void testCancelBooking_PastDeadline_Rejected() {
        when(currentUserService.getCurrentUser()).thenReturn(customer);

        UUID bookingId = UUID.randomUUID();
        Booking booking = Booking.builder()
                .bookingId(bookingId)
                .store(store)
                .tableCategory(category)
                .user(customer)
                .status(BookingStatus.PAID_CONFIRMED)
                .startTime(Instant.now().plus(Duration.ofMinutes(30)))
                .freeCancelDeadline(Instant.now().minus(Duration.ofMinutes(90))) // Past deadline
                .customerPaidAmount(new BigDecimal("150000.00"))
                .build();

        lenient().when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        lenient().when(bookingRepository.findByIdForUpdate(bookingId)).thenReturn(Optional.of(booking));

        CancelBookingRequest req = CancelBookingRequest.builder().reason("Late cancel").build();

        // Act & Assert
        AppException ex = assertThrows(AppException.class, () ->
                bookingService.cancelBooking(bookingId, req)
        );
        assertTrue(ex.getMessage().contains("quá thời hạn"));
        verify(customerWalletService, never()).creditRefundPoints(any());
        verify(merchantWalletService, never()).revertPending(any(), any(), any(), any(), any());
    }
}
