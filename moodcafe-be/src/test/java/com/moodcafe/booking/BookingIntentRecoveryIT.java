package com.moodcafe.booking;

import com.moodcafe.auth.abstraction.service.CurrentUserService;
import com.moodcafe.auth.entity.User;
import com.moodcafe.booking.abstraction.repository.BookingQuoteRepository;
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
import com.moodcafe.booking.service.BookingFinanceService;
import com.moodcafe.booking.service.BookingServiceImpl;
import com.moodcafe.finance.dto.ReservationFinancialSnapshot;
import com.moodcafe.notification.abstraction.service.NotificationDispatcherService;
import com.moodcafe.payment.abstraction.service.PayOSService;
import com.moodcafe.payment.entity.PaymentAttempt;
import com.moodcafe.payment.service.OrderCodeGeneratorService;
import com.moodcafe.payment.service.PaymentAttemptService;
import com.moodcafe.shared.exceptions.AppException;
import com.moodcafe.store.abstraction.repository.StoreImageRepository;
import com.moodcafe.store.abstraction.repository.StoreRepository;
import com.moodcafe.store.abstraction.service.StoreStaffService;
import com.moodcafe.store.entity.Store;
import com.moodcafe.store.entity.enums.StoreStatus;
import com.moodcafe.voucher.abstraction.repository.CustomerVoucherRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingIntentRecoveryIT {

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
    private PayOSService payOSService;
    @Spy
    private BookingMapper bookingMapper = Mappers.getMapper(BookingMapper.class);
    @Mock
    private StoreImageRepository storeImageRepository;
    @Mock
    private NotificationDispatcherService notificationDispatcherService;
    @Mock
    private BookingFinanceService bookingFinanceService;
    @Mock
    private BookingQuoteRepository bookingQuoteRepository;
    @Mock
    private CustomerVoucherRepository customerVoucherRepository;
    @Mock
    private OrderCodeGeneratorService orderCodeGeneratorService;
    @Mock
    private PaymentAttemptService paymentAttemptService;

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

        customer = User.builder().userId(userId).fullName("Nguyen Van A").email("a@test.com").build();
        store = Store.builder()
                .storeId(storeId)
                .name("Mood Cafe Saigon")
                .status(StoreStatus.ACTIVE)
                .openingTime(LocalTime.of(7, 0))
                .closingTime(LocalTime.of(22, 0))
                .build();

        category = TableCategory.builder()
                .categoryId(categoryId)
                .store(store)
                .name("Ban 2 nguoi")
                .maxCapacity(2)
                .appQuota(5)
                .minSpendAmount(100000L)
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
                bookingRepository,
                tableCategoryRepository,
                configRepository,
                storeRepository,
                storeStaffService,
                currentUserService,
                payOSService,
                bookingMapper,
                storeImageRepository,
                notificationDispatcherService,
                null,
                null,
                bookingFinanceService,
                bookingQuoteRepository,
                customerVoucherRepository,
                orderCodeGeneratorService,
                paymentAttemptService
        );
    }

    @Test
    @DisplayName("Replay with same intent key and identical payload returns existing booking")
    void testReplayIdenticalIntentKey() {
        when(currentUserService.getCurrentUser()).thenReturn(customer);

        String intentKey = "intent-uuid-123";
        Instant startTime = Instant.now().plusSeconds(3600);

        CreateBookingRequest req = CreateBookingRequest.builder()
                .storeId(storeId)
                .categoryId(categoryId)
                .startTime(startTime)
                .guestCount(2)
                .customerName("Nguyen Van A")
                .customerPhone("0901234567")
                .intentKey(intentKey)
                .build();

        String expectedHash = BookingServiceImpl.computeBookingPayloadHash(req);
        UUID existingBookingId = UUID.randomUUID();

        PaymentAttempt existingAttempt = PaymentAttempt.builder()
                .attemptId(UUID.randomUUID())
                .purpose("BOOKING")
                .businessId(existingBookingId.toString())
                .orderCode(2000000099L)
                .intentKey(intentKey)
                .actorId(userId)
                .payloadHash(expectedHash)
                .status("PENDING")
                .build();

        when(paymentAttemptService.findByActorIdAndPurposeAndIntentKey(userId, "BOOKING", intentKey))
                .thenReturn(Optional.of(existingAttempt));

        Booking existingBooking = Booking.builder()
                .bookingId(existingBookingId)
                .bookingCode("MC999999")
                .store(store)
                .tableCategory(category)
                .user(customer)
                .customerName("Nguyen Van A")
                .customerPhone("0901234567")
                .startTime(startTime)
                .endTime(startTime.plusSeconds(7200))
                .status(BookingStatus.PENDING_PAYMENT)
                .paymentOrderCode(2000000099L)
                .customerPaidAmount(new BigDecimal("100000"))
                .grossAmount(new BigDecimal("100000"))
                .build();

        when(bookingRepository.findById(existingBookingId)).thenReturn(Optional.of(existingBooking));
        when(configRepository.findByStoreStoreId(storeId)).thenReturn(Optional.of(config));

        BookingResponse response = bookingService.createBooking(req);

        assertNotNull(response);
        assertEquals(existingBookingId, response.getBookingId());
        assertEquals("MC999999", response.getBookingCode());
        // Verify no new booking was created
        verify(bookingRepository, never()).save(any());
        verify(orderCodeGeneratorService, never()).nextOrderCode();
    }

    @Test
    @DisplayName("Reusing intent key with DIFFERENT payload throws 409 CONFLICT")
    void testConflictOnAlteredPayloadWithSameIntentKey() {
        when(currentUserService.getCurrentUser()).thenReturn(customer);

        String intentKey = "intent-uuid-123";
        Instant startTime = Instant.now().plusSeconds(3600);

        CreateBookingRequest originalReq = CreateBookingRequest.builder()
                .storeId(storeId)
                .categoryId(categoryId)
                .startTime(startTime)
                .guestCount(2)
                .customerName("Nguyen Van A")
                .customerPhone("0901234567")
                .intentKey(intentKey)
                .build();

        String originalHash = BookingServiceImpl.computeBookingPayloadHash(originalReq);

        PaymentAttempt existingAttempt = PaymentAttempt.builder()
                .attemptId(UUID.randomUUID())
                .purpose("BOOKING")
                .businessId(UUID.randomUUID().toString())
                .orderCode(2000000099L)
                .intentKey(intentKey)
                .actorId(userId)
                .payloadHash(originalHash)
                .status("PENDING")
                .build();

        when(paymentAttemptService.findByActorIdAndPurposeAndIntentKey(userId, "BOOKING", intentKey))
                .thenReturn(Optional.of(existingAttempt));

        // Different request: guestCount changed to 1
        CreateBookingRequest differentReq = CreateBookingRequest.builder()
                .storeId(storeId)
                .categoryId(categoryId)
                .startTime(startTime)
                .guestCount(1)
                .customerName("Nguyen Van A")
                .customerPhone("0901234567")
                .intentKey(intentKey)
                .build();

        AppException ex = assertThrows(AppException.class, () -> bookingService.createBooking(differentReq));
        assertEquals("CONFLICT", ex.getErrorCode().name());
        assertTrue(ex.getMessage().contains("Idempotency-Key"));
    }
}
