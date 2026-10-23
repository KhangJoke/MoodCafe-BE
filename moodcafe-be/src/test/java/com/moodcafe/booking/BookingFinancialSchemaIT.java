package com.moodcafe.booking;

import com.moodcafe.auth.abstraction.service.CurrentUserService;
import com.moodcafe.auth.entity.User;
import com.moodcafe.booking.abstraction.repository.BookingQuoteRepository;
import com.moodcafe.booking.abstraction.repository.BookingRepository;
import com.moodcafe.booking.abstraction.repository.StoreBookingConfigRepository;
import com.moodcafe.booking.abstraction.repository.TableCategoryRepository;
import com.moodcafe.booking.dto.request.BookingQuoteRequest;
import com.moodcafe.booking.dto.request.CreateBookingRequest;
import com.moodcafe.booking.dto.response.BookingCustomerDetailResponse;
import com.moodcafe.booking.dto.response.BookingQuoteResponse;
import com.moodcafe.booking.dto.response.BookingResponse;
import com.moodcafe.booking.dto.response.BookingStaffDetailResponse;
import com.moodcafe.booking.entity.Booking;
import com.moodcafe.booking.entity.BookingQuote;
import com.moodcafe.booking.entity.StoreBookingConfig;
import com.moodcafe.booking.entity.TableCategory;
import com.moodcafe.booking.entity.enums.BookingStatus;
import com.moodcafe.booking.mapper.BookingMapper;
import com.moodcafe.booking.service.BookingFinanceService;
import com.moodcafe.booking.service.BookingServiceImpl;
import com.moodcafe.finance.dto.ReservationFinancialSnapshot;
import com.moodcafe.finance.service.ReservationPricingService;
import com.moodcafe.notification.abstraction.service.NotificationDispatcherService;
import com.moodcafe.payment.abstraction.service.PayOSService;
import com.moodcafe.shared.exceptions.AppException;
import com.moodcafe.store.abstraction.repository.StoreImageRepository;
import com.moodcafe.store.abstraction.repository.StoreRepository;
import com.moodcafe.store.abstraction.service.StoreStaffService;
import com.moodcafe.store.entity.Store;
import com.moodcafe.store.entity.enums.StoreStatus;
import com.moodcafe.voucher.abstraction.repository.CustomerVoucherRepository;
import com.moodcafe.voucher.abstraction.repository.VoucherPointAllocationRepository;
import com.moodcafe.voucher.entity.CustomerVoucher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
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
class BookingFinancialSchemaIT {

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
    private BookingQuoteRepository bookingQuoteRepository;
    @Mock
    private CustomerVoucherRepository customerVoucherRepository;
    @Mock
    private VoucherPointAllocationRepository voucherPointAllocationRepository;
    @Mock
    private ReservationPricingService reservationPricingService;
    @Mock
    private com.moodcafe.payment.service.OrderCodeGeneratorService orderCodeGeneratorService;
    @Mock
    private com.moodcafe.payment.service.PaymentAttemptService paymentAttemptService;

    private BookingFinanceService bookingFinanceService;
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

        bookingFinanceService = new BookingFinanceService(
                reservationPricingService,
                customerVoucherRepository,
                voucherPointAllocationRepository
        );

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

    @Nested
    @DisplayName("Financial Snapshot & Privacy Invariants")
    class FinancialSnapshotTests {

        @Test
        @DisplayName("Snapshot 100k / discount 10k / commission 15% -> Customer 90k, Owner 85k")
        void testSnapshotCalculation() {
            ReservationFinancialSnapshot expectedSnapshot = ReservationFinancialSnapshot.builder()
                    .grossAmount(new BigDecimal("100000"))
                    .voucherDiscount(new BigDecimal("10000"))
                    .customerPaidAmount(new BigDecimal("90000"))
                    .commissionRate(new BigDecimal("0.1500"))
                    .commissionAmount(new BigDecimal("15000"))
                    .merchantNetAmount(new BigDecimal("85000"))
                    .platformNetRevenue(new BigDecimal("5000"))
                    .voucherRefundFundedAmount(BigDecimal.ZERO)
                    .voucherRewardFundedAmount(new BigDecimal("10000"))
                    .build();

            when(reservationPricingService.quote(any())).thenReturn(expectedSnapshot);

            CustomerVoucher voucher = CustomerVoucher.builder()
                    .voucherId(UUID.randomUUID())
                    .userId(userId)
                    .faceValueVnd(new BigDecimal("10000"))
                    .status("AVAILABLE")
                    .expiresAt(Instant.now().plusSeconds(86400))
                    .build();

            ReservationFinancialSnapshot snapshot = bookingFinanceService.resolveFinancialSnapshot(store, category, voucher);

            assertEquals(new BigDecimal("100000"), snapshot.getGrossAmount());
            assertEquals(new BigDecimal("10000"), snapshot.getVoucherDiscount());
            assertEquals(new BigDecimal("90000"), snapshot.getCustomerPaidAmount());
            assertEquals(new BigDecimal("85000"), snapshot.getMerchantNetAmount());
            assertEquals(new BigDecimal("15000"), snapshot.getCommissionAmount());
        }

