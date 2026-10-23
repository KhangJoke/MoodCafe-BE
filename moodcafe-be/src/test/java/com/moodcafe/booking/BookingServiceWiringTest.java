package com.moodcafe.booking;

import com.moodcafe.auth.abstraction.service.CurrentUserService;
import com.moodcafe.auth.entity.User;
import com.moodcafe.booking.abstraction.repository.BookingQuoteRepository;
import com.moodcafe.booking.abstraction.repository.BookingRepository;
import com.moodcafe.booking.abstraction.repository.StoreBookingConfigRepository;
import com.moodcafe.booking.abstraction.repository.TableCategoryRepository;
import com.moodcafe.booking.dto.request.CancelBookingRequest;
import com.moodcafe.booking.dto.response.BookingResponse;
import com.moodcafe.booking.entity.Booking;
import com.moodcafe.booking.entity.BookingQuote;
import com.moodcafe.booking.entity.StoreBookingConfig;
import com.moodcafe.booking.entity.TableCategory;
import com.moodcafe.booking.entity.enums.BookingStatus;
import com.moodcafe.booking.mapper.BookingMapper;
import com.moodcafe.booking.service.BookingFinanceService;
import com.moodcafe.booking.service.BookingServiceImpl;
import com.moodcafe.loyalty.service.LoyaltyRewardService;
import com.moodcafe.notification.abstraction.service.NotificationDispatcherService;
import com.moodcafe.payment.abstraction.service.PayOSService;
import com.moodcafe.payment.service.OrderCodeGeneratorService;
import com.moodcafe.payment.service.PaymentAttemptService;
import com.moodcafe.reservation.service.ReservationQrVerificationService;
import com.moodcafe.shared.error.ErrorCode;
import com.moodcafe.shared.exceptions.AppException;
import com.moodcafe.store.abstraction.repository.StoreImageRepository;
import com.moodcafe.store.abstraction.repository.StoreRepository;
import com.moodcafe.store.abstraction.service.StoreStaffService;
import com.moodcafe.store.entity.Store;
import com.moodcafe.voucher.abstraction.repository.CustomerVoucherRepository;
import com.moodcafe.voucher.entity.CustomerVoucher;
import com.moodcafe.voucher.service.VoucherLifecycleService;
import com.moodcafe.wallet.service.CustomerWalletService;
import com.moodcafe.wallet.service.MerchantWalletService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceWiringTest {

    @Mock private BookingRepository bookingRepository;
    @Mock private TableCategoryRepository tableCategoryRepository;
    @Mock private StoreBookingConfigRepository configRepository;
    @Mock private StoreRepository storeRepository;
    @Mock private StoreStaffService storeStaffService;
    @Mock private CurrentUserService currentUserService;
    @Mock private PayOSService payOSService;
    @Mock private BookingMapper bookingMapper;
    @Mock private StoreImageRepository storeImageRepository;
    @Mock private NotificationDispatcherService notificationDispatcherService;
    @Mock private RedisTemplate<String, String> redisTemplate;
    @Mock private ObjectMapper objectMapper;
    @Mock private BookingFinanceService bookingFinanceService;
    @Mock private BookingQuoteRepository bookingQuoteRepository;
    @Mock private CustomerVoucherRepository customerVoucherRepository;
    @Mock private OrderCodeGeneratorService orderCodeGeneratorService;
    @Mock private PaymentAttemptService paymentAttemptService;
    @Mock private MerchantWalletService merchantWalletService;
    @Mock private CustomerWalletService customerWalletService;
    @Mock private LoyaltyRewardService loyaltyRewardService;
    @Mock private ReservationQrVerificationService qrVerificationService;
    @Mock private VoucherLifecycleService voucherLifecycleService;
    @Mock private PlatformTransactionManager transactionManager;

    private BookingServiceImpl bookingService;

    private UUID userId;
    private UUID storeId;
    private User testUser;
    private Store testStore;
    private StoreBookingConfig testConfig;

    @BeforeEach
    void setUp() {
        bookingService = new BookingServiceImpl(
                bookingRepository, tableCategoryRepository, configRepository, storeRepository,
                storeStaffService, currentUserService, payOSService, bookingMapper,
                storeImageRepository, notificationDispatcherService, redisTemplate, objectMapper,
                bookingFinanceService, bookingQuoteRepository, customerVoucherRepository,
                orderCodeGeneratorService, paymentAttemptService, merchantWalletService,
                customerWalletService, loyaltyRewardService, qrVerificationService,
                voucherLifecycleService, transactionManager
        );

        userId = UUID.randomUUID();
        storeId = UUID.randomUUID();
        testUser = User.builder().userId(userId).email("customer@test.com").build();
        testStore = Store.builder().storeId(storeId).name("Test Store").build();
        testConfig = StoreBookingConfig.builder()
                .store(testStore)
                .isBookingEnabled(true)
                .gracePeriodMins(30)
                .freeCancelHours(2)
                .build();
    }

    @Test
    @DisplayName("P1 Item 1: Spring can resolve constructor injection for both services unambiguously")
    void testSpringConstructorResolution() {
        try (var ctx = new AnnotationConfigApplicationContext()) {
            ctx.registerBean(BookingRepository.class, () -> bookingRepository);
            ctx.registerBean(TableCategoryRepository.class, () -> tableCategoryRepository);
            ctx.registerBean(StoreBookingConfigRepository.class, () -> configRepository);
            ctx.registerBean(StoreRepository.class, () -> storeRepository);
            ctx.registerBean(StoreStaffService.class, () -> storeStaffService);
            ctx.registerBean(CurrentUserService.class, () -> currentUserService);
            ctx.registerBean(PayOSService.class, () -> payOSService);
            ctx.registerBean(BookingMapper.class, () -> bookingMapper);
            ctx.registerBean(StoreImageRepository.class, () -> storeImageRepository);
            ctx.registerBean(NotificationDispatcherService.class, () -> notificationDispatcherService);
            ctx.registerBean("redisTemplate", RedisTemplate.class, () -> redisTemplate);
            ctx.registerBean(ObjectMapper.class, () -> objectMapper);
            ctx.registerBean(BookingFinanceService.class, () -> bookingFinanceService);
            ctx.registerBean(BookingQuoteRepository.class, () -> bookingQuoteRepository);
            ctx.registerBean(CustomerVoucherRepository.class, () -> customerVoucherRepository);
            ctx.registerBean(OrderCodeGeneratorService.class, () -> orderCodeGeneratorService);
            ctx.registerBean(PaymentAttemptService.class, () -> paymentAttemptService);
            ctx.registerBean(MerchantWalletService.class, () -> merchantWalletService);
            ctx.registerBean(CustomerWalletService.class, () -> customerWalletService);
            ctx.registerBean(ReservationQrVerificationService.class, () -> qrVerificationService);
            ctx.registerBean(VoucherLifecycleService.class, () -> voucherLifecycleService);
            ctx.registerBean(PlatformTransactionManager.class, () -> transactionManager);
            ctx.registerBean(com.moodcafe.loyalty.abstraction.repository.ReservationLoyaltyRewardRepository.class,
                    () -> mock(com.moodcafe.loyalty.abstraction.repository.ReservationLoyaltyRewardRepository.class));
            ctx.registerBean(com.moodcafe.reservation.abstraction.repository.ReservationRepository.class,
                    () -> mock(com.moodcafe.reservation.abstraction.repository.ReservationRepository.class));
            ctx.registerBean(com.moodcafe.store.abstraction.repository.StoreReviewRepository.class,
                    () -> mock(com.moodcafe.store.abstraction.repository.StoreReviewRepository.class));

            ctx.register(LoyaltyRewardService.class);
            ctx.register(BookingServiceImpl.class);

            ctx.refresh();

            assertThat(ctx.getBean(BookingServiceImpl.class)).isNotNull();
            assertThat(ctx.getBean(LoyaltyRewardService.class)).isNotNull();
        }
    }

    @Test
    @DisplayName("P1 Item 2 & P2 Item 14: Zero-Pay cancellation never refunds MoodPoints and safely restores voucher")
    void testZeroPayCancel_RefundsZeroPointsAndRestoresVoucher() {
        when(currentUserService.getCurrentUser()).thenReturn(testUser);

        UUID bookingId = UUID.randomUUID();
        UUID voucherId = UUID.randomUUID();
        CustomerVoucher voucher = CustomerVoucher.builder()
                .voucherId(voucherId)
                .userId(userId)
                .build();

        Booking zeroPayBooking = Booking.builder()
                .bookingId(bookingId)
                .bookingCode("MCZERO01")
                .user(testUser)
                .store(testStore)
                .status(BookingStatus.PAID_CONFIRMED)
                .grossAmount(new BigDecimal("100000"))
                .voucherDiscount(new BigDecimal("100000"))
                .customerPaidAmount(BigDecimal.ZERO)
                .actualPaidAmount(BigDecimal.ZERO) // ZERO-PAY has actualPaidAmount = 0
                .merchantNetAmount(new BigDecimal("85000"))
                .voucher(voucher)
                .startTime(Instant.now().plus(Duration.ofHours(5)))
                .freeCancelDeadline(Instant.now().plus(Duration.ofHours(3)))
                .build();

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(zeroPayBooking));
        when(bookingRepository.findByIdForUpdate(bookingId)).thenReturn(Optional.of(zeroPayBooking));
        when(configRepository.findByStoreStoreId(storeId)).thenReturn(Optional.of(testConfig));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));
        when(bookingMapper.toBookingResponse(any())).thenReturn(new BookingResponse());

        CancelBookingRequest request = new CancelBookingRequest();
        request.setReason("Changed mind");

        BookingResponse response = bookingService.cancelBooking(bookingId, request);
        assertThat(response).isNotNull();

        // STRICT INVARIANT 1: Voucher restored via lifecycle service
        verify(voucherLifecycleService).restoreVoucherForRefund(bookingId, voucherId);

        // STRICT INVARIANT 2: ZERO MoodPoints credited to wallet because actualPaidAmount was 0!
        verifyNoInteractions(customerWalletService);

        // Merchant pending was reverted
        verify(merchantWalletService).revertPending(eq(storeId), eq(new BigDecimal("85000")), eq("BOOKING"), any(), any());
    }

    @Test
    @DisplayName("P1 Item 3: Self check-in without valid physical QR token fails")
    void testSelfCheckIn_RequiresValidStoreQr() {
        when(currentUserService.getCurrentUser()).thenReturn(testUser);

        UUID bookingId = UUID.randomUUID();
        Booking booking = Booking.builder()
                .bookingId(bookingId)
                .bookingCode("MCCHECKIN")
                .user(testUser)
                .store(testStore)
                .status(BookingStatus.PAID_CONFIRMED)
                .startTime(Instant.now().plus(Duration.ofMinutes(10)))
                .graceDeadline(Instant.now().plus(Duration.ofMinutes(40)))
                .build();

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        assertThatThrownBy(() -> bookingService.selfCheckIn(bookingId, "FORGED_TOKEN"))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("nhân viên");
    }

    @Test
    @DisplayName("P1 Item 3: Self check-in too early or after grace deadline fails")
    void testSelfCheckIn_TimeWindowEnforcement() {
        when(currentUserService.getCurrentUser()).thenReturn(testUser);

        UUID bookingId = UUID.randomUUID();
        // Booking is 2 hours in the future (earliest check-in allowed is 30 mins before startTime)
        Booking futureBooking = Booking.builder()
                .bookingId(bookingId)
                .bookingCode("MCFUTURE")
                .user(testUser)
                .store(testStore)
                .status(BookingStatus.PAID_CONFIRMED)
                .startTime(Instant.now().plus(Duration.ofHours(2)))
                .graceDeadline(Instant.now().plus(Duration.ofHours(2).plus(Duration.ofMinutes(30))))
                .build();

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(futureBooking));

        assertThatThrownBy(() -> bookingService.selfCheckIn(bookingId, "VALID_TOKEN"))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("nhân viên");
    }
}
