package com.moodcafe.booking;

import com.moodcafe.auth.abstraction.service.CurrentUserService;
import com.moodcafe.auth.entity.User;
import com.moodcafe.booking.abstraction.repository.BookingRepository;
import com.moodcafe.booking.abstraction.repository.StoreBookingConfigRepository;
import com.moodcafe.booking.abstraction.repository.TableCategoryRepository;
import com.moodcafe.booking.dto.request.CreateBookingRequest;
import com.moodcafe.booking.dto.response.BookingResponse;
import com.moodcafe.booking.entity.Booking;
import com.moodcafe.booking.entity.StoreBookingConfig;
import com.moodcafe.booking.entity.TableCategory;
import com.moodcafe.booking.entity.enums.BookingStatus;
import com.moodcafe.booking.mapper.BookingMapper;
import com.moodcafe.booking.service.BookingServiceImpl;
import com.moodcafe.finance.dto.ReservationFinancialSnapshot;
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
import com.moodcafe.wallet.service.CustomerWalletService;
import com.moodcafe.wallet.service.MerchantWalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;

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
class BookingPaymentLifecycleTest {

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

        customer = User.builder().userId(userId).fullName("Tran Van B").email("b@test.com").build();
        store = Store.builder()
                .storeId(storeId)
                .name("Mood Cafe Da Nang")
                .status(StoreStatus.ACTIVE)
                .openingTime(LocalTime.of(7, 0))
                .closingTime(LocalTime.of(22, 0))
                .build();

