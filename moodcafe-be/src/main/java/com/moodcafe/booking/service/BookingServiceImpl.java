package com.moodcafe.booking.service;

import com.moodcafe.auth.abstraction.service.CurrentUserService;
import com.moodcafe.auth.entity.User;
import com.moodcafe.booking.abstraction.repository.BookingRepository;
import com.moodcafe.booking.abstraction.repository.StoreBookingConfigRepository;
import com.moodcafe.booking.abstraction.repository.TableCategoryRepository;
import com.moodcafe.booking.abstraction.service.BookingService;
import com.moodcafe.notification.abstraction.service.NotificationDispatcherService;
import com.moodcafe.notification.entity.enums.NotificationType;
import com.moodcafe.booking.dto.request.CancelBookingRequest;
import com.moodcafe.booking.dto.request.CreateBookingRequest;
import com.moodcafe.booking.dto.request.DeskCheckInRequest;
import com.moodcafe.booking.dto.response.BookingDeskSummaryResponse;
import com.moodcafe.booking.dto.response.BookingResponse;
import com.moodcafe.booking.dto.response.CategoryScheduleDTO;
import com.moodcafe.booking.dto.response.SlotAvailabilityResponse;
import com.moodcafe.booking.dto.response.StoreDailyScheduleResponse;
import com.moodcafe.booking.dto.response.TimeSlotDTO;
import com.moodcafe.booking.entity.Booking;
import com.moodcafe.booking.entity.StoreBookingConfig;
import com.moodcafe.booking.entity.TableCategory;
import com.moodcafe.booking.entity.enums.BookingEventType;
import com.moodcafe.booking.entity.enums.BookingStatus;
import com.moodcafe.booking.entity.enums.SlotStatus;
import com.moodcafe.booking.mapper.BookingMapper;
import com.moodcafe.payment.dto.response.PayOSResponse;
import com.moodcafe.payment.abstraction.service.PayOSService;
import com.moodcafe.shared.error.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moodcafe.shared.exceptions.AppException;
import com.moodcafe.store.abstraction.repository.StoreImageRepository;
import com.moodcafe.store.abstraction.repository.StoreRepository;
import com.moodcafe.store.abstraction.service.StoreStaffService;
import com.moodcafe.store.entity.Store;
import com.moodcafe.store.entity.StoreImage;
import com.moodcafe.store.entity.enums.StoreStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.moodcafe.shared.response.PageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
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
                            .note("Quán hiện đang tạm tắt nhận đặt bàn")
                            .build());
                } else if (Boolean.TRUE.equals(config.getIsTimeLimitEnabled())) {
                    // Case 1: Quán BẬT giới hạn thời gian ngồi
                    int standardMins = config.getSittingDurationMins();
                    int bufferMins = config.getCleanupBufferMins();
                    int totalOccupiedMins = standardMins + bufferMins;
                    Instant standardEnd = slotStart.plus(Duration.ofMinutes(totalOccupiedMins));

                    Instant firstIntervalEnd = slotStart.plus(Duration.ofMinutes(30));
                    long occupiedAtStart = catBookings.stream()
                            .filter(b -> b.getStartTime().isBefore(firstIntervalEnd) && b.getEndTime().isAfter(slotStart))
                            .count();

                    int availableAtStart = Math.max(0, cat.getAppQuota() - (int) occupiedAtStart);

                    if (availableAtStart <= 0) {
                        timeSlots.add(TimeSlotDTO.builder()
                                .time(timeStr)
                                .startTime(slotStart)
                                .endTime(standardEnd)
                                .availableCount(0)
                                .occupiedCount(cat.getAppQuota())
                                .status(SlotStatus.FULL)
                                .availableDurationMins(0)
                                .note("Đã kín chỗ trong khung giờ này")
                                .build());
                    } else {
                        // Kiểm tra điểm nghẽn (blockage) trong khoảng [slotStart, standardEnd]
                        Instant earliestBlockage = null;
                        for (int m = 15; m < totalOccupiedMins; m += 15) {
                            Instant checkPoint = slotStart.plus(Duration.ofMinutes(m));
                            long occupiedAtPoint = catBookings.stream()
                                    .filter(b -> b.getStartTime().isBefore(checkPoint.plusSeconds(1)) && b.getEndTime().isAfter(checkPoint))
                                    .count();
                            if (occupiedAtPoint >= cat.getAppQuota()) {
                                earliestBlockage = checkPoint;
                                break;
                            }
                        }

                        if (earliestBlockage != null) {
                            long rawDiffMins = ChronoUnit.MINUTES.between(slotStart, earliestBlockage);
                            int availableMins = Math.max(0, (int) rawDiffMins - bufferMins);
                            String nextBookingStr = earliestBlockage.atZone(VN_ZONE).toLocalTime().format(timeFormatter);

                            if (availableMins < 45) {
                                timeSlots.add(TimeSlotDTO.builder()
                                        .time(timeStr)
                                        .startTime(slotStart)
                                        .endTime(earliestBlockage)
                                        .availableCount(0)
                                        .occupiedCount(cat.getAppQuota())
                                        .status(SlotStatus.FULL)
                                        .availableDurationMins(availableMins)
                                        .nextBookingTime(nextBookingStr)
                                        .note("Khoảng trống chỉ còn " + availableMins + " phút, không đủ thời gian phục vụ tối thiểu")
                                        .build());
                            } else {
                                timeSlots.add(TimeSlotDTO.builder()
                                        .time(timeStr)
                                        .startTime(slotStart)
                                        .endTime(earliestBlockage)
                                        .availableCount(availableAtStart)
                                        .occupiedCount((int) occupiedAtStart)
                                        .status(SlotStatus.LIMITED_TIME)
                                        .availableDurationMins(availableMins)
                                        .nextBookingTime(nextBookingStr)
                                        .note("Chỉ còn trống " + availableMins + " phút do ca sau có khách đặt lúc " + nextBookingStr)
                                        .build());
                            }
                        } else {
                            SlotStatus status = (availableAtStart == 1) ? SlotStatus.ALMOST_FULL : SlotStatus.AVAILABLE;
                            timeSlots.add(TimeSlotDTO.builder()
                                    .time(timeStr)
                                    .startTime(slotStart)
                                    .endTime(standardEnd)
                                    .availableCount(availableAtStart)
                                    .occupiedCount((int) occupiedAtStart)
                                    .status(status)
                                    .availableDurationMins(standardMins)
                                    .build());
                        }
                    }
                } else {
                    // Case 2: Quán TẮT giới hạn thời gian ngồi
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
    public BookingResponse createBooking(CreateBookingRequest request) {
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

        Instant now = Instant.now();
        Instant minAdvance = now.plus(Duration.ofMinutes(config.getMinAdvanceMins()));
        if (request.getStartTime().isBefore(minAdvance)) {
            throw new AppException(ErrorCode.BOOKING_ADVANCE_TIME_INVALID);
        }

        // Khóa bi quan chống Race Condition khi nhiều khách cùng đặt bàn cuối
        TableCategory category = tableCategoryRepository.findByIdWithLock(request.getCategoryId())
                .orElseThrow(() -> new AppException(ErrorCode.TABLE_CATEGORY_NOT_FOUND));

        if (!Boolean.TRUE.equals(category.getIsActive())) {
            throw new AppException(ErrorCode.TABLE_CATEGORY_NOT_FOUND, "Loại bàn này đang tạm ngừng nhận khách");
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

        String bookingCode = "MC" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        String qrToken = UUID.randomUUID().toString();

        boolean requiresDeposit = category.getMinSpendAmount() != null && category.getMinSpendAmount() > 0;

        Booking booking = Booking.builder()
                .bookingCode(bookingCode)
                .store(store)
                .tableCategory(category)
                .user(currentUser)
                .customerName(request.getCustomerName().trim())
                .customerPhone(request.getCustomerPhone().trim())
                .customerEmail(request.getCustomerEmail() != null ? request.getCustomerEmail().trim() : currentUser.getEmail())
                .startTime(request.getStartTime())
                .endTime(endTime)
                .status(requiresDeposit ? BookingStatus.PENDING_PAYMENT : BookingStatus.PAID_CONFIRMED)
                .minSpendAmount(category.getMinSpendAmount() != null ? category.getMinSpendAmount() : 0L)
                .qrCodeToken(qrToken)
                .note(request.getNote() != null ? request.getNote().trim() : null)
                .refundProcessed(false)
                .build();

        booking = bookingRepository.save(booking);

        // Tạo link thanh toán PayOS cho hạn mức đồ uống trả trước (F&B Credit) nếu có cọc
        PayOSResponse payResponse = null;
        if (requiresDeposit) {
            try {
                payResponse = payOSService.createPaymentLinkForBooking(booking);
                cachePayOSResponse(booking.getBookingId(), payResponse);
            } catch (Exception e) {
                log.error("Failed to create PayOS payment link for booking {}: {}", booking.getBookingCode(), e.getMessage());
            }
        }

        BookingResponse response = enrichBookingResponse(booking, config);
        if (payResponse != null) {
            response.setCheckoutUrl(payResponse.getCheckoutUrl());
            response.setPaymentQrCode(payResponse.getQrCode());
            response.setPaymentAccountNumber(payResponse.getAccountNumber());
            response.setPaymentAccountName(payResponse.getAccountName());
            response.setPaymentBin(payResponse.getBin());
            response.setPaymentDescription(payResponse.getDescription());
        }

        BookingEventType eventType = requiresDeposit ? BookingEventType.CREATED : BookingEventType.PAID_CONFIRMED;
        String eventMsg = requiresDeposit
                ? "Đơn đặt bàn mới (" + booking.getBookingCode() + ") đang chờ thanh toán cọc."
                : "Đơn đặt bàn mới (" + booking.getBookingCode() + ") đã được xác nhận tự động.";
        dispatchBookingNotification(booking, eventType, eventMsg);

        return response;
    }

    @Override
    @Transactional
    public void confirmBookingPayment(Long paymentOrderCode) {
        Booking booking = bookingRepository.findByPaymentOrderCode(paymentOrderCode)
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));

        if (booking.getStatus() == BookingStatus.PENDING_PAYMENT) {
            booking.setStatus(BookingStatus.PAID_CONFIRMED);
            bookingRepository.save(booking);
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
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));

        booking = reconcileBookingStatus(booking);
        StoreBookingConfig config = getOrCreateConfig(booking.getStore());
        return enrichBookingResponse(booking, config);
    }

    @Override
    @Transactional
    public BookingResponse getBookingByCode(String bookingCode) {
        Booking booking = bookingRepository.findByBookingCode(bookingCode.trim().toUpperCase())
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));

        booking = reconcileBookingStatus(booking);
        StoreBookingConfig config = getOrCreateConfig(booking.getStore());
        return enrichBookingResponse(booking, config);
    }

    @Override
    @Transactional
    public BookingResponse cancelBooking(UUID bookingId, CancelBookingRequest request) {
        User currentUser = currentUserService.getCurrentUser();
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));

        if (!booking.getUser().getUserId().equals(currentUser.getUserId())) {
            throw new AppException(ErrorCode.FORBIDDEN_ACTION, "Bạn không thể hủy đơn đặt của người khác");
        }

        if (booking.getStatus() != BookingStatus.PAID_CONFIRMED && booking.getStatus() != BookingStatus.PENDING_PAYMENT) {
            throw new AppException(ErrorCode.CANNOT_CANCEL_BOOKING, "Trạng thái đơn hiện tại không thể hủy");
        }

        StoreBookingConfig config = getOrCreateConfig(booking.getStore());
        Instant cancelDeadline = booking.getStartTime().minus(Duration.ofHours(config.getFreeCancelHours()));
        if (Instant.now().isAfter(cancelDeadline)) {
            throw new AppException(ErrorCode.CANNOT_CANCEL_BOOKING,
                    "Đã quá thời hạn hủy đơn miễn phí (trước giờ hẹn " + config.getFreeCancelHours() + " tiếng)");
        }

        booking.setStatus(BookingStatus.CANCELLED_REFUNDED);
        booking.setCancelledAt(Instant.now());
        booking.setCancelReason(request.getReason().trim());
        booking.setRefundProcessed(false); // Lưu log để chủ quán/admin chuyển khoản hoàn tiền thủ công

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

    @Override
    @Transactional
    public BookingResponse getBookingPaymentLink(UUID bookingId) {
        User currentUser = currentUserService.getCurrentUser();
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));

        if (!booking.getUser().getUserId().equals(currentUser.getUserId())) {
            throw new AppException(ErrorCode.FORBIDDEN_ACTION, "Bạn không thể thanh toán đơn đặt của người khác");
        }

        if (booking.getStatus() != BookingStatus.PENDING_PAYMENT) {
            throw new AppException(ErrorCode.INVALID_BOOKING_STATUS_TRANSITION, "Đơn đặt bàn này không ở trạng thái chờ thanh toán");
        }

        StoreBookingConfig config = getOrCreateConfig(booking.getStore());

        PayOSResponse payResponse = getCachedPayOSResponse(booking.getBookingId());
        if (payResponse == null) {
            payResponse = payOSService.createPaymentLinkForBooking(booking);
            cachePayOSResponse(booking.getBookingId(), payResponse);
        }

        BookingResponse res = enrichBookingResponse(booking, config);
        if (payResponse != null) {
            res.setCheckoutUrl(payResponse.getCheckoutUrl());
            res.setPaymentQrCode(payResponse.getQrCode());
            res.setPaymentAccountNumber(payResponse.getAccountNumber());
            res.setPaymentAccountName(payResponse.getAccountName());
            res.setPaymentBin(payResponse.getBin());
            res.setPaymentDescription(payResponse.getDescription());
        }
        return res;
    }

    @Override
    @Transactional
    public List<BookingResponse> getMyBookings() {
        User currentUser = currentUserService.getCurrentUser();
        List<Booking> bookings = bookingRepository.findAllByUserUserIdOrderByCreatedAtDesc(currentUser.getUserId());

        return bookings.stream()
                .map(this::reconcileBookingStatus)
                .map(b -> enrichBookingResponse(b, getOrCreateConfig(b.getStore())))
                .toList();
    }

    @Override
    @Transactional
    public PageResponse<BookingResponse> getMyBookings(Pageable pageable) {
        User currentUser = currentUserService.getCurrentUser();
        Page<Booking> bookingPage = bookingRepository.findAllByUserUserIdOrderByCreatedAtDesc(currentUser.getUserId(), pageable);

        List<BookingResponse> items = bookingPage.getContent().stream()
                .map(this::reconcileBookingStatus)
                .map(b -> enrichBookingResponse(b, getOrCreateConfig(b.getStore())))
                .toList();

        return PageResponse.<BookingResponse>builder()
                .items(items)
                .page(bookingPage.getNumber())
                .size(bookingPage.getSize())
                .totalElements(bookingPage.getTotalElements())
                .totalPages(bookingPage.getTotalPages())
                .last(bookingPage.isLast())
                .build();
    }

    @Override
    @Transactional
    public List<BookingResponse> getStoreDeskBookings(UUID storeId, LocalDate date, Instant from, Instant to) {
        storeStaffService.requireStoreAccess(storeId, "OWNER", "MANAGER", "CASHIER");

        List<Booking> bookings;

        if (from != null && to != null) {
            bookings = bookingRepository.findAllByStoreStoreIdAndStartTimeBetweenOrderByCreatedAtDesc(
                    storeId,
                    from,
                    to
            );
        } else if (from != null) {
            bookings = bookingRepository.findAllByStoreStoreIdAndStartTimeBetweenOrderByCreatedAtDesc(
                    storeId,
                    from,
                    from.plus(Duration.ofDays(30))
            );
        } else if (to != null) {
            bookings = bookingRepository.findAllByStoreStoreIdAndStartTimeBetweenOrderByCreatedAtDesc(
                    storeId,
                    to.minus(Duration.ofDays(30)),
                    to
            );
        } else if (date != null) {
            ZonedDateTime startOfDay = date.atStartOfDay(VN_ZONE);
            ZonedDateTime endOfDay = date.atTime(LocalTime.MAX).atZone(VN_ZONE);
            bookings = bookingRepository.findAllByStoreStoreIdAndStartTimeBetweenOrderByCreatedAtDesc(
                    storeId,
                    startOfDay.toInstant(),
                    endOfDay.toInstant()
            );
        } else {
            // Mặc định lấy đơn của ngày hôm nay (giờ VN), sắp xếp mới nhất
            LocalDate today = LocalDate.now(VN_ZONE);
            ZonedDateTime startOfDay = today.atStartOfDay(VN_ZONE);
            ZonedDateTime endOfDay = today.atTime(LocalTime.MAX).atZone(VN_ZONE);
            bookings = bookingRepository.findAllByStoreStoreIdAndStartTimeBetweenOrderByCreatedAtDesc(
                    storeId,
                    startOfDay.toInstant(),
                    endOfDay.toInstant()
            );
        }

        StoreBookingConfig config = getOrCreateConfig(storeRepository.getReferenceById(storeId));
        return bookings.stream()
                .map(this::reconcileBookingStatus)
                .map(b -> enrichBookingResponse(b, config))
                .toList();
    }

    @Override
    @Transactional
    public PageResponse<BookingResponse> getStoreDeskBookings(UUID storeId, LocalDate date, Instant from, Instant to, Pageable pageable) {
        storeStaffService.requireStoreAccess(storeId, "OWNER", "MANAGER", "CASHIER");

        Page<Booking> bookingPage;

        if (from != null && to != null) {
            bookingPage = bookingRepository.findAllByStoreStoreIdAndStartTimeBetweenOrderByCreatedAtDesc(
                    storeId,
                    from,
                    to,
                    pageable
            );
        } else if (from != null) {
            bookingPage = bookingRepository.findAllByStoreStoreIdAndStartTimeBetweenOrderByCreatedAtDesc(
                    storeId,
                    from,
                    from.plus(Duration.ofDays(30)),
                    pageable
            );
        } else if (to != null) {
            bookingPage = bookingRepository.findAllByStoreStoreIdAndStartTimeBetweenOrderByCreatedAtDesc(
                    storeId,
                    to.minus(Duration.ofDays(30)),
                    to,
                    pageable
            );
        } else if (date != null) {
            ZonedDateTime startOfDay = date.atStartOfDay(VN_ZONE);
            ZonedDateTime endOfDay = date.atTime(LocalTime.MAX).atZone(VN_ZONE);
            bookingPage = bookingRepository.findAllByStoreStoreIdAndStartTimeBetweenOrderByCreatedAtDesc(
                    storeId,
                    startOfDay.toInstant(),
                    endOfDay.toInstant(),
                    pageable
            );
        } else {
            // Mặc định lấy đơn của ngày hôm nay (giờ VN), sắp xếp mới nhất
            LocalDate today = LocalDate.now(VN_ZONE);
            ZonedDateTime startOfDay = today.atStartOfDay(VN_ZONE);
            ZonedDateTime endOfDay = today.atTime(LocalTime.MAX).atZone(VN_ZONE);
            bookingPage = bookingRepository.findAllByStoreStoreIdAndStartTimeBetweenOrderByCreatedAtDesc(
                    storeId,
                    startOfDay.toInstant(),
                    endOfDay.toInstant(),
                    pageable
            );
        }

        StoreBookingConfig config = getOrCreateConfig(storeRepository.getReferenceById(storeId));
        List<BookingResponse> items = bookingPage.getContent().stream()
                .map(this::reconcileBookingStatus)
                .map(b -> enrichBookingResponse(b, config))
                .toList();

        return PageResponse.<BookingResponse>builder()
                .items(items)
                .page(bookingPage.getNumber())
                .size(bookingPage.getSize())
                .totalElements(bookingPage.getTotalElements())
                .totalPages(bookingPage.getTotalPages())
                .last(bookingPage.isLast())
                .build();
    }

    @Override
    @Transactional
    public BookingDeskSummaryResponse getStoreDeskSummary(UUID storeId, LocalDate date) {
        storeStaffService.requireStoreAccess(storeId, "OWNER", "MANAGER", "CASHIER");

        LocalDate queryDate = date != null ? date : LocalDate.now(VN_ZONE);
        ZonedDateTime startOfDay = queryDate.atStartOfDay(VN_ZONE);
        ZonedDateTime endOfDay = queryDate.atTime(LocalTime.MAX).atZone(VN_ZONE);

        List<Booking> bookings = bookingRepository.findAllByStoreStoreIdAndStartTimeBetweenOrderByStartTimeAsc(
                storeId,
                startOfDay.toInstant(),
                endOfDay.toInstant()
        ).stream().map(this::reconcileBookingStatus).toList();

        long pendingArrival = bookings.stream().filter(b -> b.getStatus() == BookingStatus.PAID_CONFIRMED).count();
        long checkedIn = bookings.stream().filter(b -> b.getStatus() == BookingStatus.CHECKED_IN).count();
        long tableExpired = bookings.stream().filter(b -> b.getStatus() == BookingStatus.TABLE_EXPIRED).count();
        long completed = bookings.stream().filter(b -> b.getStatus() == BookingStatus.COMPLETED).count();
        long claimedLate = bookings.stream().filter(b -> b.getStatus() == BookingStatus.CLAIMED_LATE).count();

        List<TableCategory> categories = tableCategoryRepository.findAllByStoreStoreIdAndIsActiveTrue(storeId);
        long totalAppQuota = categories.stream().mapToInt(TableCategory::getAppQuota).sum();

        return BookingDeskSummaryResponse.builder()
                .totalTodayBookings(bookings.size())
                .pendingArrivalCount(pendingArrival)
                .checkedInCount(checkedIn)
                .tableExpiredCount(tableExpired)
                .completedCount(completed)
                .claimedLateCount(claimedLate)
                .totalAppQuota(totalAppQuota)
                .build();
    }

    @Override
    @Transactional
    public BookingResponse deskCheckIn(UUID storeId, DeskCheckInRequest request) {
        storeStaffService.requireStoreAccess(storeId, "OWNER", "MANAGER", "CASHIER");

        String code = request.getCodeOrToken().trim().toUpperCase();
        Booking booking = bookingRepository.findByBookingCode(code)
                .or(() -> bookingRepository.findAll().stream()
                        .filter(b -> request.getCodeOrToken().trim().equalsIgnoreCase(b.getQrCodeToken()))
                        .findFirst())
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));

        if (!booking.getStore().getStoreId().equals(storeId)) {
            throw new AppException(ErrorCode.BOOKING_STORE_MISMATCH);
        }

        booking = reconcileBookingStatus(booking);

        if (booking.getStatus() == BookingStatus.EXPIRED_NO_SHOW) {
            throw new AppException(ErrorCode.INVALID_BOOKING_STATUS_TRANSITION,
                    "Đơn đặt bàn này đã hết hạn hiệu lực hoàn toàn (EXPIRED_NO_SHOW).");
        }

        if (booking.getStatus() == BookingStatus.TABLE_EXPIRED) {
            throw new AppException(ErrorCode.INVALID_BOOKING_STATUS_TRANSITION,
                    "Đơn này đã quá giờ giữ bàn và bị thu hồi slot (TABLE_EXPIRED). Vui lòng dùng chức năng 'Xác nhận giao nước trễ' tại quầy.");
        }

        if (booking.getStatus() != BookingStatus.PAID_CONFIRMED) {
            throw new AppException(ErrorCode.INVALID_BOOKING_STATUS_TRANSITION,
                    "Không thể check-in đơn ở trạng thái " + booking.getStatus());
        }

        booking.setStatus(BookingStatus.CHECKED_IN);
        booking.setCheckedInAt(Instant.now());
        booking = bookingRepository.save(booking);

        StoreBookingConfig config = getOrCreateConfig(booking.getStore());
        BookingResponse response = enrichBookingResponse(booking, config);

        dispatchBookingNotification(
                booking,
                BookingEventType.CHECKED_IN,
                "Khách " + booking.getCustomerName() + " đã check-in vào bàn thành công."
        );
        return response;
    }

    @Override
    @Transactional
    public BookingResponse deskClaimLate(UUID storeId, UUID bookingId) {
        storeStaffService.requireStoreAccess(storeId, "OWNER", "MANAGER", "CASHIER");

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));

        if (!booking.getStore().getStoreId().equals(storeId)) {
            throw new AppException(ErrorCode.BOOKING_STORE_MISMATCH);
        }

        booking = reconcileBookingStatus(booking);

        if (booking.getStatus() == BookingStatus.EXPIRED_NO_SHOW) {
            throw new AppException(ErrorCode.INVALID_BOOKING_STATUS_TRANSITION,
                    "Đơn đặt bàn này đã quá thời hạn bảo lưu nước (kết thúc ngày tại quán). Không thể nhận nước trễ.");
        }

        if (booking.getStatus() != BookingStatus.TABLE_EXPIRED) {
            throw new AppException(ErrorCode.INVALID_BOOKING_STATUS_TRANSITION,
                    "Chỉ có đơn quá giờ giữ bàn (TABLE_EXPIRED) mới có thể xác nhận đổi nước trễ");
        }

        booking.setStatus(BookingStatus.CLAIMED_LATE);
        booking.setClaimedLateAt(Instant.now());
        booking = bookingRepository.save(booking);

        StoreBookingConfig config = getOrCreateConfig(booking.getStore());
        BookingResponse response = enrichBookingResponse(booking, config);

        dispatchBookingNotification(
                booking,
                BookingEventType.CLAIMED_LATE,
                "Đã xác nhận giao nước trễ cho khách " + booking.getCustomerName()
        );
        return response;
    }

    @Override
    @Transactional
    public BookingResponse deskComplete(UUID storeId, UUID bookingId) {
        storeStaffService.requireStoreAccess(storeId, "OWNER", "MANAGER", "CASHIER");

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));

        if (!booking.getStore().getStoreId().equals(storeId)) {
            throw new AppException(ErrorCode.BOOKING_STORE_MISMATCH);
        }

        if (booking.getStatus() != BookingStatus.CHECKED_IN) {
            throw new AppException(ErrorCode.INVALID_BOOKING_STATUS_TRANSITION,
                    "Chỉ có đơn đang ngồi (CHECKED_IN) mới có thể hoàn tất trả bàn");
        }

        booking.setStatus(BookingStatus.COMPLETED);
        booking.setCompletedAt(Instant.now());
        booking = bookingRepository.save(booking);

        StoreBookingConfig config = getOrCreateConfig(booking.getStore());
        BookingResponse response = enrichBookingResponse(booking, config);

        dispatchBookingNotification(
                booking,
                BookingEventType.COMPLETED,
                "Đơn đặt bàn " + booking.getBookingCode() + " đã hoàn tất và trả bàn."
        );
        return response;
    }

    // ==================== Private Helpers ====================

    private StoreBookingConfig getOrCreateConfig(Store store) {
        return configRepository.findByStoreStoreId(store.getStoreId())
                .orElseGet(() -> configRepository.save(
                        StoreBookingConfig.builder()
                                .store(store)
                                .isBookingEnabled(false)
                                .isTimeLimitEnabled(true)
                                .sittingDurationMins(120)
                                .cleanupBufferMins(15)
                                .gracePeriodMins(30)
                                .minAdvanceMins(30)
                                .freeCancelHours(2)
                                .build()
                ));
    }

    private Instant calculateEndTime(Instant startTime, StoreBookingConfig config, Store store, TableCategory category) {
        if (Boolean.TRUE.equals(config.getIsTimeLimitEnabled())) {
            int totalMins = config.getSittingDurationMins() + config.getCleanupBufferMins();
            Instant standardEnd = startTime.plus(Duration.ofMinutes(totalMins));

            if (category != null) {
                // Kiểm tra xem có đơn nào phía sau làm kín bàn trước standardEnd không
                for (int m = 15; m < totalMins; m += 15) {
                    Instant checkPoint = startTime.plus(Duration.ofMinutes(m));
                    long count = bookingRepository.countOverlappingBookings(
                            category.getCategoryId(),
                            OCCUPIED_STATUSES,
                            checkPoint,
                            checkPoint.plusSeconds(1)
                    );
                    if (count >= category.getAppQuota()) {
                        long availableMins = ChronoUnit.MINUTES.between(startTime, checkPoint) - config.getCleanupBufferMins();
                        if (availableMins < 45) {
                            throw new AppException(ErrorCode.NO_TABLE_AVAILABLE, "Khoảng trống chỉ còn " + availableMins + " phút, không đủ thời gian phục vụ tối thiểu");
                        }
                        return checkPoint;
                    }
                }
            }

            return standardEnd;
        }

        // Nếu quán tắt giới hạn thời gian: chiếm slot đến giờ đóng cửa của quán trong ngày đó
        LocalTime closingTime = store.getClosingTime() != null ? store.getClosingTime() : LocalTime.of(23, 0);
        ZonedDateTime startZoned = startTime.atZone(VN_ZONE);
        ZonedDateTime closingZoned = startZoned.with(closingTime);

        if (closingZoned.isBefore(startZoned)) {
            closingZoned = closingZoned.plusDays(1);
        }

        return closingZoned.toInstant();
    }

    @Transactional
    public Booking reconcileBookingStatus(Booking booking) {
        if (booking == null || booking.getStatus() == null) {
            return booking;
        }

        Instant now = Instant.now();
        LocalDate bookingDate = booking.getStartTime().atZone(VN_ZONE).toLocalDate();
        Store store = booking.getStore();
        LocalTime closingTime = (store != null && store.getClosingTime() != null)
                ? store.getClosingTime()
                : LocalTime.of(23, 0);
        Instant closingInstant = bookingDate.atTime(closingTime).atZone(VN_ZONE).toInstant();

        if (booking.getStatus() == BookingStatus.PAID_CONFIRMED) {
            StoreBookingConfig config = getOrCreateConfig(booking.getStore());
            int graceMins = config != null && config.getGracePeriodMins() != null ? config.getGracePeriodMins() : 30;
            Instant graceExpiry = booking.getStartTime().plus(Duration.ofMinutes(graceMins));

            if (now.isAfter(graceExpiry)) {
                if (now.isAfter(closingInstant)) {
                    booking.setStatus(BookingStatus.EXPIRED_NO_SHOW);
                    booking = bookingRepository.save(booking);
                    log.info("Reconciled booking {} from PAID_CONFIRMED to EXPIRED_NO_SHOW (past store closing deadline)", booking.getBookingCode());
                } else {
                    booking.setStatus(BookingStatus.TABLE_EXPIRED);
                    booking = bookingRepository.save(booking);
                    log.info("Reconciled booking {} from PAID_CONFIRMED to TABLE_EXPIRED (past grace period)", booking.getBookingCode());
                }
            }
        } else if (booking.getStatus() == BookingStatus.TABLE_EXPIRED) {
            if (now.isAfter(closingInstant)) {
                booking.setStatus(BookingStatus.EXPIRED_NO_SHOW);
                booking = bookingRepository.save(booking);
                log.info("Reconciled booking {} from TABLE_EXPIRED to EXPIRED_NO_SHOW (past store closing deadline)", booking.getBookingCode());
            }
        }

        return booking;
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

        boolean cancelable = (booking.getStatus() == BookingStatus.PAID_CONFIRMED || booking.getStatus() == BookingStatus.PENDING_PAYMENT)
                && Instant.now().isBefore(booking.getStartTime().minus(Duration.ofHours(config.getFreeCancelHours())));
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
                        "/customer/bookings"
                );
                log.info("Dispatched booking notification to customer: userId={}, type={}, bookingCode={}",
                        customerUserId, notifType, bookingCode);
            } catch (Exception e) {
                log.error("Failed to dispatch booking notification to customer {}: {}", customerUserId, e.getMessage());
            }
        }

        // 2. Gửi thông báo đến Nhân sự của Quán qua dispatchToStoreStaff dùng chung
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