        @Test
        @DisplayName("Customer JSON does NOT leak commission or merchant net amount")
        void testCustomerDetailPrivacy() {
            Booking booking = Booking.builder()
                    .bookingId(UUID.randomUUID())
                    .bookingCode("MC123456")
                    .store(store)
                    .tableCategory(category)
                    .user(customer)
                    .customerName("Nguyen Van A")
                    .customerPhone("0901234567")
                    .customerEmail("a@test.com")
                    .startTime(Instant.now().plusSeconds(3600))
                    .endTime(Instant.now().plusSeconds(7200))
                    .status(BookingStatus.PAID_CONFIRMED)
                    .grossAmount(new BigDecimal("100000"))
                    .voucherDiscount(new BigDecimal("10000"))
                    .customerPaidAmount(new BigDecimal("90000"))
                    .commissionAmount(new BigDecimal("15000"))
                    .merchantNetAmount(new BigDecimal("85000"))
                    .actualPaidAmount(new BigDecimal("90000"))
                    .build();

            when(currentUserService.getCurrentUser()).thenReturn(customer);
            when(bookingRepository.findById(booking.getBookingId())).thenReturn(Optional.of(booking));
            org.mockito.Mockito.lenient().when(configRepository.findByStoreStoreId(storeId)).thenReturn(Optional.of(config));

            BookingCustomerDetailResponse customerDetail = bookingService.getCustomerBookingDetail(booking.getBookingId());

            assertNotNull(customerDetail);
            assertEquals(new BigDecimal("100000"), customerDetail.getGrossAmount());
            assertEquals(new BigDecimal("10000"), customerDetail.getVoucherDiscount());
            assertEquals(new BigDecimal("90000"), customerDetail.getCustomerPaidAmount());
            // Verify fields: BookingCustomerDetailResponse doesn't have getCommissionAmount or getMerchantNetAmount getters
            assertFalse(hasMethod(customerDetail.getClass(), "getCommissionAmount"));
            assertFalse(hasMethod(customerDetail.getClass(), "getMerchantNetAmount"));
        }

        @Test
        @DisplayName("Staff Detail JSON does NOT leak commission, net, or voucher discount")
        void testStaffDetailPrivacy() {
            Booking booking = Booking.builder()
                    .bookingId(UUID.randomUUID())
                    .bookingCode("MC123456")
                    .store(store)
                    .tableCategory(category)
                    .user(customer)
                    .customerName("Nguyen Van A")
                    .customerPhone("0901234567")
                    .customerEmail("a@test.com")
                    .guestCount(2)
                    .startTime(Instant.now().plusSeconds(3600))
                    .endTime(Instant.now().plusSeconds(7200))
                    .status(BookingStatus.PAID_CONFIRMED)
                    .grossAmount(new BigDecimal("100000"))
                    .voucherDiscount(new BigDecimal("10000"))
                    .customerPaidAmount(new BigDecimal("90000"))
                    .commissionAmount(new BigDecimal("15000"))
                    .merchantNetAmount(new BigDecimal("85000"))
                    .build();

            when(currentUserService.getCurrentUser()).thenReturn(customer);
            when(bookingRepository.findById(booking.getBookingId())).thenReturn(Optional.of(booking));
            org.mockito.Mockito.lenient().when(configRepository.findByStoreStoreId(storeId)).thenReturn(Optional.of(config));

            BookingStaffDetailResponse staffDetail = bookingService.getStaffBookingDetail(storeId, booking.getBookingId());

            assertNotNull(staffDetail);
            assertEquals(new BigDecimal("100000"), staffDetail.getGrossAmount());
            assertFalse(hasMethod(staffDetail.getClass(), "getCommissionAmount"));
            assertFalse(hasMethod(staffDetail.getClass(), "getMerchantNetAmount"));
            assertFalse(hasMethod(staffDetail.getClass(), "getVoucherDiscount"));
            assertFalse(hasMethod(staffDetail.getClass(), "getBankAccount"));
        }
    }

