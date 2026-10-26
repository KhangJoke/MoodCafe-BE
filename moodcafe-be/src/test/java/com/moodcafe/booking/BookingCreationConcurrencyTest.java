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
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingCreationConcurrencyTest {

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
                .appQuota(1) // Only 1 slot available!
                .minSpendAmount(50000L)
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
    }

    @Test
    @DisplayName("Two requests competing for last slot: First succeeds, second receives NO_TABLE_AVAILABLE")
    void testLastSlotConcurrency() {
        when(currentUserService.getCurrentUser()).thenReturn(customer);
        when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
        when(configRepository.findByStoreStoreId(storeId)).thenReturn(Optional.of(config));
        when(tableCategoryRepository.findByIdWithLock(categoryId)).thenReturn(Optional.of(category));

        ReservationFinancialSnapshot snapshot = ReservationFinancialSnapshot.builder()
                .grossAmount(new BigDecimal("50000"))
                .voucherDiscount(BigDecimal.ZERO)
                .customerPaidAmount(new BigDecimal("50000"))
                .commissionRate(new BigDecimal("0.1500"))
                .commissionAmount(new BigDecimal("7500"))
                .merchantNetAmount(new BigDecimal("42500"))
                .build();
        when(reservationPricingService.quote(any())).thenReturn(snapshot);

        when(paymentService.generateOrderCode()).thenReturn(2000000001L);

        // Atomic occupied counter simulates DB lock serialization
        AtomicInteger occupiedCounter = new AtomicInteger(0);
        when(bookingRepository.countOverlappingBookings(eq(categoryId), any(), any(), any()))
                .thenAnswer(inv -> (long) occupiedCounter.get());

        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> {
            Booking b = inv.getArgument(0);
            b.setBookingId(UUID.randomUUID());
            occupiedCounter.incrementAndGet(); // Slot is now occupied!
            return b;
        });

        CreateBookingRequest req1 = CreateBookingRequest.builder()
                .storeId(storeId)
                .categoryId(categoryId)
                .startTime(Instant.now().plusSeconds(3600))
                .guestCount(2)
                .customerName("Khach 1")
                .customerPhone("0901111111")
                .build();

        CreateBookingRequest req2 = CreateBookingRequest.builder()
                .storeId(storeId)
                .categoryId(categoryId)
                .startTime(Instant.now().plusSeconds(3600))
                .guestCount(2)
                .customerName("Khach 2")
                .customerPhone("0902222222")
                .build();

        // 1st request succeeds
        BookingResponse resp1 = bookingService.createBooking(req1);
        assertNotNull(resp1);
        assertEquals(BookingStatus.PENDING_PAYMENT, resp1.getStatus());

        // 2nd request fails because quota is 1 and occupied is now 1
        AppException ex = assertThrows(AppException.class, () -> bookingService.createBooking(req2));
        assertEquals("NO_TABLE_AVAILABLE", ex.getErrorCode().name());
    }
}
