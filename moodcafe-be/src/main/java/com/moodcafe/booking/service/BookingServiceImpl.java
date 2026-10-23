package com.moodcafe.booking.service;

import com.moodcafe.auth.abstraction.service.CurrentUserService;
import com.moodcafe.auth.entity.User;
import com.moodcafe.booking.abstraction.repository.BookingQuoteRepository;
import com.moodcafe.booking.abstraction.repository.BookingRepository;
import com.moodcafe.loyalty.service.LoyaltyRewardService;
import com.moodcafe.wallet.dto.RefundPointCommand;
import com.moodcafe.wallet.service.CustomerWalletService;
import com.moodcafe.wallet.service.MerchantWalletService;
import com.moodcafe.booking.abstraction.repository.StoreBookingConfigRepository;
import com.moodcafe.booking.abstraction.repository.TableCategoryRepository;
import com.moodcafe.booking.abstraction.service.BookingService;
import com.moodcafe.booking.dto.request.BookingQuoteRequest;
import com.moodcafe.booking.dto.request.CancelBookingRequest;
import com.moodcafe.booking.dto.request.CreateBookingRequest;
import com.moodcafe.booking.dto.request.DeskCheckInRequest;
import com.moodcafe.booking.dto.response.BookingCustomerDetailResponse;
import com.moodcafe.booking.dto.response.BookingDeskSummaryResponse;
import com.moodcafe.booking.dto.response.BookingQuoteResponse;
import com.moodcafe.booking.dto.response.BookingResponse;
import com.moodcafe.booking.dto.response.BookingStaffDetailResponse;
import com.moodcafe.booking.dto.response.CategoryScheduleDTO;
import com.moodcafe.booking.dto.response.RefundOutcomeResponse;
import com.moodcafe.booking.dto.response.SlotAvailabilityResponse;
import com.moodcafe.booking.dto.response.StoreDailyScheduleResponse;
import com.moodcafe.booking.dto.response.TimeSlotDTO;
import com.moodcafe.booking.entity.Booking;
import com.moodcafe.booking.entity.BookingQuote;
import com.moodcafe.booking.entity.StoreBookingConfig;
import com.moodcafe.booking.entity.TableCategory;
import com.moodcafe.booking.entity.enums.BookingEventType;
import com.moodcafe.booking.entity.enums.BookingStatus;
import com.moodcafe.booking.entity.enums.SlotStatus;
import com.moodcafe.booking.mapper.BookingMapper;
import com.moodcafe.finance.dto.ReservationFinancialSnapshot;
import com.moodcafe.notification.abstraction.service.NotificationDispatcherService;
import com.moodcafe.notification.entity.enums.NotificationType;
import com.moodcafe.payment.abstraction.service.PayOSService;
import com.moodcafe.payment.dto.response.PayOSResponse;
import com.moodcafe.payment.entity.PaymentAttempt;
import com.moodcafe.payment.service.OrderCodeGeneratorService;
import com.moodcafe.payment.service.PaymentAttemptService;
import com.moodcafe.shared.error.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moodcafe.shared.exceptions.AppException;
import com.moodcafe.shared.response.PageResponse;
import com.moodcafe.store.abstraction.repository.StoreImageRepository;
import com.moodcafe.store.abstraction.repository.StoreRepository;
import com.moodcafe.store.abstraction.service.StoreStaffService;
import com.moodcafe.store.entity.Store;
import com.moodcafe.store.entity.StoreImage;
import com.moodcafe.store.entity.enums.StoreStatus;
import com.moodcafe.voucher.abstraction.repository.CustomerVoucherRepository;
import com.moodcafe.voucher.entity.CustomerVoucher;
import com.moodcafe.reservation.service.ReservationQrVerificationService;
import com.moodcafe.voucher.service.VoucherLifecycleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import java.util.function.Supplier;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
public class BookingServiceImpl implements BookingService {

    private static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final Set<BookingStatus> OCCUPIED_STATUSES = EnumSet.of(
            BookingStatus.PENDING_PAYMENT,
            BookingStatus.PAID_CONFIRMED,
            BookingStatus.CHECKED_IN
    );

    private final BookingRepository bookingRepository;
    private final TableCategoryRepository tableCategoryRepository;
    private final StoreBookingConfigRepository configRepository;
    private final StoreRepository storeRepository;
    private final StoreStaffService storeStaffService;
    private final CurrentUserService currentUserService;
    private final PayOSService payOSService;
    private final BookingMapper bookingMapper;
    private final StoreImageRepository storeImageRepository;
    private final NotificationDispatcherService notificationDispatcherService;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final BookingFinanceService bookingFinanceService;
    private final BookingQuoteRepository bookingQuoteRepository;
    private final CustomerVoucherRepository customerVoucherRepository;
    private final OrderCodeGeneratorService orderCodeGeneratorService;
    private final PaymentAttemptService paymentAttemptService;
    private final MerchantWalletService merchantWalletService;
    private final CustomerWalletService customerWalletService;
    private final LoyaltyRewardService loyaltyRewardService;
    private final ReservationQrVerificationService qrVerificationService;
    private final VoucherLifecycleService voucherLifecycleService;
    private final PlatformTransactionManager transactionManager;

    @Autowired
    public BookingServiceImpl(
            BookingRepository bookingRepository,
            TableCategoryRepository tableCategoryRepository,
            StoreBookingConfigRepository configRepository,
            StoreRepository storeRepository,
            StoreStaffService storeStaffService,
            CurrentUserService currentUserService,
            PayOSService payOSService,
            BookingMapper bookingMapper,
            StoreImageRepository storeImageRepository,
            NotificationDispatcherService notificationDispatcherService,
            RedisTemplate<String, String> redisTemplate,
            ObjectMapper objectMapper,
            BookingFinanceService bookingFinanceService,
            BookingQuoteRepository bookingQuoteRepository,
            CustomerVoucherRepository customerVoucherRepository,
            OrderCodeGeneratorService orderCodeGeneratorService,
            PaymentAttemptService paymentAttemptService,
            MerchantWalletService merchantWalletService,
            CustomerWalletService customerWalletService,
            LoyaltyRewardService loyaltyRewardService,
            ReservationQrVerificationService qrVerificationService,
            VoucherLifecycleService voucherLifecycleService,
            PlatformTransactionManager transactionManager
    ) {
        this.bookingRepository = bookingRepository;
        this.tableCategoryRepository = tableCategoryRepository;
        this.configRepository = configRepository;
        this.storeRepository = storeRepository;
        this.storeStaffService = storeStaffService;
        this.currentUserService = currentUserService;
        this.payOSService = payOSService;
        this.bookingMapper = bookingMapper;
        this.storeImageRepository = storeImageRepository;
        this.notificationDispatcherService = notificationDispatcherService;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.bookingFinanceService = bookingFinanceService;
        this.bookingQuoteRepository = bookingQuoteRepository;
        this.customerVoucherRepository = customerVoucherRepository;
        this.orderCodeGeneratorService = orderCodeGeneratorService;
        this.paymentAttemptService = paymentAttemptService;
        this.merchantWalletService = merchantWalletService;
        this.customerWalletService = customerWalletService;
        this.loyaltyRewardService = loyaltyRewardService;
        this.qrVerificationService = qrVerificationService;
        this.voucherLifecycleService = voucherLifecycleService;
        this.transactionManager = transactionManager;
    }

    public BookingServiceImpl(
            BookingRepository bookingRepository,
            TableCategoryRepository tableCategoryRepository,
            StoreBookingConfigRepository configRepository,
            StoreRepository storeRepository,
            StoreStaffService storeStaffService,
            CurrentUserService currentUserService,
            PayOSService payOSService,
            BookingMapper bookingMapper,
            StoreImageRepository storeImageRepository,
            NotificationDispatcherService notificationDispatcherService,
            RedisTemplate<String, String> redisTemplate,
            ObjectMapper objectMapper,
            BookingFinanceService bookingFinanceService,
            BookingQuoteRepository bookingQuoteRepository,
            CustomerVoucherRepository customerVoucherRepository,
            OrderCodeGeneratorService orderCodeGeneratorService,
            PaymentAttemptService paymentAttemptService,
            MerchantWalletService merchantWalletService,
            CustomerWalletService customerWalletService,
            LoyaltyRewardService loyaltyRewardService
    ) {
        this(bookingRepository, tableCategoryRepository, configRepository, storeRepository,
             storeStaffService, currentUserService, payOSService, bookingMapper, storeImageRepository,
             notificationDispatcherService, redisTemplate, objectMapper, bookingFinanceService,
             bookingQuoteRepository, customerVoucherRepository, orderCodeGeneratorService,
             paymentAttemptService, merchantWalletService, customerWalletService,
             loyaltyRewardService, null, null, null);
    }

    public BookingServiceImpl(
            BookingRepository bookingRepository,
            TableCategoryRepository tableCategoryRepository,
            StoreBookingConfigRepository configRepository,
            StoreRepository storeRepository,
            StoreStaffService storeStaffService,
            CurrentUserService currentUserService,
            PayOSService payOSService,
            BookingMapper bookingMapper,
            StoreImageRepository storeImageRepository,
            NotificationDispatcherService notificationDispatcherService,
            RedisTemplate<String, String> redisTemplate,
            ObjectMapper objectMapper,
            BookingFinanceService bookingFinanceService,
            BookingQuoteRepository bookingQuoteRepository,
            CustomerVoucherRepository customerVoucherRepository,
            OrderCodeGeneratorService orderCodeGeneratorService,
            PaymentAttemptService paymentAttemptService
    ) {
        this(bookingRepository, tableCategoryRepository, configRepository, storeRepository,
             storeStaffService, currentUserService, payOSService, bookingMapper, storeImageRepository,
             notificationDispatcherService, redisTemplate, objectMapper, bookingFinanceService,
             bookingQuoteRepository, customerVoucherRepository, orderCodeGeneratorService,
             paymentAttemptService, null, null, null, null, null, null);
    }