    @Nested
    @DisplayName("Validation & Guard Rules")
    class ValidationTests {

        @Test
        @DisplayName("GuestCount exceeding category maxCapacity throws BAD_REQUEST")
        void testGuestCountExceedsCapacity() {
            when(currentUserService.getCurrentUser()).thenReturn(customer);
            when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
            org.mockito.Mockito.lenient().when(configRepository.findByStoreStoreId(storeId)).thenReturn(Optional.of(config));
            when(tableCategoryRepository.findByCategoryIdAndStoreStoreId(categoryId, storeId)).thenReturn(Optional.of(category));

            BookingQuoteRequest request = BookingQuoteRequest.builder()
                    .storeId(storeId)
                    .categoryId(categoryId)
                    .startTime(Instant.now().plusSeconds(7200))
                    .guestCount(5) // Category maxCapacity is 2
                    .build();

            AppException ex = assertThrows(AppException.class, () -> bookingService.getBookingQuote(request));
            assertTrue(ex.getMessage().contains("vượt quá sức chứa tối đa"));
        }

        @Test
        @DisplayName("Quote with expired voucher throws BAD_REQUEST")
        void testExpiredVoucherInQuote() {
            when(currentUserService.getCurrentUser()).thenReturn(customer);
            when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
            org.mockito.Mockito.lenient().when(configRepository.findByStoreStoreId(storeId)).thenReturn(Optional.of(config));
            when(tableCategoryRepository.findByCategoryIdAndStoreStoreId(categoryId, storeId)).thenReturn(Optional.of(category));

            UUID voucherId = UUID.randomUUID();
            CustomerVoucher expiredVoucher = CustomerVoucher.builder()
                    .voucherId(voucherId)
                    .userId(userId)
                    .status("AVAILABLE")
                    .expiresAt(Instant.now().minusSeconds(3600)) // Expired 1 hour ago
                    .build();

            when(customerVoucherRepository.findById(voucherId)).thenReturn(Optional.of(expiredVoucher));

            BookingQuoteRequest request = BookingQuoteRequest.builder()
                    .storeId(storeId)
                    .categoryId(categoryId)
                    .startTime(Instant.now().plusSeconds(7200))
                    .guestCount(2)
                    .voucherId(voucherId)
                    .build();

            AppException ex = assertThrows(AppException.class, () -> bookingService.getBookingQuote(request));
            assertTrue(ex.getMessage().contains("đã hết hạn"));
        }

        @Test
        @DisplayName("Create booking with altered selection vs quote throws 409 QUOTE_CHANGED")
        void testQuoteChangedConflict() {
            when(currentUserService.getCurrentUser()).thenReturn(customer);
            when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
            org.mockito.Mockito.lenient().when(configRepository.findByStoreStoreId(storeId)).thenReturn(Optional.of(config));
            when(tableCategoryRepository.findByIdWithLock(categoryId)).thenReturn(Optional.of(category));

            UUID quoteId = UUID.randomUUID();
            Instant startTime = Instant.now().plusSeconds(7200);

            // Quote had guestCount = 2
            String originalFingerprint = bookingFinanceService.calculateFingerprint(storeId, categoryId, startTime, 2, null);
            BookingQuote quote = BookingQuote.builder()
                    .quoteId(quoteId)
                    .selectionFingerprint(originalFingerprint)
                    .expiresAt(Instant.now().plusSeconds(900))
                    .build();

            when(bookingQuoteRepository.findByQuoteIdAndUserUserId(quoteId, userId)).thenReturn(Optional.of(quote));

            // Create request with guestCount = 1 -> fingerprint mismatch
            CreateBookingRequest createRequest = CreateBookingRequest.builder()
                    .storeId(storeId)
                    .categoryId(categoryId)
                    .startTime(startTime)
                    .guestCount(1)
                    .customerName("Nguyen Van A")
                    .customerPhone("0901234567")
                    .quoteId(quoteId)
                    .build();

            AppException ex = assertThrows(AppException.class, () -> bookingService.createBooking(createRequest));
            assertTrue(ex.getMessage().contains("QUOTE_CHANGED"));
        }
    }

    private boolean hasMethod(Class<?> clazz, String methodName) {
        try {
            clazz.getMethod(methodName);
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }
}