        category = TableCategory.builder()
                .categoryId(categoryId)
                .store(store)
                .name("Ban 4 nguoi")
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
    }

    @Test
    @DisplayName("PayOS Webhook: Confirms pending booking, marks voucher USED, holds merchant pending net balance")
    void testConfirmBookingPayment_Success() {
        Long orderCode = 888999L;
        UUID bookingId = UUID.randomUUID();
        UUID voucherId = UUID.randomUUID();

        CustomerVoucher voucher = CustomerVoucher.builder()
                .voucherId(voucherId)
                .status("LOCKED")
                .lockedBookingId(bookingId)
                .build();

        Booking booking = Booking.builder()
                .bookingId(bookingId)
                .bookingCode("MC-DN-001")
                .store(store)
                .tableCategory(category)
                .user(customer)
                .status(BookingStatus.PENDING_PAYMENT)
                .paymentOrderCode(orderCode)
                .grossAmount(new BigDecimal("200000.00"))
                .customerPaidAmount(new BigDecimal("150000.00"))
                .merchantNetAmount(new BigDecimal("170000.00"))
                .commissionAmount(new BigDecimal("30000.00"))
                .voucher(voucher)
                .build();

        when(bookingRepository.findByPaymentOrderCodeForUpdate(orderCode)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act: Webhook confirmation with paidAmount = 150,000 VND
        bookingService.confirmBookingPayment(orderCode, 150000L);

        // Assert
        assertEquals(BookingStatus.PAID_CONFIRMED, booking.getStatus());
        assertEquals(new BigDecimal("150000"), booking.getActualPaidAmount());
        assertNotNull(booking.getPaidAt());
        assertEquals("USED", voucher.getStatus());
        assertNotNull(voucher.getUsedAt());

        verify(customerVoucherRepository).save(voucher);
        verify(merchantWalletService).holdPending(
                eq(storeId),
                eq(new BigDecimal("170000.00")),
                eq("BOOKING"),
                eq(bookingId.toString()),
                contains("MC-DN-001")
        );
    }

    @Test
    @DisplayName("PayOS Webhook: Underpayment triggers BAD_REQUEST exception")
    void testConfirmBookingPayment_UnderpaymentFails() {
        Long orderCode = 888999L;
        Booking booking = Booking.builder()
                .bookingId(UUID.randomUUID())
                .status(BookingStatus.PENDING_PAYMENT)
                .paymentOrderCode(orderCode)
                .customerPaidAmount(new BigDecimal("150000.00"))
                .build();

        when(bookingRepository.findByPaymentOrderCodeForUpdate(orderCode)).thenReturn(Optional.of(booking));

        // Act & Assert
        AppException ex = assertThrows(AppException.class, () ->
                bookingService.confirmBookingPayment(orderCode, 100000L) // Only paid 100k vs 150k required
        );
        assertTrue(ex.getMessage().contains("không khớp"));
        assertEquals(BookingStatus.PENDING_PAYMENT, booking.getStatus());
        verify(merchantWalletService, never()).holdPending(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("PayOS Webhook: Idempotent repeat webhook does not duplicate state or pending hold")
    void testConfirmBookingPayment_Idempotent() {
        Long orderCode = 888999L;
        Booking booking = Booking.builder()
                .bookingId(UUID.randomUUID())
                .bookingCode("MC-DN-002")
                .status(BookingStatus.PAID_CONFIRMED)
                .paymentOrderCode(orderCode)
                .build();

        when(bookingRepository.findByPaymentOrderCodeForUpdate(orderCode)).thenReturn(Optional.of(booking));

        // Act
        bookingService.confirmBookingPayment(orderCode, 150000L);

        // Assert: No-op
        verify(bookingRepository, never()).save(any());
        verify(merchantWalletService, never()).holdPending(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Zero-Pay Booking: 100% voucher creates PAID_CONFIRMED immediately and holds merchant pending net balance without PayOS call")
    void testCreateBooking_ZeroPayPath() {
        when(currentUserService.getCurrentUser()).thenReturn(customer);
        when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
        when(configRepository.findByStoreStoreId(storeId)).thenReturn(Optional.of(config));
        when(tableCategoryRepository.findByIdWithLock(categoryId)).thenReturn(Optional.of(category));
        when(bookingRepository.countOverlappingBookings(any(), any(), any(), any())).thenReturn(0L);
        when(paymentService.generateOrderCode()).thenReturn(999111L);

        UUID voucherId = UUID.randomUUID();
        CustomerVoucher voucher = CustomerVoucher.builder()
                .voucherId(voucherId)
                .userId(userId)
                .status("AVAILABLE")
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
        when(customerVoucherRepository.findByIdWithLock(voucherId)).thenReturn(Optional.of(voucher));

        // Financial snapshot with 100% discount
        ReservationFinancialSnapshot zeroPaySnapshot = ReservationFinancialSnapshot.builder()
                .grossAmount(new BigDecimal("200000.00"))
                .voucherDiscount(new BigDecimal("200000.00"))
                .customerPaidAmount(BigDecimal.ZERO)
                .commissionRate(new BigDecimal("0.15"))
                .commissionAmount(new BigDecimal("30000.00"))
                .merchantNetAmount(new BigDecimal("170000.00"))
                .voucherRefundFundedAmount(BigDecimal.ZERO)
                .voucherRewardFundedAmount(new BigDecimal("200000.00"))
                .build();

        when(reservationPricingService.quote(any())).thenReturn(zeroPaySnapshot);

        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> {
            Booking b = inv.getArgument(0);
            b.setBookingId(UUID.randomUUID());
            return b;
        });

        CreateBookingRequest request = CreateBookingRequest.builder()
                .storeId(storeId)
                .categoryId(categoryId)
                .startTime(Instant.now().plusSeconds(7200))
                .guestCount(2)
                .voucherId(voucherId)
                .customerName("Tran Van B")
                .customerPhone("0909123456")
                .build();

        // Act
        BookingResponse response = bookingService.createBooking(request);

        // Assert
        assertEquals(BookingStatus.PAID_CONFIRMED, response.getStatus());
        assertNull(response.getCheckoutUrl());
        assertEquals("USED", voucher.getStatus());
        verify(paymentService, never()).createBookingPaymentLink(any(), anyLong(), anyLong(), any(), any());
        verify(merchantWalletService).holdPending(
                eq(storeId),
                eq(new BigDecimal("170000.00")),
                eq("BOOKING"),
                any(),
                contains("100% voucher")
        );
    }
    @Test
    void missingVerifiedAmountCannotConfirmPayment() {
        Booking booking = Booking.builder().bookingId(UUID.randomUUID()).status(BookingStatus.PENDING_PAYMENT)
                .customerPaidAmount(BigDecimal.valueOf(150000)).build();
        when(bookingRepository.findByPaymentOrderCodeForUpdate(123L)).thenReturn(Optional.of(booking));
        assertThrows(AppException.class, () -> bookingService.confirmBookingPayment(123L, null));
        assertEquals(BookingStatus.PENDING_PAYMENT, booking.getStatus());
    }


    @Test
    void verifiedLatePaymentOnClosedBookingRefundsActualCashIdempotently() {
        Booking booking = Booking.builder().bookingId(UUID.randomUUID()).user(customer).store(store)
                .status(BookingStatus.PAYMENT_FAILED).customerPaidAmount(BigDecimal.valueOf(150000))
                .actualPaidAmount(BigDecimal.ZERO).paymentOrderCode(123L).build();
        when(bookingRepository.findByPaymentOrderCodeForUpdate(123L)).thenReturn(Optional.of(booking));
        bookingService.confirmBookingPayment(123L, 160000L);
        bookingService.confirmBookingPayment(123L, 160000L);
        assertEquals(BigDecimal.valueOf(160000), booking.getActualPaidAmount());
        assertTrue(booking.getRefundProcessed());
        var command = org.mockito.ArgumentCaptor.forClass(com.moodcafe.wallet.dto.RefundPointCommand.class);
        verify(customerWalletService, times(1)).creditRefundPoints(command.capture());
        assertEquals(160000L, command.getValue().getPoints());
        assertEquals("REFUND_BOOKING_" + booking.getBookingId(), command.getValue().getBusinessKey());
        verifyNoInteractions(merchantWalletService);
    }

    @Test
    void alreadyRefundedPartialPaymentRefundsOnlyVerifiedCumulativeIncrease() {
        Booking booking = Booking.builder().bookingId(UUID.randomUUID()).user(customer).store(store)
                .status(BookingStatus.CANCELLED_REFUNDED).customerPaidAmount(BigDecimal.valueOf(100))
                .actualPaidAmount(BigDecimal.valueOf(40)).refundProcessed(true).paymentOrderCode(123L).build();
        when(bookingRepository.findByPaymentOrderCodeForUpdate(123L)).thenReturn(Optional.of(booking));
        bookingService.confirmBookingPayment(123L, 100L);
        bookingService.confirmBookingPayment(123L, 100L);
        bookingService.confirmBookingPayment(123L, 40L);
        assertEquals(BigDecimal.valueOf(100), booking.getActualPaidAmount());
        var command = org.mockito.ArgumentCaptor.forClass(com.moodcafe.wallet.dto.RefundPointCommand.class);
        verify(customerWalletService, times(1)).creditRefundPoints(command.capture());
        assertEquals(60L, command.getValue().getPoints());
        assertTrue(command.getValue().getBusinessKey().contains("100"));
    }

}