    @Override
    @Transactional(readOnly = true)
    public StoreDailyScheduleResponse getStoreDailySchedule(UUID storeId, LocalDate date) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new AppException(ErrorCode.STORE_NOT_FOUND));

        StoreBookingConfig config = getOrCreateConfig(store);
        boolean isBookingEnabled = Boolean.TRUE.equals(config.getIsBookingEnabled());

        LocalDate targetDate = date != null ? date : LocalDate.now(VN_ZONE);
        LocalTime openingTime = store.getOpeningTime() != null ? store.getOpeningTime() : LocalTime.of(7, 0);
        LocalTime closingTime = store.getClosingTime() != null ? store.getClosingTime() : LocalTime.of(22, 0);

        List<TableCategory> categories = tableCategoryRepository.findAllByStoreStoreIdAndIsActiveTrue(storeId);
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

        ZonedDateTime dayStart = targetDate.atTime(0, 0).atZone(VN_ZONE);
        ZonedDateTime dayEnd = targetDate.atTime(23, 59, 59).atZone(VN_ZONE);
        List<Booking> dayBookings = bookingRepository.findOccupyingBookingsForDay(
                storeId,
                OCCUPIED_STATUSES,
                dayStart.toInstant(),
                dayEnd.toInstant()
        );

        Instant now = Instant.now();
        Instant minAdvanceInstant = now.plus(Duration.ofMinutes(config.getMinAdvanceMins()));

        List<CategoryScheduleDTO> categoryDTOs = new ArrayList<>();

        for (TableCategory cat : categories) {
            List<Booking> catBookings = dayBookings.stream()
                    .filter(b -> b.getTableCategory().getCategoryId().equals(cat.getCategoryId()))
                    .toList();

            List<TimeSlotDTO> timeSlots = new ArrayList<>();
            LocalTime currentSlotTime = openingTime;

            while (!currentSlotTime.isAfter(closingTime.minusMinutes(30))) {
                Instant slotStart = targetDate.atTime(currentSlotTime).atZone(VN_ZONE).toInstant();
                String timeStr = currentSlotTime.format(timeFormatter);

                boolean isPast = slotStart.isBefore(minAdvanceInstant);

                if (isPast) {
                    timeSlots.add(TimeSlotDTO.builder()
                            .time(timeStr)
                            .startTime(slotStart)
                            .endTime(calculateEndTime(slotStart, config, store, cat))
                            .availableCount(0)
                            .occupiedCount(cat.getAppQuota())
                            .status(SlotStatus.PAST)
                            .availableDurationMins(0)
                            .build());
                } else if (!isBookingEnabled) {
                    timeSlots.add(TimeSlotDTO.builder()
                            .time(timeStr)
                            .startTime(slotStart)
                            .endTime(calculateEndTime(slotStart, config, store, cat))
                            .availableCount(0)
                            .occupiedCount(cat.getAppQuota())
                            .status(SlotStatus.FULL)
                            .availableDurationMins(0)
                            .note("Quán đang tạm tắt tính năng đặt bàn")
                            .build());
                } else if (Boolean.TRUE.equals(config.getIsTimeLimitEnabled())) {
                    Instant normalEnd = slotStart.plus(Duration.ofMinutes(config.getSittingDurationMins()));
                    Instant closingInstant = targetDate.atTime(closingTime).atZone(VN_ZONE).toInstant();
                    Instant effectiveEnd = normalEnd.isAfter(closingInstant) ? closingInstant : normalEnd;
                    Instant bufferEnd = effectiveEnd.plus(Duration.ofMinutes(config.getCleanupBufferMins()));

                    long occupied = catBookings.stream()
                            .filter(b -> b.getStartTime().isBefore(bufferEnd) && b.getEndTime().isAfter(slotStart))
                            .count();

                    int available = Math.max(0, cat.getAppQuota() - (int) occupied);
                    int availableDuration = (int) ChronoUnit.MINUTES.between(slotStart, effectiveEnd);

                    if (available <= 0) {
                        timeSlots.add(TimeSlotDTO.builder()
                                .time(timeStr)
                                .startTime(slotStart)
                                .endTime(effectiveEnd)
                                .availableCount(0)
                                .occupiedCount(cat.getAppQuota())
                                .status(SlotStatus.FULL)
                                .availableDurationMins(0)
                                .build());
                    } else {
                        SlotStatus status = (available == 1) ? SlotStatus.ALMOST_FULL : SlotStatus.AVAILABLE;
                        timeSlots.add(TimeSlotDTO.builder()
                                .time(timeStr)
                                .startTime(slotStart)
                                .endTime(effectiveEnd)
                                .availableCount(available)
                                .occupiedCount((int) occupied)
                                .status(status)
                                .availableDurationMins(availableDuration)
                                .build());
                    }
                } else {
                    Instant closingInstant = targetDate.atTime(closingTime).atZone(VN_ZONE).toInstant();
                    long occupied = catBookings.stream()
                            .filter(b -> b.getStartTime().isBefore(closingInstant) && b.getEndTime().isAfter(slotStart))
                            .count();

                    int available = Math.max(0, cat.getAppQuota() - (int) occupied);
                    int remainingMins = Math.max(0, (int) ChronoUnit.MINUTES.between(slotStart, closingInstant));

                    if (available <= 0) {
                        timeSlots.add(TimeSlotDTO.builder()
                                .time(timeStr)
                                .startTime(slotStart)
                                .endTime(closingInstant)
                                .availableCount(0)
                                .occupiedCount(cat.getAppQuota())
                                .status(SlotStatus.FULL)
                                .availableDurationMins(0)
                                .note("Đã kín chỗ đến giờ đóng cửa")
                                .build());
                    } else {
                        SlotStatus status = (available == 1) ? SlotStatus.ALMOST_FULL : SlotStatus.AVAILABLE;
                        timeSlots.add(TimeSlotDTO.builder()
                                .time(timeStr)
                                .startTime(slotStart)
                                .endTime(closingInstant)
                                .availableCount(available)
                                .occupiedCount((int) occupied)
                                .status(status)
                                .availableDurationMins(remainingMins)
                                .build());
                    }
                }

                currentSlotTime = currentSlotTime.plusMinutes(30);
            }

            categoryDTOs.add(CategoryScheduleDTO.builder()
                    .categoryId(cat.getCategoryId())
                    .categoryName(cat.getName())
                    .maxCapacity(cat.getMaxCapacity())
                    .minSpendAmount(cat.getMinSpendAmount())
                    .totalQuota(cat.getAppQuota())
                    .imageUrl(cat.getImageUrl())
                    .description(cat.getDescription())
                    .timeSlots(timeSlots)
                    .build());
        }

        return StoreDailyScheduleResponse.builder()
                .storeId(store.getStoreId())
                .storeName(store.getName())
                .date(targetDate)
                .isBookingEnabled(isBookingEnabled)
                .isTimeLimitEnabled(config.getIsTimeLimitEnabled())
                .sittingDurationMins(config.getSittingDurationMins())
                .cleanupBufferMins(config.getCleanupBufferMins())
                .minAdvanceMins(config.getMinAdvanceMins())
                .gracePeriodMins(config.getGracePeriodMins())
                .freeCancelHours(config.getFreeCancelHours())
                .openingTime(openingTime.format(timeFormatter))
                .closingTime(closingTime.format(timeFormatter))
                .categories(categoryDTOs)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public SlotAvailabilityResponse checkSlotAvailability(UUID storeId, UUID categoryId, Instant startTime) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new AppException(ErrorCode.STORE_NOT_FOUND));

        TableCategory category = tableCategoryRepository.findByCategoryIdAndStoreStoreId(categoryId, storeId)
                .orElseThrow(() -> new AppException(ErrorCode.TABLE_CATEGORY_NOT_FOUND));

        StoreBookingConfig config = getOrCreateConfig(store);
        if (!Boolean.TRUE.equals(config.getIsBookingEnabled())) {
            throw new AppException(ErrorCode.BOOKING_DISABLED_BY_STORE);
        }

        Instant now = Instant.now();
        Instant minAdvance = now.plus(Duration.ofMinutes(config.getMinAdvanceMins()));
        if (startTime.isBefore(minAdvance)) {
            throw new AppException(ErrorCode.BOOKING_ADVANCE_TIME_INVALID);
        }

        Instant endTime = calculateEndTime(startTime, config, store, category);

        long occupied = bookingRepository.countOverlappingBookings(
                categoryId,
                OCCUPIED_STATUSES,
                startTime,
                endTime
        );

        int totalQuota = category.getAppQuota();
        int available = Math.max(0, totalQuota - (int) occupied);

        return SlotAvailabilityResponse.builder()
                .categoryId(category.getCategoryId())
                .categoryName(category.getName())
                .startTime(startTime)
                .endTime(endTime)
                .totalQuota(totalQuota)
                .occupiedCount((int) occupied)
                .availableCount(available)
                .isAvailable(available > 0)
                .minSpendAmount(category.getMinSpendAmount())
                .build();
    }

    @Override
    @Transactional
    public BookingQuoteResponse getBookingQuote(BookingQuoteRequest request) {
        User currentUser = currentUserService.getCurrentUser();

        Store store = storeRepository.findById(request.getStoreId())
                .orElseThrow(() -> new AppException(ErrorCode.STORE_NOT_FOUND));

        if (store.getStatus() == StoreStatus.SUSPENDED) {
            throw new AppException(ErrorCode.FORBIDDEN_STORE_ACCESS, "Cơ sở đang bị tạm đình chỉ hoạt động");
        }

        StoreBookingConfig config = getOrCreateConfig(store);
        if (!Boolean.TRUE.equals(config.getIsBookingEnabled())) {
            throw new AppException(ErrorCode.BOOKING_DISABLED_BY_STORE);
        }

        TableCategory category = tableCategoryRepository.findByCategoryIdAndStoreStoreId(request.getCategoryId(), request.getStoreId())
                .orElseThrow(() -> new AppException(ErrorCode.TABLE_CATEGORY_NOT_FOUND));

        if (!category.getStore().getStoreId().equals(store.getStoreId())) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Loại bàn không thuộc cơ sở đã chọn");
        }
        if (!Boolean.TRUE.equals(category.getIsActive())) {
            throw new AppException(ErrorCode.TABLE_CATEGORY_NOT_FOUND, "Loại bàn này đang tạm ngừng nhận khách");
        }

        int guestCount = request.getGuestCount() != null ? request.getGuestCount() : 2;
        if (category.getMaxCapacity() != null && guestCount > category.getMaxCapacity()) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Số lượng khách (" + guestCount + ") vượt quá sức chứa tối đa của loại bàn (" + category.getMaxCapacity() + " người)");
        }

        CustomerVoucher voucher = null;
        if (request.getVoucherId() != null) {
            voucher = customerVoucherRepository.findById(request.getVoucherId())
                    .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Voucher không tồn tại"));

            if (!voucher.getUserId().equals(currentUser.getUserId())) {
                throw new AppException(ErrorCode.FORBIDDEN_ACTION, "Voucher không thuộc quyền sở hữu của bạn");
            }
            if (!"AVAILABLE".equalsIgnoreCase(voucher.getStatus())) {
                throw new AppException(ErrorCode.BAD_REQUEST, "Voucher không ở trạng thái khả dụng");
            }
            if (voucher.getExpiresAt().isBefore(Instant.now())) {
                throw new AppException(ErrorCode.BAD_REQUEST, "Voucher đã hết hạn sử dụng");
            }
        }

        ReservationFinancialSnapshot snapshot = bookingFinanceService.resolveFinancialSnapshot(store, category, voucher);
        BookingFinanceService.Deadlines deadlines = bookingFinanceService.calculateDeadlines(store, config, request.getStartTime());
        String fingerprint = bookingFinanceService.calculateFingerprint(
                request.getStoreId(),
                request.getCategoryId(),
                request.getStartTime(),
                guestCount,
                request.getVoucherId()
        );

        Instant expiresAt = Instant.now().plus(Duration.ofMinutes(15));
        BookingQuote quote = BookingQuote.builder()
                .user(currentUser)
                .store(store)
                .tableCategory(category)
                .startTime(request.getStartTime())
                .guestCount(guestCount)
                .voucher(voucher)
                .selectionFingerprint(fingerprint)
                .pricingVersion(1)
                .grossAmount(snapshot.getGrossAmount())
                .voucherDiscount(snapshot.getVoucherDiscount())
                .customerPaidAmount(snapshot.getCustomerPaidAmount())
                .commissionRate(snapshot.getCommissionRate())
                .commissionAmount(snapshot.getCommissionAmount())
                .merchantNetAmount(snapshot.getMerchantNetAmount())
                .voucherRefundFundedAmount(snapshot.getVoucherRefundFundedAmount())
                .voucherRewardFundedAmount(snapshot.getVoucherRewardFundedAmount())
                .isEligible(true)
                .freeCancelDeadline(deadlines.freeCancelDeadline())
                .graceDeadline(deadlines.graceDeadline())
                .serviceClosingDeadline(deadlines.serviceClosingDeadline())
                .expiresAt(expiresAt)
                .build();

        quote = bookingQuoteRepository.save(quote);

        return BookingQuoteResponse.builder()
                .quoteId(quote.getQuoteId())
                .expiresAt(expiresAt)
                .selectionFingerprint(fingerprint)
                .pricingVersion(quote.getPricingVersion())
                .grossAmount(snapshot.getGrossAmount())
                .voucherDiscount(snapshot.getVoucherDiscount())
                .customerPaidAmount(snapshot.getCustomerPaidAmount())
                .currency("VND")
                .isEligible(true)
                .freeCancelDeadline(deadlines.freeCancelDeadline())
                .graceDeadline(deadlines.graceDeadline())
                .serviceClosingDeadline(deadlines.serviceClosingDeadline())
                .build();
    }

    public static String computeBookingPayloadHash(CreateBookingRequest request) {
        String raw = (request.getStoreId() != null ? request.getStoreId().toString() : "") + "|"
                + (request.getCategoryId() != null ? request.getCategoryId().toString() : "") + "|"
                + (request.getStartTime() != null ? request.getStartTime().toEpochMilli() : "") + "|"
                + (request.getGuestCount() != null ? request.getGuestCount() : 2) + "|"
                + (request.getVoucherId() != null ? request.getVoucherId().toString() : "") + "|"
                + (request.getCustomerName() != null ? request.getCustomerName().trim() : "") + "|"
                + (request.getCustomerPhone() != null ? request.getCustomerPhone().trim() : "");
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public BookingResponse createBooking(CreateBookingRequest request) {
        BookingResponse prepared;
        try {
            prepared = inTransaction(() -> prepareBooking(request));
        } catch (org.springframework.dao.DataIntegrityViolationException conflict) {
            // A competing request may have committed the same scoped intent while we prepared.
            prepared = inTransaction(() -> findExistingIntent(request));
            if (prepared == null) throw new AppException(ErrorCode.CONFLICT, "Báo giá hoặc yêu cầu đã được sử dụng");
        }
        if (prepared.getStatus() == BookingStatus.PENDING_PAYMENT && prepared.getCheckoutUrl() == null) {
            return resumePayment(prepared);
        }
        return prepared;
    }

    private <T> T inTransaction(Supplier<T> action) {
        if (transactionManager == null) return action.get(); // compatibility for existing isolated unit fixtures
        return new TransactionTemplate(transactionManager).execute(status -> action.get());
    }

    private BookingResponse findExistingIntent(CreateBookingRequest request) {
        if (request.getIntentKey() == null || request.getIntentKey().isBlank()) return null;
        UUID actor = currentUserService.getCurrentUser().getUserId();
        Optional<PaymentAttempt> existing = paymentAttemptService.findByActorIdAndPurposeAndIntentKey(
                actor, "BOOKING", request.getIntentKey().trim());
        if (existing.isEmpty()) return null;
        PaymentAttempt attempt = existing.get();
        if (attempt.getPayloadHash() == null || !attempt.getPayloadHash().equals(computeBookingPayloadHash(request))) {
            throw new AppException(ErrorCode.CONFLICT, "Idempotency-Key đã được sử dụng với thông tin đặt bàn khác");
        }
        Booking booking = bookingRepository.findById(UUID.fromString(attempt.getBusinessId()))
                .orElseThrow(() -> new AppException(ErrorCode.CONFLICT, "Yêu cầu đang được phục hồi"));
        if (!booking.getUser().getUserId().equals(actor)) throw new AppException(ErrorCode.FORBIDDEN_ACTION);
        return enrichBookingResponse(booking, getOrCreateConfig(booking.getStore()));
    }

    private BookingResponse prepareBooking(CreateBookingRequest request) {
        User currentUser = currentUserService.getCurrentUser();

        String effectiveIntentKey = (request.getIntentKey() != null && !request.getIntentKey().isBlank())
                ? request.getIntentKey().trim()
                : null;
        String incomingHash = computeBookingPayloadHash(request);

        BookingResponse replay = findExistingIntent(request);
        if (replay != null) return replay;

        Store store = storeRepository.findById(request.getStoreId())
                .orElseThrow(() -> new AppException(ErrorCode.STORE_NOT_FOUND));

        if (store.getStatus() == StoreStatus.SUSPENDED) {
            throw new AppException(ErrorCode.FORBIDDEN_STORE_ACCESS, "Cơ sở đang bị tạm đình chỉ hoạt động");
        }

        StoreBookingConfig config = getOrCreateConfig(store);
        if (!Boolean.TRUE.equals(config.getIsBookingEnabled())) {
            throw new AppException(ErrorCode.BOOKING_DISABLED_BY_STORE);
        }

        Instant now = Instant.now();
        Instant minAdvance = now.plus(Duration.ofMinutes(config.getMinAdvanceMins()));
        if (request.getStartTime().isBefore(minAdvance)) {
            throw new AppException(ErrorCode.BOOKING_ADVANCE_TIME_INVALID);
        }

        // Khóa bi quan chống Race Condition khi nhiều khách cùng đặt bàn cuối
        TableCategory category = tableCategoryRepository.findByIdWithLock(request.getCategoryId())
                .orElseThrow(() -> new AppException(ErrorCode.TABLE_CATEGORY_NOT_FOUND));

        // Recheck after obtaining the category lock: a concurrent identical draft may
        // have committed while this request waited for the last table.
        replay = findExistingIntent(request);
        if (replay != null) return replay;
        if (!category.getStore().getStoreId().equals(store.getStoreId())) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Loại bàn không thuộc cơ sở đã chọn");
        }
        if (!Boolean.TRUE.equals(category.getIsActive())) {
            throw new AppException(ErrorCode.TABLE_CATEGORY_NOT_FOUND, "Loại bàn này đang tạm ngừng nhận khách");
        }

        int guestCount = request.getGuestCount() != null ? request.getGuestCount() : 2;
        if (category.getMaxCapacity() != null && guestCount > category.getMaxCapacity()) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Số lượng khách (" + guestCount + ") vượt quá sức chứa tối đa của loại bàn (" + category.getMaxCapacity() + " người)");
        }

        Instant endTime = calculateEndTime(request.getStartTime(), config, store, category);

        long occupied = bookingRepository.countOverlappingBookings(
                category.getCategoryId(),
                OCCUPIED_STATUSES,
                request.getStartTime(),
                endTime
        );

        if (occupied >= category.getAppQuota()) {
            throw new AppException(ErrorCode.NO_TABLE_AVAILABLE);
        }

        // Validate Quote - MANDATORY for all bookings
        if (request.getQuoteId() == null) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Yêu cầu đặt bàn bắt buộc phải có quoteId hợp lệ từ hệ thống");
        }
        BookingQuote quote = bookingQuoteRepository.findByQuoteIdAndUserUserId(request.getQuoteId(), currentUser.getUserId())
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Báo giá không tồn tại hoặc đã hết hạn"));

        if (bookingRepository.existsByQuoteQuoteId(quote.getQuoteId())) {
            throw new AppException(ErrorCode.CONFLICT, "Báo giá đã được dùng cho một đơn đặt bàn khác");
        }
        if (quote.getExpiresAt().isBefore(now)) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Báo giá đã hết hạn, vui lòng lấy báo giá mới");
        }

        String currentFingerprint = bookingFinanceService.calculateFingerprint(
                request.getStoreId(),
                request.getCategoryId(),
                request.getStartTime(),
                guestCount,
                request.getVoucherId()
        );
        if (!quote.getSelectionFingerprint().equals(currentFingerprint)) {
            throw new AppException(ErrorCode.CONFLICT, "QUOTE_CHANGED: Thông tin lựa chọn đã thay đổi so với báo giá");
        }

        // Validate Voucher if provided
        CustomerVoucher voucher = null;
        if (request.getVoucherId() != null) {
            voucher = customerVoucherRepository.findByIdWithLock(request.getVoucherId())
                    .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Voucher không tồn tại"));

            if (!voucher.getUserId().equals(currentUser.getUserId())) {
                throw new AppException(ErrorCode.FORBIDDEN_ACTION, "Voucher không thuộc quyền sở hữu của bạn");
            }
            if (!"AVAILABLE".equalsIgnoreCase(voucher.getStatus())) {
                throw new AppException(ErrorCode.BAD_REQUEST, "Voucher không ở trạng thái khả dụng");
            }
            if (voucher.getExpiresAt().isBefore(now)) {
                throw new AppException(ErrorCode.BAD_REQUEST, "Voucher đã hết hạn sử dụng");
            }
        }

        ReservationFinancialSnapshot snapshot = ReservationFinancialSnapshot.builder()
                .grossAmount(quote.getGrossAmount())
                .voucherDiscount(quote.getVoucherDiscount())
                .customerPaidAmount(quote.getCustomerPaidAmount())
                .commissionRate(quote.getCommissionRate())
                .commissionAmount(quote.getCommissionAmount())
                .merchantNetAmount(quote.getMerchantNetAmount())
                .voucherRefundFundedAmount(quote.getVoucherRefundFundedAmount())
                .voucherRewardFundedAmount(quote.getVoucherRewardFundedAmount())
                .build();
        BookingFinanceService.Deadlines deadlines = bookingFinanceService.calculateDeadlines(store, config, request.getStartTime());

        String bookingCode = "MC" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        String qrToken = UUID.randomUUID().toString();
        long orderCode = orderCodeGeneratorService.nextOrderCode();

        boolean requiresDeposit = snapshot.getCustomerPaidAmount().compareTo(BigDecimal.ZERO) > 0;

        Booking booking = Booking.builder()
                .bookingCode(bookingCode)
                .store(store)
                .tableCategory(category)
                .user(currentUser)
                .customerName(request.getCustomerName().trim())
                .customerPhone(request.getCustomerPhone().trim())
                .customerEmail(request.getCustomerEmail() != null ? request.getCustomerEmail().trim() : currentUser.getEmail())
                .guestCount(guestCount)
                .quote(quote)
                .startTime(request.getStartTime())
                .endTime(endTime)
                .status(requiresDeposit ? BookingStatus.PENDING_PAYMENT : BookingStatus.PAID_CONFIRMED)
                .minSpendAmount(category.getMinSpendAmount() != null ? category.getMinSpendAmount() : 0L)
                .grossAmount(snapshot.getGrossAmount())
                .voucherDiscount(snapshot.getVoucherDiscount())
                .customerPaidAmount(snapshot.getCustomerPaidAmount())
                .commissionAmount(snapshot.getCommissionAmount())
                .merchantNetAmount(snapshot.getMerchantNetAmount())
                .voucher(voucher)
                .voucherRefundFundedAmount(snapshot.getVoucherRefundFundedAmount())
                .voucherRewardFundedAmount(snapshot.getVoucherRewardFundedAmount())
                .commissionRateSnapshot(snapshot.getCommissionRate())
                .actualPaidAmount(BigDecimal.ZERO)
                .paidAt(requiresDeposit ? null : Instant.now())
                .paymentOrderCode(orderCode)
                .paymentExpiresAt(Instant.now().plus(Duration.ofMinutes(15)))
                .freeCancelDeadline(deadlines.freeCancelDeadline())
                .graceDeadline(deadlines.graceDeadline())
                .serviceClosingDeadline(deadlines.serviceClosingDeadline())
                .qrCodeToken(qrToken)
                .note(request.getNote() != null ? request.getNote().trim() : null)
                .refundProcessed(false)
                .build();

        booking = bookingRepository.save(booking);

        // Persist durable payment attempt
        paymentAttemptService.createAttempt(
                "BOOKING",
                booking.getBookingId().toString(),
                orderCode,
                snapshot.getCustomerPaidAmount(),
                "VND",
                effectiveIntentKey,
                currentUser.getUserId(),
                incomingHash
        );

        if (voucher != null) {
            if (requiresDeposit) {
                voucher.setStatus("LOCKED");
                voucher.setLockedBookingId(booking.getBookingId());
                voucher.setHoldUntil(Instant.now().plus(Duration.ofMinutes(15)));
            } else {
                // 100% Zero-pay voucher: mark USED immediately
                voucher.setStatus("USED");
                voucher.setLockedBookingId(booking.getBookingId());
                voucher.setUsedAt(Instant.now());
                paymentAttemptService.markStatus(orderCode, "PAID");
            }
            customerVoucherRepository.save(voucher);
        }

        if (!requiresDeposit) {
            if (booking.getMerchantNetAmount() != null && booking.getMerchantNetAmount().compareTo(BigDecimal.ZERO) > 0) {
                if (merchantWalletService != null) merchantWalletService.holdPending(
                        booking.getStore().getStoreId(),
                        booking.getMerchantNetAmount(),
                        "BOOKING",
                        booking.getBookingId().toString(),
                        "Tạm giữ doanh thu đơn cọc 100% voucher #" + booking.getBookingCode()
                );
            }
        }

        BookingResponse response = enrichBookingResponse(booking, config);

        BookingEventType eventType = requiresDeposit ? BookingEventType.CREATED : BookingEventType.PAID_CONFIRMED;
        String eventMsg = requiresDeposit
                ? "Đơn đặt bàn mới (" + booking.getBookingCode() + ") đang chờ thanh toán cọc."
                : "Đơn đặt bàn mới (" + booking.getBookingCode() + ") đã được xác nhận tự động (Zero-Pay).";
        dispatchBookingNotification(booking, eventType, eventMsg);

        return response;
    }

    @Override
    @Transactional
    public void confirmBookingPayment(Long paymentOrderCode) {
        confirmBookingPayment(paymentOrderCode, null);
    }

    @Override
    @Transactional
    public void confirmBookingPayment(Long paymentOrderCode, Long paidAmountVnd) {
        Booking booking = paymentAttemptService.findByOrderCode(paymentOrderCode)
                .filter(a -> "BOOKING".equalsIgnoreCase(a.getBusinessType()))
                .flatMap(a -> {
                    try {
                        return bookingRepository.findByIdForUpdate(UUID.fromString(a.getBusinessId()));
                    } catch (IllegalArgumentException ignored) {
                        return Optional.empty();
                    }
                })
                .or(() -> bookingRepository.findByOrderCodeForUpdate(paymentOrderCode))
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));

        if (paidAmountVnd == null || paidAmountVnd < 0) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Thiếu số tiền thực nhận đã xác minh từ cổng thanh toán");
        }
        if (booking.getStatus() == BookingStatus.PAYMENT_FAILED || booking.getStatus() == BookingStatus.CANCELLED_REFUNDED) {
            BigDecimal previous = booking.getActualPaidAmount() != null ? booking.getActualPaidAmount() : BigDecimal.ZERO;
            BigDecimal cumulative = BigDecimal.valueOf(paidAmountVnd);
            if (cumulative.compareTo(previous) > 0) {
                BigDecimal delta = cumulative.subtract(previous);
                String businessKey = previous.signum() == 0 ? "REFUND_BOOKING_" + booking.getBookingId()
                        : "REFUND_BOOKING_" + booking.getBookingId() + "_TOTAL_" + paidAmountVnd;
                customerWalletService.creditRefundPoints(RefundPointCommand.builder()
                        .userId(booking.getUser().getUserId()).points(delta.longValueExact())
                        .refundId(booking.getBookingId().toString()).businessKey(businessKey)
                        .description("Hoàn phần tiền thực nhận tăng thêm đơn " + booking.getBookingCode()).build());
                booking.setActualPaidAmount(cumulative);
                booking.setPaidAt(Instant.now());
                booking.setRefundProcessed(true);
                bookingRepository.save(booking);
            }
            paymentAttemptService.markStatus(paymentOrderCode, "PAID");
            return;
        }
        if (booking.getStatus() == BookingStatus.PAID_CONFIRMED) {
            log.info("Booking {} already confirmed as PAID_CONFIRMED", booking.getBookingCode());
            return;
        }

        if (booking.getStatus() == BookingStatus.PENDING_PAYMENT) {
            if (paidAmountVnd == null || paidAmountVnd < 0) {
                throw new AppException(ErrorCode.BAD_REQUEST, "Thiếu số tiền thực nhận đã xác minh từ cổng thanh toán");
            }
            if (paidAmountVnd != null) {
                BigDecimal actualPaid = BigDecimal.valueOf(paidAmountVnd);
                if (booking.getCustomerPaidAmount() != null && actualPaid.compareTo(booking.getCustomerPaidAmount()) < 0) {
                    log.error("Paid amount {} is less than required customerPaidAmount {} for booking {}",
                            actualPaid, booking.getCustomerPaidAmount(), booking.getBookingCode());
                    throw new AppException(ErrorCode.BAD_REQUEST, "Số tiền thanh toán thực tế không khớp với đơn đặt bàn");
                }
                booking.setActualPaidAmount(actualPaid);
            } else {
                booking.setActualPaidAmount(booking.getCustomerPaidAmount());
            }

            booking.setStatus(BookingStatus.PAID_CONFIRMED);
            booking.setPaidAt(Instant.now());
            bookingRepository.save(booking);

            if (booking.getVoucher() != null) {
                CustomerVoucher v = booking.getVoucher();
                v.setStatus("USED");
                v.setUsedAt(Instant.now());
                customerVoucherRepository.save(v);
            }

            // Hold net amount in merchant pending balance
            if (booking.getMerchantNetAmount() != null && booking.getMerchantNetAmount().compareTo(BigDecimal.ZERO) > 0) {
                if (merchantWalletService != null) merchantWalletService.holdPending(
                        booking.getStore().getStoreId(),
                        booking.getMerchantNetAmount(),
                        "BOOKING",
                        booking.getBookingId().toString(),
                        "Doanh thu cọc đặt bàn #" + booking.getBookingCode()
                );
            }

            if (paymentAttemptService != null) {
                paymentAttemptService.markStatus(paymentOrderCode, "PAID");
            }

            log.info("Booking {} confirmed payment successfully", booking.getBookingCode());

            evictCachedPayOSResponse(booking.getBookingId());

            dispatchBookingNotification(
                    booking,
                    BookingEventType.PAID_CONFIRMED,
                    "Khách " + booking.getCustomerName() + " đã thanh toán cọc thành công cho đơn " + booking.getBookingCode()
            );
        }
    }

    @Override
    @Transactional
    public BookingResponse getBookingDetail(UUID bookingId) {
        User currentUser = currentUserService.getCurrentUser();
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));

        boolean isOwner = booking.getUser().getUserId().equals(currentUser.getUserId());
        boolean isStaffOrAdmin = currentUserService.isSystemAdmin();
        if (!isStaffOrAdmin) {
            try {
                storeStaffService.requireStoreAccess(booking.getStore().getStoreId());
                isStaffOrAdmin = true;
            } catch (Exception ignored) {}
        }

        if (!isOwner && !isStaffOrAdmin) {
            throw new AppException(ErrorCode.FORBIDDEN_ACTION, "Bạn không thể xem đơn đặt của người khác");
        }

        booking = reconcileBookingStatus(booking);
        StoreBookingConfig config = getOrCreateConfig(booking.getStore());
        return enrichBookingResponse(booking, config);
    }

    @Override
    @Transactional(readOnly = true)
    public BookingCustomerDetailResponse getCustomerBookingDetail(UUID bookingId) {
        User currentUser = currentUserService.getCurrentUser();
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));

        if (!booking.getUser().getUserId().equals(currentUser.getUserId())) {
            throw new AppException(ErrorCode.FORBIDDEN_ACTION, "Bạn không thể xem đơn đặt của người khác");
        }

        booking = reconcileBookingStatus(booking);
        BookingCustomerDetailResponse response = bookingMapper.toCustomerDetail(booking);
        response.setAllowedActions(bookingFinanceService.calculateCustomerAllowedActions(booking));

        if (booking.getStatus() == BookingStatus.PAID_CONFIRMED || booking.getStatus() == BookingStatus.CHECKED_IN
                || booking.getStatus() == BookingStatus.COMPLETED) {
            response.setPaymentStatus("PAID");
        } else if (booking.getStatus() == BookingStatus.PENDING_PAYMENT) {
            response.setPaymentStatus("PENDING");
            PayOSResponse cached = getCachedPayOSResponse(booking.getBookingId());
            if (cached != null) {
                response.setCheckoutUrl(cached.getCheckoutUrl());
                response.setPaymentQrCode(cached.getQrCode());
            }
        } else {
            response.setPaymentStatus("CANCELLED");
        }

        if (Boolean.TRUE.equals(booking.getRefundProcessed())) {
            response.setRefundOutcome(RefundOutcomeResponse.builder()
                    .actualPaidRefundPoints(booking.getActualPaidAmount() != null ? booking.getActualPaidAmount().longValue() : 0L)
                    .voucherRestorationStatus(booking.getVoucher() != null ? "RESTORED" : "NO_VOUCHER")
                    .processedAt(booking.getCancelledAt())
                    .eventId("EVT-" + booking.getBookingCode())
                    .build());
        }

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public BookingStaffDetailResponse getStaffBookingDetail(UUID storeId, UUID bookingId) {
        User currentUser = currentUserService.getCurrentUser();
        storeStaffService.requireStoreAccess(storeId);

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));

        if (!booking.getStore().getStoreId().equals(storeId)) {
            throw new AppException(ErrorCode.FORBIDDEN_STORE_ACCESS, "Đơn đặt bàn không thuộc cơ sở này");
        }

        booking = reconcileBookingStatus(booking);
        BookingStaffDetailResponse response = bookingMapper.toStaffDetail(booking);
        response.setAllowedActions(bookingFinanceService.calculateStaffAllowedActions(booking));
        return response;
    }

    @Override
    @Transactional
    public BookingResponse getBookingByCode(String bookingCode) {
        Booking booking = bookingRepository.findByBookingCode(bookingCode.trim().toUpperCase())
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));

        booking = reconcileBookingStatus(booking);
        StoreBookingConfig config = getOrCreateConfig(booking.getStore());
        BookingResponse res = enrichBookingResponse(booking, config);

        // Sanitize sensitive PII if caller is not the owner or store staff
        User currentUser = null;
        try {
            currentUser = currentUserService.getCurrentUser();
        } catch (Exception ignored) {}

        boolean isAuthorized = currentUser != null && (
                booking.getUser().getUserId().equals(currentUser.getUserId())
                        || currentUserService.isSystemAdmin()
        );

        if (!isAuthorized && currentUser != null) {
            try {
                storeStaffService.requireStoreAccess(booking.getStore().getStoreId());
                isAuthorized = true;
            } catch (Exception ignored) {}
        }

        if (!isAuthorized) {
            res.setQrCodeToken(null);
            res.setCustomerPhone(maskPhone(res.getCustomerPhone()));
            res.setCustomerEmail(maskEmail(res.getCustomerEmail()));
        }

        return res;
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 6) return phone;
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 3);
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return email;
        int atIdx = email.indexOf("@");
        String name = email.substring(0, atIdx);
        String domain = email.substring(atIdx);
        if (name.length() <= 2) return name.charAt(0) + "*" + domain;
        return name.substring(0, 2) + "***" + domain;
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public BookingResponse cancelBooking(UUID bookingId, CancelBookingRequest request) {
        BookingResponse snapshot = inTransaction(() -> {
            Booking booking = bookingRepository.findById(bookingId)
                    .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));
            if (!booking.getUser().getUserId().equals(currentUserService.getCurrentUser().getUserId())) {
                throw new AppException(ErrorCode.FORBIDDEN_ACTION);
            }
            StoreBookingConfig config = getOrCreateConfig(booking.getStore());
            Instant deadline = booking.getFreeCancelDeadline() != null ? booking.getFreeCancelDeadline()
                    : booking.getStartTime().minus(Duration.ofHours(config.getFreeCancelHours()));
            if (booking.getStatus() == BookingStatus.PAID_CONFIRMED && Instant.now().isAfter(deadline)) {
                throw new AppException(ErrorCode.CANNOT_CANCEL_BOOKING,
                        "Đã quá thời hạn hủy đơn miễn phí (trước giờ hẹn " + config.getFreeCancelHours() + " tiếng)");
            }
            return enrichBookingResponse(booking, config);
        });
        var observation = snapshot.getStatus() == BookingStatus.PENDING_PAYMENT
                ? paymentAttemptService.cancelAndReconcile(snapshot.getPaymentOrderCode()) : null;
        if (observation != null && observation.getStatus() == com.moodcafe.payment.dto.GatewayPaymentObservation.GatewayStatus.PAID) {
            inTransaction(() -> { confirmBookingPayment(snapshot.getPaymentOrderCode(),
                    observation.getAmount() != null ? observation.getAmount().longValueExact() : null); return null; });
        }
        return inTransaction(() -> cancelBookingLocked(bookingId, request, observation));
    }

    private BookingResponse cancelBookingLocked(UUID bookingId, CancelBookingRequest request,
            com.moodcafe.payment.dto.GatewayPaymentObservation observation) {
        User currentUser = currentUserService.getCurrentUser();
        Booking booking = bookingRepository.findByIdForUpdate(bookingId)
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));

        if (!booking.getUser().getUserId().equals(currentUser.getUserId())) {
            throw new AppException(ErrorCode.FORBIDDEN_ACTION, "Bạn không thể hủy đơn đặt của người khác");
        }

        if (booking.getStatus() == BookingStatus.CANCELLED_REFUNDED) {
            return enrichBookingResponse(booking, getOrCreateConfig(booking.getStore()));
        }
        if (booking.getStatus() != BookingStatus.PAID_CONFIRMED && booking.getStatus() != BookingStatus.PENDING_PAYMENT) {
            throw new AppException(ErrorCode.CANNOT_CANCEL_BOOKING, "Trạng thái đơn hiện tại không thể hủy");
        }

        StoreBookingConfig config = getOrCreateConfig(booking.getStore());
        Instant cancelDeadline = booking.getFreeCancelDeadline() != null
                ? booking.getFreeCancelDeadline()
                : booking.getStartTime().minus(Duration.ofHours(config.getFreeCancelHours()));

        if (booking.getStatus() == BookingStatus.PAID_CONFIRMED && Instant.now().isAfter(cancelDeadline)) {
            throw new AppException(ErrorCode.CANNOT_CANCEL_BOOKING,
                    "Đã quá thời hạn hủy đơn miễn phí (trước giờ hẹn " + config.getFreeCancelHours() + " tiếng)");
        }

        if (booking.getStatus() == BookingStatus.PENDING_PAYMENT) {
            if (observation == null || (observation.getStatus() != com.moodcafe.payment.dto.GatewayPaymentObservation.GatewayStatus.CANCELLED
                    && observation.getStatus() != com.moodcafe.payment.dto.GatewayPaymentObservation.GatewayStatus.EXPIRED)) {
                throw new AppException(ErrorCode.CONFLICT, "Thanh toán đang được xác minh, vui lòng thử lại sau");
            }
        }

        if (booking.getStatus() == BookingStatus.PENDING_PAYMENT && observation != null
                && observation.getAmount() != null && observation.getAmount().signum() > 0) {
            booking.setActualPaidAmount(observation.getAmount());
            booking.setPaidAt(Instant.now());
            refundActualCash(booking);
        }

        // Voucher restoration / unlock logic via canonical lifecycle service
        if (booking.getVoucher() != null) {
            if (voucherLifecycleService != null) {
                voucherLifecycleService.restoreVoucherForRefund(booking.getBookingId(), booking.getVoucher().getVoucherId());
            } else {
                CustomerVoucher voucher = customerVoucherRepository.findByIdWithLock(booking.getVoucher().getVoucherId())
                        .orElse(null);
                if (voucher != null) {
                    voucher.setStatus("AVAILABLE");
                    voucher.setLockedBookingId(null);
                    voucher.setHoldUntil(null);
                    voucher.setUsedAt(null);
                    customerVoucherRepository.save(voucher);
                }
            }
        }

        // Revert merchant pending balance if booking was PAID_CONFIRMED
        if (booking.getStatus() == BookingStatus.PAID_CONFIRMED) {
            if (booking.getMerchantNetAmount() != null && booking.getMerchantNetAmount().compareTo(BigDecimal.ZERO) > 0) {
                if (merchantWalletService != null) merchantWalletService.revertPending(
                        booking.getStore().getStoreId(),
                        booking.getMerchantNetAmount(),
                        "BOOKING",
                        booking.getBookingId().toString(),
                        "Đảo hoàn doanh thu tạm giữ do khách hủy đơn #" + booking.getBookingCode()
                );
            }

            // Refund customer paid amount back to MoodPoints (1 MoodPoint = 1 VND)
            // STRICT RULE: Only refund actual money paid. Zero-pay has actualPaidAmount = 0, so 0 points are refunded!
            BigDecimal paid = booking.getActualPaidAmount() != null ? booking.getActualPaidAmount() : BigDecimal.ZERO;
            if (paid.compareTo(BigDecimal.ZERO) > 0) {
                if (customerWalletService != null) customerWalletService.creditRefundPoints(RefundPointCommand.builder()
                        .userId(booking.getUser().getUserId())
                        .points(paid.longValue())
                        .refundId(booking.getBookingId().toString())
                        .businessKey("REFUND_BOOKING_" + booking.getBookingId())
                        .description("Hoàn cọc đơn đặt bàn " + booking.getBookingCode() + " thành MoodPoints (1:1)")
                        .build());
            }
        }

        booking.setStatus(BookingStatus.CANCELLED_REFUNDED);
        booking.setCancelledAt(Instant.now());
        booking.setCancelReason(request.getReason() != null ? request.getReason().trim() : null);
        booking.setRefundProcessed(true);

        booking = bookingRepository.save(booking);
        evictCachedPayOSResponse(booking.getBookingId());
        BookingResponse response = enrichBookingResponse(booking, config);

        dispatchBookingNotification(
                booking,
                BookingEventType.CANCELLED,
                "Đơn đặt bàn " + booking.getBookingCode() + " đã bị hủy."
        );
        return response;
    }

    private void refundActualCash(Booking booking) {
        BigDecimal paid = booking.getActualPaidAmount() != null ? booking.getActualPaidAmount() : BigDecimal.ZERO;
        if (paid.signum() > 0) customerWalletService.creditRefundPoints(RefundPointCommand.builder()
                .userId(booking.getUser().getUserId()).points(paid.longValueExact())
                .refundId(booking.getBookingId().toString()).businessKey("REFUND_BOOKING_" + booking.getBookingId())
                .description("Hoàn tiền thực nhận đơn đặt bàn " + booking.getBookingCode()).build());
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void timeoutPendingBooking(UUID bookingId) {
        Long orderCode = inTransaction(() -> bookingRepository.findById(bookingId)
                .filter(b -> b.getStatus() == BookingStatus.PENDING_PAYMENT)
                .map(Booking::getPaymentOrderCode).orElse(null));
        if (orderCode == null) return;
        var observation = paymentAttemptService.cancelAndReconcile(orderCode);
        if (observation == null) return;
        if (observation.getStatus() == com.moodcafe.payment.dto.GatewayPaymentObservation.GatewayStatus.PAID) {
            inTransaction(() -> { confirmBookingPayment(orderCode,
                    observation.getAmount() != null ? observation.getAmount().longValueExact() : null); return null; });
            return;
        }
        if (observation.getStatus() != com.moodcafe.payment.dto.GatewayPaymentObservation.GatewayStatus.CANCELLED
                && observation.getStatus() != com.moodcafe.payment.dto.GatewayPaymentObservation.GatewayStatus.EXPIRED) return;
        inTransaction(() -> {
            Booking booking = bookingRepository.findByIdForUpdate(bookingId).orElseThrow();
            if (booking.getStatus() != BookingStatus.PENDING_PAYMENT) return null;
            booking.setStatus(BookingStatus.PAYMENT_FAILED);
            if (booking.getVoucher() != null) voucherLifecycleService.restoreVoucherForRefund(bookingId, booking.getVoucher().getVoucherId());
            if (observation.getAmount() != null && observation.getAmount().signum() > 0) {
                booking.setActualPaidAmount(observation.getAmount());
                booking.setPaidAt(Instant.now());
                refundActualCash(booking);
                booking.setRefundProcessed(true);
            }
            bookingRepository.save(booking);
            paymentAttemptService.markStatus(orderCode, observation.getStatus().name());
            dispatchBookingNotification(booking, BookingEventType.PAYMENT_FAILED, "Đơn đặt bàn đã hết hạn thanh toán");
            return null;
        });
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public BookingResponse getBookingPaymentLink(UUID bookingId) {
        BookingResponse prepared = inTransaction(() -> {
            User user = currentUserService.getCurrentUser();
            Booking booking = bookingRepository.findById(bookingId)
                    .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));
            if (!booking.getUser().getUserId().equals(user.getUserId())) {
                throw new AppException(ErrorCode.FORBIDDEN_ACTION);
            }
            return enrichBookingResponse(booking, getOrCreateConfig(booking.getStore()));
        });
        return prepared.getStatus() == BookingStatus.PENDING_PAYMENT ? resumePayment(prepared) : prepared;
    }

    private BookingResponse resumePayment(BookingResponse prepared) {
        Long orderCode = prepared.getPaymentOrderCode();
        // Recover persisted gateway details first. Never allocate a new order code on retry.
        Optional<PaymentAttempt> attempt = paymentAttemptService.findByOrderCode(orderCode);
        if (isPaymentDeadlinePassed(prepared)) {
            timeoutPendingBooking(prepared.getBookingId());
            throw new AppException(ErrorCode.BAD_REQUEST, "Đơn đặt bàn đã hết thời gian giữ chỗ.");
        }
        if (attempt.isPresent() && isTerminalAttempt(attempt.get().getStatus())) {
            prepared = createReplacementPaymentAttempt(prepared);
            orderCode = prepared.getPaymentOrderCode();
            attempt = paymentAttemptService.findByOrderCode(orderCode);
        }
        if (attempt.isPresent() && attempt.get().getCheckoutUrl() != null) {
            var observation = paymentAttemptService.reconcile(orderCode);
            if (observation != null && (observation.getStatus() == com.moodcafe.payment.dto.GatewayPaymentObservation.GatewayStatus.CANCELLED
                    || observation.getStatus() == com.moodcafe.payment.dto.GatewayPaymentObservation.GatewayStatus.EXPIRED)) {
                paymentAttemptService.markStatus(orderCode, observation.getStatus().name());
                prepared = createReplacementPaymentAttempt(prepared);
                orderCode = prepared.getPaymentOrderCode();
                attempt = paymentAttemptService.findByOrderCode(orderCode);
            } else if (observation != null && observation.getStatus() == com.moodcafe.payment.dto.GatewayPaymentObservation.GatewayStatus.PAID) {
                final Long paidOrderCode = orderCode;
                inTransaction(() -> {
                    confirmBookingPayment(paidOrderCode,
                            observation.getAmount() != null ? observation.getAmount().longValueExact() : null);
                    return null;
                });
                Booking booking = bookingRepository.findById(prepared.getBookingId()).orElseThrow();
                return enrichBookingResponse(booking, getOrCreateConfig(booking.getStore()));
            } else {
                prepared.setCheckoutUrl(attempt.get().getCheckoutUrl());
                prepared.setPaymentQrCode(attempt.get().getQrCodeUrl());
                return prepared;
            }
        }
        final BookingResponse activeBooking = prepared;
        final Long activeOrderCode = orderCode;
        try {
            PayOSResponse gateway = payOSService.createBookingPaymentLink(activeBooking.getBookingId(), activeOrderCode,
                    activeBooking.getCustomerPaidAmount().longValueExact(), "DATBAN " + activeOrderCode);
            if (gateway == null) return activeBooking;
            if ("PAID".equals(gateway.getStatus())) {
                var observed = paymentAttemptService.reconcile(activeOrderCode);
                if (observed != null && observed.getStatus() == com.moodcafe.payment.dto.GatewayPaymentObservation.GatewayStatus.PAID) {
                    inTransaction(() -> { confirmBookingPayment(activeOrderCode,
                            observed.getAmount() != null ? observed.getAmount().longValueExact() : null); return null; });
                }
            }
            return inTransaction(() -> {
                Booking booking = bookingRepository.findById(activeBooking.getBookingId())
                        .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));
                if (booking.getStatus() == BookingStatus.PENDING_PAYMENT) {
                    booking.setPaymentUrl(gateway.getCheckoutUrl());
                    bookingRepository.save(booking);
                }
                paymentAttemptService.recordGatewayResponse(activeOrderCode, gateway.getPaymentLinkId(),
                        gateway.getCheckoutUrl(), gateway.getQrCode());
                cachePayOSResponse(booking.getBookingId(), gateway);
                BookingResponse response = enrichBookingResponse(booking, getOrCreateConfig(booking.getStore()));
                if (booking.getStatus() == BookingStatus.PENDING_PAYMENT) {
                    response.setCheckoutUrl(gateway.getCheckoutUrl());
                    response.setPaymentQrCode(gateway.getQrCode());
                    response.setPaymentAccountNumber(gateway.getAccountNumber());
                    response.setPaymentAccountName(gateway.getAccountName());
                    response.setPaymentBin(gateway.getBin());
                    response.setPaymentDescription(gateway.getDescription());
                }
                return response;
            });
        } catch (Exception ex) {
            // Preparation already committed: accepted-but-timeout must retain booking, voucher and attempt.
            inTransaction(() -> {
                paymentAttemptService.markStatus(activeOrderCode, "UNKNOWN");
                return null;
            });
            log.warn("Gateway outcome unknown for booking {} / order {}", activeBooking.getBookingId(), activeOrderCode);
            return activeBooking;
        }
    }

    private boolean isTerminalAttempt(String status) {
        return "CANCELLED".equalsIgnoreCase(status) || "EXPIRED".equalsIgnoreCase(status);
    }

    private boolean isPaymentDeadlinePassed(BookingResponse booking) {
        return booking.getPaymentExpiresAt() != null && !Instant.now().isBefore(booking.getPaymentExpiresAt());
    }

    /** Creates a new PayOS session while preserving the original booking hold and voucher lock. */
    private BookingResponse createReplacementPaymentAttempt(BookingResponse prepared) {
        return inTransaction(() -> {
            Booking booking = bookingRepository.findByIdForUpdate(prepared.getBookingId())
                    .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));
            if (booking.getStatus() != BookingStatus.PENDING_PAYMENT) {
                return enrichBookingResponse(booking, getOrCreateConfig(booking.getStore()));
            }
            if (booking.getPaymentExpiresAt() != null && !Instant.now().isBefore(booking.getPaymentExpiresAt())) {
                throw new AppException(ErrorCode.BAD_REQUEST, "Đơn đặt bàn đã hết thời gian giữ chỗ.");
            }
            Optional<PaymentAttempt> active = paymentAttemptService.findActiveBookingAttempt(booking.getBookingId().toString());
            if (active.isPresent()) {
                booking.setPaymentOrderCode(active.get().getOrderCode());
                bookingRepository.save(booking);
                return enrichBookingResponse(booking, getOrCreateConfig(booking.getStore()));
            }
            long replacementOrderCode = orderCodeGeneratorService.nextOrderCode();
            paymentAttemptService.createAttempt("BOOKING", booking.getBookingId().toString(), replacementOrderCode,
                    booking.getCustomerPaidAmount(), "VND", null, booking.getUser().getUserId(), null);
            booking.setPaymentOrderCode(replacementOrderCode);
            booking.setPaymentUrl(null);
            bookingRepository.save(booking);
            return enrichBookingResponse(booking, getOrCreateConfig(booking.getStore()));
        });
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponse> getMyBookings() {
        User currentUser = currentUserService.getCurrentUser();
        return bookingRepository.findAllByUserUserIdOrderByStartTimeDesc(currentUser.getUserId()).stream()
                .map(b -> {
                    StoreBookingConfig config = getOrCreateConfig(b.getStore());
                    return enrichBookingResponse(b, config);
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<BookingResponse> getMyBookings(Pageable pageable) {
        return getMyBookings(null, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<BookingResponse> getMyBookings(String status, Pageable pageable) {
        User currentUser = currentUserService.getCurrentUser();
        Page<Booking> page;
        if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status.trim())) {
            String s = status.trim().toUpperCase();
            if ("CANCELLED".equals(s) || "CANCELLED_REFUNDED".equals(s)) {
                List<BookingStatus> cancelledStatuses = List.of(
                        BookingStatus.CANCELLED_REFUNDED,
                        BookingStatus.PAYMENT_FAILED,
                        BookingStatus.TABLE_EXPIRED,
                        BookingStatus.EXPIRED_NO_SHOW
                );
                page = bookingRepository.findAllByUserUserIdAndStatusInOrderByCreatedAtDesc(
                        currentUser.getUserId(), cancelledStatuses, pageable);
            } else {
                try {
                    BookingStatus bookingStatus = BookingStatus.valueOf(s);
                    page = bookingRepository.findAllByUserUserIdAndStatusOrderByCreatedAtDesc(
                            currentUser.getUserId(), bookingStatus, pageable);
                } catch (IllegalArgumentException e) {
                    page = bookingRepository.findAllByUserUserIdOrderByCreatedAtDesc(currentUser.getUserId(), pageable);
                }
            }
        } else {
            page = bookingRepository.findAllByUserUserIdOrderByCreatedAtDesc(currentUser.getUserId(), pageable);
        }

        List<BookingResponse> items = page.getContent().stream()
                .map(b -> {
                    StoreBookingConfig config = getOrCreateConfig(b.getStore());
                    return enrichBookingResponse(b, config);
                })
                .toList();

        return PageResponse.<BookingResponse>builder()
                .items(items)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponse> getStoreDeskBookings(UUID storeId, LocalDate date, Instant from, Instant to) {
        User currentUser = currentUserService.getCurrentUser();
        storeStaffService.requireStoreAccess(storeId);

        List<Booking> list;
        if (from != null && to != null) {
            list = bookingRepository.findAllByStoreStoreIdAndStartTimeBetweenOrderByStartTimeAsc(storeId, from, to);
        } else if (date != null) {
            ZonedDateTime dayStart = date.atTime(0, 0).atZone(VN_ZONE);
            ZonedDateTime dayEnd = date.atTime(23, 59, 59).atZone(VN_ZONE);
            list = bookingRepository.findAllByStoreStoreIdAndStartTimeBetweenOrderByStartTimeAsc(
                    storeId, dayStart.toInstant(), dayEnd.toInstant());
        } else {
            list = bookingRepository.findAllByStoreStoreIdOrderByStartTimeDesc(storeId);
        }

        Store store = storeRepository.findById(storeId).orElse(null);
        StoreBookingConfig config = getOrCreateConfig(store);

        return list.stream()
                .map(b -> enrichBookingResponse(b, config))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<BookingResponse> getStoreDeskBookings(UUID storeId, LocalDate date, Instant from, Instant to, Pageable pageable) {
        User currentUser = currentUserService.getCurrentUser();
        storeStaffService.requireStoreAccess(storeId);

        Page<Booking> page;
        if (from != null && to != null) {
            page = bookingRepository.findAllByStoreStoreIdAndStartTimeBetweenOrderByCreatedAtDesc(storeId, from, to, pageable);
        } else if (date != null) {
            ZonedDateTime dayStart = date.atTime(0, 0).atZone(VN_ZONE);
            ZonedDateTime dayEnd = date.atTime(23, 59, 59).atZone(VN_ZONE);
            page = bookingRepository.findAllByStoreStoreIdAndStartTimeBetweenOrderByCreatedAtDesc(
                    storeId, dayStart.toInstant(), dayEnd.toInstant(), pageable);
        } else {
            page = bookingRepository.findAllByStoreStoreIdOrderByCreatedAtDesc(storeId, pageable);
        }

        Store store = storeRepository.findById(storeId).orElse(null);
        StoreBookingConfig config = getOrCreateConfig(store);

        List<BookingResponse> items = page.getContent().stream()
                .map(b -> enrichBookingResponse(b, config))
                .toList();

        return PageResponse.<BookingResponse>builder()
                .items(items)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public BookingDeskSummaryResponse getStoreDeskSummary(UUID storeId, LocalDate date) {
        User currentUser = currentUserService.getCurrentUser();
        storeStaffService.requireStoreAccess(storeId);

        LocalDate targetDate = date != null ? date : LocalDate.now(VN_ZONE);
        ZonedDateTime dayStart = targetDate.atTime(0, 0).atZone(VN_ZONE);
        ZonedDateTime dayEnd = targetDate.atTime(23, 59, 59).atZone(VN_ZONE);

        List<Booking> dayBookings = bookingRepository.findAllByStoreStoreIdAndStartTimeBetweenOrderByStartTimeAsc(
                storeId, dayStart.toInstant(), dayEnd.toInstant());

        long pending = dayBookings.stream().filter(b -> b.getStatus() == BookingStatus.PENDING_PAYMENT).count();
        long confirmed = dayBookings.stream().filter(b -> b.getStatus() == BookingStatus.PAID_CONFIRMED).count();
        long checkedIn = dayBookings.stream().filter(b -> b.getStatus() == BookingStatus.CHECKED_IN).count();
        long completed = dayBookings.stream().filter(b -> b.getStatus() == BookingStatus.COMPLETED).count();
        long tableExpired = dayBookings.stream().filter(b -> b.getStatus() == BookingStatus.TABLE_EXPIRED).count();
        long claimedLate = dayBookings.stream().filter(b -> b.getStatus() == BookingStatus.CLAIMED_LATE).count();
        long cancelled = dayBookings.stream().filter(b -> b.getStatus() == BookingStatus.CANCELLED_REFUNDED).count();

        long totalRevenue = dayBookings.stream()
                .filter(b -> b.getStatus() == BookingStatus.PAID_CONFIRMED
                        || b.getStatus() == BookingStatus.CHECKED_IN
                        || b.getStatus() == BookingStatus.COMPLETED
                        || b.getStatus() == BookingStatus.CLAIMED_LATE)
                .mapToLong(b -> b.getGrossAmount() != null ? b.getGrossAmount().longValue() : b.getMinSpendAmount())
                .sum();

        long totalQuota = tableCategoryRepository.findAllByStoreStoreIdAndIsActiveTrue(storeId).stream()
                .mapToLong(TableCategory::getAppQuota)
                .sum();

        return BookingDeskSummaryResponse.builder()
                .totalTodayBookings(dayBookings.size())
                .pendingArrivalCount(confirmed)
                .checkedInCount(checkedIn)
                .completedCount(completed)
                .tableExpiredCount(tableExpired)
                .claimedLateCount(claimedLate)
                .totalAppQuota(totalQuota)
                .build();
    }

    @Override
    @Transactional
    public BookingResponse deskCheckIn(UUID storeId, DeskCheckInRequest request) {
        User currentUser = currentUserService.getCurrentUser();
        storeStaffService.requireStoreAccess(storeId);

        String codeOrToken = request.getCodeOrToken() != null ? request.getCodeOrToken().trim() : "";
        Booking booking = bookingRepository.findAllByStoreStoreIdOrderByCreatedAtDesc(storeId).stream()
                .filter(b -> codeOrToken.equals(b.getQrCodeToken()))
                .findFirst()
                .orElse(null);

        if (booking == null && !codeOrToken.isBlank()) {
            booking = bookingRepository.findByBookingCode(codeOrToken.toUpperCase())
                    .filter(b -> b.getStore().getStoreId().equals(storeId))
                    .orElse(null);
        }

        if (booking == null) {
            throw new AppException(ErrorCode.BOOKING_NOT_FOUND, "Không tìm thấy thông tin đơn đặt bàn tại quán");
        }

        if (booking.getStatus() != BookingStatus.PAID_CONFIRMED) {
            throw new AppException(ErrorCode.INVALID_BOOKING_STATUS_TRANSITION,
                    "Đơn đặt bàn không ở trạng thái sẵn sàng check-in (Hiện tại: " + booking.getStatus() + ")");
        }

        booking.setStatus(BookingStatus.CHECKED_IN);
        booking.setCheckedInAt(Instant.now());
        booking = bookingRepository.save(booking);

        Instant graceDeadline = booking.getGraceDeadline();
        if (graceDeadline == null || !Instant.now().isAfter(graceDeadline)) {
            try {
                if (loyaltyRewardService != null) loyaltyRewardService.awardBookingOnTimeCheckIn(booking);
            } catch (Exception ex) {
                log.warn("Failed to award on-time check-in reward: {}", ex.getMessage());
            }
        }

        StoreBookingConfig config = getOrCreateConfig(booking.getStore());
        BookingResponse res = enrichBookingResponse(booking, config);

        dispatchBookingNotification(
                booking,
                BookingEventType.CHECKED_IN,
                "Khách " + booking.getCustomerName() + " đã check-in thành công cho đơn " + booking.getBookingCode()
        );
        return res;
    }

    @Override
    @Transactional
    public BookingResponse deskClaimLate(UUID storeId, UUID bookingId) {
        User currentUser = currentUserService.getCurrentUser();
        storeStaffService.requireStoreAccess(storeId);

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));

        if (!booking.getStore().getStoreId().equals(storeId)) {
            throw new AppException(ErrorCode.FORBIDDEN_STORE_ACCESS, "Đơn đặt bàn không thuộc cơ sở này");
        }

        if (booking.getStatus() != BookingStatus.TABLE_EXPIRED) {
            throw new AppException(ErrorCode.INVALID_BOOKING_STATUS_TRANSITION,
                    "Chỉ đơn quá thời gian giữ bàn (TABLE_EXPIRED) mới có thể xác nhận đổi nước trễ");
        }

        booking.setStatus(BookingStatus.CLAIMED_LATE);
        booking.setClaimedLateAt(Instant.now());
        booking = bookingRepository.save(booking);

        if (booking.getMerchantNetAmount() != null && booking.getMerchantNetAmount().compareTo(BigDecimal.ZERO) > 0) {
            if (merchantWalletService != null) merchantWalletService.settleToAvailable(
                    booking.getStore().getStoreId(),
                    booking.getMerchantNetAmount(),
                    "BOOKING",
                    booking.getBookingId().toString(),
                    "Giải ngân doanh thu đổi nước trễ #" + booking.getBookingCode()
            );
        }

        try {
            if (loyaltyRewardService != null) loyaltyRewardService.awardBookingSpendReward(booking);
        } catch (Exception ex) {
            log.warn("Failed to award spend reward: {}", ex.getMessage());
        }

        StoreBookingConfig config = getOrCreateConfig(booking.getStore());
        BookingResponse res = enrichBookingResponse(booking, config);

        dispatchBookingNotification(
                booking,
                BookingEventType.CLAIMED_LATE,
                "Khách " + booking.getCustomerName() + " đã nhận đổi nước trễ cho đơn " + booking.getBookingCode()
        );
        return res;
    }

    @Override
    @Transactional
    public BookingResponse deskComplete(UUID storeId, UUID bookingId) {
        User currentUser = currentUserService.getCurrentUser();
        storeStaffService.requireStoreAccess(storeId);

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));

        if (!booking.getStore().getStoreId().equals(storeId)) {
            throw new AppException(ErrorCode.FORBIDDEN_STORE_ACCESS, "Đơn đặt bàn không thuộc cơ sở này");
        }

        if (booking.getStatus() != BookingStatus.CHECKED_IN) {
            throw new AppException(ErrorCode.INVALID_BOOKING_STATUS_TRANSITION,
                    "Chỉ đơn đang ngồi (CHECKED_IN) mới có thể hoàn tất trả bàn");
        }

        booking.setStatus(BookingStatus.COMPLETED);
        booking.setCompletedAt(Instant.now());
        booking = bookingRepository.save(booking);

        if (booking.getMerchantNetAmount() != null && booking.getMerchantNetAmount().compareTo(BigDecimal.ZERO) > 0) {
            if (merchantWalletService != null) merchantWalletService.settleToAvailable(
                    booking.getStore().getStoreId(),
                    booking.getMerchantNetAmount(),
                    "BOOKING",
                    booking.getBookingId().toString(),
                    "Giải ngân doanh thu đơn đặt bàn hoàn tất #" + booking.getBookingCode()
            );
        }

        try {
            if (loyaltyRewardService != null) loyaltyRewardService.awardBookingSpendReward(booking);
        } catch (Exception ex) {
            log.warn("Failed to award spend reward: {}", ex.getMessage());
        }

        StoreBookingConfig config = getOrCreateConfig(booking.getStore());
        BookingResponse res = enrichBookingResponse(booking, config);

        dispatchBookingNotification(
                booking,
                BookingEventType.COMPLETED,
                "Đơn đặt bàn " + booking.getBookingCode() + " đã hoàn tất phục vụ."
        );
        return res;
    }

    @Override
    @Transactional
    public BookingResponse selfCheckIn(UUID bookingId, String qrToken) {
        User currentUser = currentUserService.getCurrentUser();
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));

        if (!booking.getUser().getUserId().equals(currentUser.getUserId())) {
            throw new AppException(ErrorCode.FORBIDDEN_ACTION, "Bạn không thể thao tác trên đơn của người khác");
        }
        if (booking.getStatus() != BookingStatus.PAID_CONFIRMED) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Đơn không ở trạng thái chờ nhận bàn");
        }

        requireTrustedPhysicalChallenge();

        // 2. Strict arrival time window check: cannot check in > 30 mins before start, or after grace deadline
        Instant now = Instant.now();
        Instant earliestAllowed = booking.getStartTime().minus(Duration.ofMinutes(30));
        if (now.isBefore(earliestAllowed)) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Chưa đến thời gian nhận bàn (chỉ có thể nhận bàn trước tối đa 30 phút)");
        }
        Instant graceDeadline = booking.getGraceDeadline() != null
                ? booking.getGraceDeadline()
                : booking.getStartTime().plus(Duration.ofMinutes(30));
        if (now.isAfter(graceDeadline)) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Đã quá thời gian nhận bàn (grace period)");
        }

        booking.setStatus(BookingStatus.CHECKED_IN);
        booking.setCheckedInAt(now);
        booking = bookingRepository.save(booking);

        if (!now.isAfter(graceDeadline)) {
            try {
                if (loyaltyRewardService != null) loyaltyRewardService.awardBookingOnTimeCheckIn(booking);
            } catch (Exception ex) {
                log.warn("Failed to award on-time check-in reward: {}", ex.getMessage());
            }
        }

        StoreBookingConfig config = getOrCreateConfig(booking.getStore());
        return enrichBookingResponse(booking, config);
    }

    @Override
    @Transactional
    public BookingResponse selfCheckOut(UUID bookingId, String qrToken) {
        User currentUser = currentUserService.getCurrentUser();
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));

        if (!booking.getUser().getUserId().equals(currentUser.getUserId())) {
            throw new AppException(ErrorCode.FORBIDDEN_ACTION, "Bạn không thể thao tác trên đơn của người khác");
        }
        if (booking.getStatus() != BookingStatus.CHECKED_IN) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Đơn không ở trạng thái đang sử dụng");
        }

        requireTrustedPhysicalChallenge();

        Instant now = Instant.now();
        Instant checkedInMoment = booking.getCheckedInAt() != null ? booking.getCheckedInAt() : booking.getStartTime();
        long durationMinutes = Duration.between(checkedInMoment, now).toMinutes();

        booking.setStatus(BookingStatus.COMPLETED);
        booking.setCompletedAt(now);
        booking = bookingRepository.save(booking);

        if (booking.getMerchantNetAmount() != null && booking.getMerchantNetAmount().compareTo(BigDecimal.ZERO) > 0) {
            if (merchantWalletService != null) merchantWalletService.settleToAvailable(
                    booking.getStore().getStoreId(),
                    booking.getMerchantNetAmount(),
                    "BOOKING",
                    booking.getBookingId().toString(),
                    "Giải ngân doanh thu đơn tự quét trả bàn #" + booking.getBookingCode()
            );
        }

        try {
            if (loyaltyRewardService != null) loyaltyRewardService.awardBookingSpendReward(booking);
        } catch (Exception ex) {
            log.warn("Failed to award spend reward: {}", ex.getMessage());
        }

        if (durationMinutes >= 30) {
            try {
                if (loyaltyRewardService != null) loyaltyRewardService.awardBookingSelfCheckOut(booking, durationMinutes);
            } catch (Exception ex) {
                log.warn("Failed to award self checkout reward: {}", ex.getMessage());
            }
        }

        StoreBookingConfig config = getOrCreateConfig(booking.getStore());
        return enrichBookingResponse(booking, config);
    }

    private void requireTrustedPhysicalChallenge() {
        // No staff-provisioned, short-lived store challenge exists yet. A customer booking
        // token or a token signed using the historical public default secret proves no presence.
        throw new AppException(ErrorCode.FORBIDDEN_ACTION,
                "Vui lòng nhờ nhân viên xác nhận nhận/trả bàn tại quán");
    }

    private Instant calculateEndTime(Instant startTime, StoreBookingConfig config, Store store, TableCategory category) {
        LocalTime closingTime = store.getClosingTime() != null ? store.getClosingTime() : LocalTime.of(22, 0);
        ZonedDateTime startVn = startTime.atZone(VN_ZONE);
        Instant closingInstant = startVn.toLocalDate().atTime(closingTime).atZone(VN_ZONE).toInstant();

        if (Boolean.TRUE.equals(config.getIsTimeLimitEnabled())) {
            Instant calculated = startTime.plus(Duration.ofMinutes(config.getSittingDurationMins()));
            return calculated.isAfter(closingInstant) ? closingInstant : calculated;
        } else {
            return closingInstant;
        }
    }

    private StoreBookingConfig getOrCreateConfig(Store store) {
        if (store == null || store.getStoreId() == null) {
            return StoreBookingConfig.builder().build();
        }
        return configRepository.findByStoreStoreId(store.getStoreId())
                .orElseGet(() -> StoreBookingConfig.builder()
                        .store(store)
                        .isBookingEnabled(false)
                        .isTimeLimitEnabled(true)
                        .sittingDurationMins(120)
                        .cleanupBufferMins(15)
                        .gracePeriodMins(30)
                        .minAdvanceMins(30)
                        .freeCancelHours(2)
                        .build());
    }

    private Booking reconcileBookingStatus(Booking booking) {
        // Scheduled transitions own status + settlement atomically. Reads never close a booking.
        return booking;
    }

    private void settleNoShowRevenue(Booking booking) {
        if (booking.getMerchantNetAmount() != null && booking.getMerchantNetAmount().compareTo(BigDecimal.ZERO) > 0) {
            if (merchantWalletService != null) {
                merchantWalletService.settleToAvailable(
                        booking.getStore().getStoreId(),
                        booking.getMerchantNetAmount(),
                        "BOOKING",
                        booking.getBookingId().toString(),
                        "Giải ngân đơn no-show cuối ngày #" + booking.getBookingCode()
                );
            }
        }
    }

    private BookingResponse enrichBookingResponse(Booking booking, StoreBookingConfig config) {
        BookingResponse res = bookingMapper.toBookingResponse(booking);
        res.setGracePeriodMins(config.getGracePeriodMins());
        res.setFreeCancelHours(config.getFreeCancelHours());

        if (booking.getStore() != null && booking.getStore().getStoreId() != null) {
            UUID storeId = booking.getStore().getStoreId();
            String storeImg = storeImageRepository.findByStoreStoreIdAndPrimaryTrue(storeId)
                    .map(StoreImage::getImageUrl)
                    .orElseGet(() -> storeImageRepository.findAllByStoreStoreId(storeId).stream()
                            .findFirst()
                            .map(StoreImage::getImageUrl)
                            .orElse(null));
            res.setStoreImageUrl(storeImg);
        }

        res.setGrossAmount(booking.getGrossAmount());
        res.setVoucherDiscount(booking.getVoucherDiscount());
        res.setCustomerPaidAmount(booking.getCustomerPaidAmount());
        res.setActualPaidAmount(booking.getActualPaidAmount());
        res.setPaymentExpiresAt(booking.getPaymentExpiresAt());
        paymentAttemptService.findByOrderCode(booking.getPaymentOrderCode())
                .ifPresent(attempt -> res.setPaymentSessionStatus(attempt.getStatus()));
        res.setGuestCount(booking.getGuestCount());
        res.setFreeCancelDeadline(booking.getFreeCancelDeadline());
        res.setGraceDeadline(booking.getGraceDeadline());
        res.setServiceClosingDeadline(booking.getServiceClosingDeadline());
        var actions = bookingFinanceService.calculateCustomerAllowedActions(booking);
        res.setAllowedActions(actions == null ? List.of() : actions.stream()
                .filter(action -> !action.startsWith("SELF_CHECK_")).toList());

        boolean cancelable = (booking.getStatus() == BookingStatus.PAID_CONFIRMED || booking.getStatus() == BookingStatus.PENDING_PAYMENT)
                && (booking.getFreeCancelDeadline() == null || Instant.now().isBefore(booking.getFreeCancelDeadline()));
        res.setIsCancelable(cancelable);

        if (booking.getStatus() == BookingStatus.PENDING_PAYMENT) {
            PayOSResponse cached = getCachedPayOSResponse(booking.getBookingId());
            if (cached != null) {
                res.setCheckoutUrl(cached.getCheckoutUrl());
                res.setPaymentQrCode(cached.getQrCode());
                res.setPaymentAccountNumber(cached.getAccountNumber());
                res.setPaymentAccountName(cached.getAccountName());
                res.setPaymentBin(cached.getBin());
                res.setPaymentDescription(cached.getDescription());
            }
        }

        if (Boolean.TRUE.equals(booking.getRefundProcessed())) {
            res.setRefundOutcome(RefundOutcomeResponse.builder()
                    .actualPaidRefundPoints(booking.getActualPaidAmount() != null ? booking.getActualPaidAmount().longValue() : 0L)
                    .voucherRestorationStatus(booking.getVoucher() != null ? "RESTORED" : "NO_VOUCHER")
                    .processedAt(booking.getCancelledAt())
                    .eventId("EVT-" + booking.getBookingCode())
                    .build());
        }

        return res;
    }

    @Override
    public void dispatchBookingNotification(Booking booking, BookingEventType eventType, String message) {
        if (booking == null) {
            return;
        }

        UUID storeId = booking.getStore() != null ? booking.getStore().getStoreId() : null;
        String storeName = booking.getStore() != null ? booking.getStore().getName() : "Quán";
        UUID customerUserId = booking.getUser() != null ? booking.getUser().getUserId() : null;
        UUID bookingId = booking.getBookingId();
        String bookingCode = booking.getBookingCode();
        String customerName = booking.getCustomerName() != null ? booking.getCustomerName() : "Khách hàng";
        String referenceId = bookingId != null ? bookingId.toString() : bookingCode;

        String customerTitle = null;
        String customerMsg = message;
        String staffTitle = null;
        String staffMsg = null;
        NotificationType notifType;

        switch (eventType) {
            case PAID_CONFIRMED -> {
                notifType = NotificationType.RESERVATION_CONFIRMED;
                customerTitle = "Đặt bàn thành công (" + bookingCode + ")";
                customerMsg = message != null ? message : "Đơn đặt bàn của bạn tại " + storeName + " đã được xác nhận thanh toán.";
                staffTitle = "Đơn đặt bàn mới (" + bookingCode + ")";
                staffMsg = "Khách hàng " + customerName + " vừa đặt bàn thành công tại quán.";
            }
            case CHECKED_IN -> {
                notifType = NotificationType.RESERVATION_UPDATED;
                customerTitle = "Đã check-in vào bàn (" + bookingCode + ")";
                customerMsg = "Chào mừng bạn đến với " + storeName + "! Bàn của bạn đã sẵn sàng phục vụ.";
                staffTitle = "Khách đã nhận bàn (" + bookingCode + ")";
                staffMsg = "Khách hàng " + customerName + " (" + bookingCode + ") đã check-in thành công.";
            }
            case COMPLETED -> {
                notifType = NotificationType.RESERVATION_UPDATED;
                customerTitle = "Trải nghiệm hoàn tất (" + bookingCode + ")";
                customerMsg = "Cảm ơn bạn đã ghé thăm " + storeName + ". Hãy để lại đánh giá cho quán nhé!";
                staffTitle = "Hoàn tất đơn đặt bàn (" + bookingCode + ")";
                staffMsg = "Đơn đặt bàn " + bookingCode + " của khách " + customerName + " đã hoàn tất phục vụ.";
            }
            case TABLE_EXPIRED -> {
                notifType = NotificationType.RESERVATION_UPDATED;
                customerTitle = "Quá giờ giữ bàn (" + bookingCode + ")";
                customerMsg = message != null ? message : "Đơn đặt bàn " + bookingCode + " đã quá thời gian giữ bàn 15 phút.";
                staffTitle = "Bàn quá thời gian giữ (" + bookingCode + ")";
                staffMsg = "Đơn " + bookingCode + " của khách " + customerName + " đã quá 15 phút giữ bàn.";
            }
            case CLAIMED_LATE -> {
                notifType = NotificationType.RESERVATION_UPDATED;
                customerTitle = "Nhận bàn trễ (" + bookingCode + ")";
                customerMsg = message != null ? message : "Bạn đã nhận bàn trễ cho đơn " + bookingCode + ".";
                staffTitle = "Khách nhận bàn trễ (" + bookingCode + ")";
                staffMsg = "Khách hàng " + customerName + " đã nhận bàn trễ cho đơn " + bookingCode + ".";
            }
            case CANCELLED -> {
                notifType = NotificationType.RESERVATION_CANCELLED;
                customerTitle = "Đơn đặt bàn đã hủy (" + bookingCode + ")";
                customerMsg = message != null ? message : "Đơn đặt bàn " + bookingCode + " tại " + storeName + " đã được hủy.";
                staffTitle = "Đơn đặt bàn đã hủy (" + bookingCode + ")";
                staffMsg = "Đơn đặt bàn " + bookingCode + " của khách " + customerName + " đã bị hủy.";
            }
            case PAYMENT_FAILED -> {
                notifType = NotificationType.RESERVATION_CANCELLED;
                customerTitle = "Thanh toán đặt bàn thất bại (" + bookingCode + ")";
                customerMsg = message != null ? message : "Giao dịch cọc đồ uống cho đơn " + bookingCode + " đã thất bại hoặc quá hạn.";
            }
            case CREATED -> {
                notifType = NotificationType.RESERVATION_CREATED;
                customerTitle = "Tạo đơn đặt bàn (" + bookingCode + ")";
                customerMsg = "Vui lòng thanh toán cọc trong vòng 15 phút để hoàn tất giữ chỗ tại " + storeName + ".";
            }
            default -> {
                log.info("Booking event {} not requiring notification dispatch.", eventType);
                return;
            }
        }

        // 1. Gửi thông báo đến Khách hàng cá nhân
        if (customerUserId != null && customerTitle != null) {
            try {
                notificationDispatcherService.dispatch(
                        customerUserId,
                        customerTitle,
                        customerMsg,
                        notifType,
                        referenceId,
                        referenceId != null ? "/bookings/" + referenceId : "/bookings"
                );
                log.info("Dispatched booking notification to customer: userId={}, type={}, bookingCode={}",
                        customerUserId, notifType, bookingCode);
            } catch (Exception e) {
                log.error("Failed to dispatch booking notification to customer {}: {}", customerUserId, e.getMessage());
            }
        }

        // 2. Gửi thông báo đến Nhân sự của Quán
        if (storeId != null && staffTitle != null) {
            try {
                notificationDispatcherService.dispatchToStoreStaff(
                        storeId,
                        staffTitle,
                        staffMsg != null ? staffMsg : message,
                        notifType,
                        referenceId,
                        "/merchant/bookings?storeId=" + storeId,
                        customerUserId
                );
                log.info("Dispatched booking notification to store staff for store {}: type={}, bookingCode={}",
                        storeId, notifType, bookingCode);
            } catch (Exception e) {
                log.error("Failed to dispatch booking notification to store staff for store {}: {}", storeId, e.getMessage());
            }
        }
    }

    private void cachePayOSResponse(UUID bookingId, PayOSResponse payResponse) {
        if (bookingId == null || payResponse == null || redisTemplate == null) {
            return;
        }
        try {
            String key = "booking:payos:" + bookingId;
            String json = objectMapper.writeValueAsString(payResponse);
            redisTemplate.opsForValue().set(key, json, Duration.ofMinutes(15));
            log.info("Cached PayOS payment details in Redis for booking {} with TTL 15m", bookingId);
        } catch (Exception e) {
            log.warn("Failed to cache PayOS info in Redis for booking {}: {}", bookingId, e.getMessage());
        }
    }

    private PayOSResponse getCachedPayOSResponse(UUID bookingId) {
        if (bookingId == null || redisTemplate == null) {
            return null;
        }
        try {
            String key = "booking:payos:" + bookingId;
            String json = redisTemplate.opsForValue().get(key);
            if (json != null && !json.isBlank()) {
                return objectMapper.readValue(json, PayOSResponse.class);
            }
        } catch (Exception e) {
            log.warn("Failed to read cached PayOS info from Redis for booking {}: {}", bookingId, e.getMessage());
        }
        return null;
    }

    private void evictCachedPayOSResponse(UUID bookingId) {
        if (bookingId == null || redisTemplate == null) {
            return;
        }
        try {
            redisTemplate.delete("booking:payos:" + bookingId);
        } catch (Exception ignored) {}
    }
}
