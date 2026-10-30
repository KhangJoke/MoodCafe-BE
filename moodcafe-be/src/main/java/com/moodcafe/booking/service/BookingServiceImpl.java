package com.moodcafe.booking.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moodcafe.auth.abstraction.service.CurrentUserService;
import com.moodcafe.auth.entity.User;
import com.moodcafe.booking.abstraction.repository.BookingRepository;
import com.moodcafe.booking.abstraction.repository.StoreBookingConfigRepository;
import com.moodcafe.booking.abstraction.repository.TableCategoryRepository;
import com.moodcafe.booking.abstraction.service.BookingService;
import com.moodcafe.booking.dto.request.CancelBookingRequest;
import com.moodcafe.booking.dto.request.CreateBookingRequest;
import com.moodcafe.booking.dto.request.DeskCheckInRequest;
import com.moodcafe.booking.dto.response.BookingDeskSummaryResponse;
import com.moodcafe.booking.dto.response.BookingResponse;
import com.moodcafe.booking.enricher.BookingResponseEnricher;
import com.moodcafe.booking.dto.response.CategoryScheduleDTO;
import com.moodcafe.booking.dto.response.RefundOutcomeResponse;
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
import com.moodcafe.finance.dto.ReservationFinancialSnapshot;
import com.moodcafe.finance.dto.ReservationPricingInput;
import com.moodcafe.finance.service.BookingCommissionService;
import com.moodcafe.finance.service.ReservationPricingService;
import com.moodcafe.loyalty.service.LoyaltyRewardService;
import com.moodcafe.notification.abstraction.service.NotificationDispatcherService;
import com.moodcafe.notification.entity.enums.NotificationType;
import com.moodcafe.payment.abstraction.service.PaymentService;
import com.moodcafe.payment.dto.response.PayOSResponse;
import vn.payos.model.v2.paymentRequests.PaymentLink;
import vn.payos.model.v2.paymentRequests.PaymentLinkStatus;
import com.moodcafe.shared.error.ErrorCode;
import com.moodcafe.shared.exceptions.AppException;
import com.moodcafe.shared.response.PageResponse;
import com.moodcafe.store.abstraction.repository.StoreImageRepository;
import com.moodcafe.store.abstraction.repository.StoreRepository;
import com.moodcafe.store.abstraction.service.StoreStaffService;
import com.moodcafe.store.entity.Store;
import com.moodcafe.store.entity.StoreImage;
import com.moodcafe.store.entity.enums.StoreStatus;
import com.moodcafe.voucher.abstraction.repository.CustomerVoucherRepository;
import com.moodcafe.voucher.abstraction.repository.VoucherPointAllocationRepository;
import com.moodcafe.voucher.entity.CustomerVoucher;
import com.moodcafe.voucher.entity.VoucherPointAllocation;
import com.moodcafe.voucher.service.VoucherLifecycleService;
import com.moodcafe.loyalty.dto.RefundPointCommand;
import com.moodcafe.loyalty.service.CustomerLoyaltyService;
import com.moodcafe.wallet.service.MerchantWalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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
import com.moodcafe.gamification.abstraction.service.CustomerTitleService;
import com.moodcafe.gamification.abstraction.service.QuestEventService;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final Set<BookingStatus> OCCUPIED_STATUSES = EnumSet.of(
            BookingStatus.PENDING_PAYMENT,
            BookingStatus.PAID_CONFIRMED,
            BookingStatus.CHECKED_IN,
            BookingStatus.CHECK_OUT_PENDING,
            BookingStatus.WALK_IN_HOLD
    );

    private final BookingCommissionService commissionService;
    private final BookingRepository bookingRepository;
    private final TableCategoryRepository tableCategoryRepository;
    private final StoreBookingConfigRepository configRepository;
    private final StoreRepository storeRepository;
    private final StoreStaffService storeStaffService;
    private final CurrentUserService currentUserService;
    private final PaymentService paymentService;
    private final BookingMapper bookingMapper;
    private final StoreImageRepository storeImageRepository;
    private final NotificationDispatcherService notificationDispatcherService;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final CustomerVoucherRepository customerVoucherRepository;
    private final VoucherPointAllocationRepository voucherPointAllocationRepository;
    private final ReservationPricingService reservationPricingService;
    private final MerchantWalletService merchantWalletService;
    private final CustomerLoyaltyService customerLoyaltyService;
    private final LoyaltyRewardService loyaltyRewardService;
    private final VoucherLifecycleService voucherLifecycleService;
    private final PlatformTransactionManager transactionManager;

    @Autowired(required = false)
    private QuestEventService questEventService;

    @Autowired(required = false)
    private CustomerTitleService customerTitleService;

    @Autowired(required = false)
    private BookingResponseEnricher bookingResponseEnricher;

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
                    timeSlots.add(buildBlockedTimeSlot(timeStr, slotStart, calculateEndTime(slotStart, config, store, cat), cat.getAppQuota(), SlotStatus.PAST, null));
                } else if (!isBookingEnabled) {
                    timeSlots.add(buildBlockedTimeSlot(timeStr, slotStart, calculateEndTime(slotStart, config, store, cat), cat.getAppQuota(), SlotStatus.FULL, "Quán hiện đang tạm tắt nhận đặt bàn"));
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
                        timeSlots.add(buildBlockedTimeSlot(timeStr, slotStart, standardEnd, cat.getAppQuota(), SlotStatus.FULL, "Đã kín chỗ trong khung giờ này"));
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
                                timeSlots.add(buildTimeSlot(timeStr, slotStart, earliestBlockage, 0, cat.getAppQuota(),
                                        SlotStatus.FULL, availableMins,
                                        "Khoảng trống chỉ còn " + availableMins + " phút, không đủ thời gian phục vụ tối thiểu", nextBookingStr));
                            } else {
                                timeSlots.add(buildTimeSlot(timeStr, slotStart, earliestBlockage, availableAtStart, (int) occupiedAtStart,
                                        SlotStatus.LIMITED_TIME, availableMins,
                                        "Chỉ còn trống " + availableMins + " phút do ca sau có khách đặt lúc " + nextBookingStr, nextBookingStr));
                            }
                        } else {
                            SlotStatus status = (availableAtStart == 1) ? SlotStatus.ALMOST_FULL : SlotStatus.AVAILABLE;
                            timeSlots.add(buildTimeSlot(timeStr, slotStart, standardEnd, availableAtStart, (int) occupiedAtStart,
                                    status, standardMins, null, null));
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
                        timeSlots.add(buildBlockedTimeSlot(timeStr, slotStart, closingInstant, cat.getAppQuota(), SlotStatus.FULL, "Đã kín chỗ đến giờ đóng cửa"));
                    } else {
                        SlotStatus status = (available == 1) ? SlotStatus.ALMOST_FULL : SlotStatus.AVAILABLE;
                        timeSlots.add(buildTimeSlot(timeStr, slotStart, closingInstant, available, (int) occupied,
                                status, remainingMins, null, null));
                    }
                }

                currentSlotTime = currentSlotTime.plusMinutes(30);
            }

            categoryDTOs.add(bookingMapper.toCategoryScheduleDTO(cat, timeSlots));
        }

        return bookingMapper.toStoreDailyScheduleResponse(store, targetDate, config, isBookingEnabled,
                openingTime.format(timeFormatter), closingTime.format(timeFormatter), categoryDTOs);
    }

    private TimeSlotDTO buildTimeSlot(String timeStr, Instant startTime, Instant endTime,
                                      int availableCount, int occupiedCount, SlotStatus status,
                                      int availableDurationMins, String note, String nextBookingTime) {
        return TimeSlotDTO.builder()
                .time(timeStr)
                .startTime(startTime)
                .endTime(endTime)
                .availableCount(availableCount)
                .occupiedCount(occupiedCount)
                .status(status)
                .availableDurationMins(availableDurationMins)
                .note(note)
                .nextBookingTime(nextBookingTime)
                .build();
    }

    private TimeSlotDTO buildBlockedTimeSlot(String timeStr, Instant startTime, Instant endTime,
                                             int quota, SlotStatus status, String note) {
        return buildTimeSlot(timeStr, startTime, endTime, 0, quota, status, 0, note, null);
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

        return bookingMapper.toSlotAvailabilityResponse(category, startTime, endTime, totalQuota, (int) occupied, available);
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
    public BookingResponse createBooking(CreateBookingRequest request) {
        Booking savedBooking = inTransaction(() -> doCreateBooking(request));

        PayOSResponse payResponse = null;
        if (savedBooking.getStatus() == BookingStatus.PENDING_PAYMENT) {
            try {
                Long expiredAtEpoch = savedBooking.getPaymentExpiresAt() != null
                        ? savedBooking.getPaymentExpiresAt().getEpochSecond()
                        : (Instant.now().getEpochSecond() + 900);
                payResponse = paymentService.createBookingPaymentLink(
                        savedBooking.getBookingId(),
                        savedBooking.getPaymentOrderCode(),
                        savedBooking.getCustomerPaidAmount().longValueExact(),
                        "DATBAN " + savedBooking.getPaymentOrderCode(),
                        expiredAtEpoch
                );
                if (payResponse != null) {
                    cachePayOSResponse(savedBooking.getBookingId(), payResponse);
                }
            } catch (Exception e) {
                log.error("Failed to create PayOS payment link for booking {}: {}", savedBooking.getBookingCode(), e.getMessage());
            }
        }

        StoreBookingConfig config = getOrCreateConfig(savedBooking.getStore());
        BookingResponse response = enrichBookingResponse(savedBooking, config);
        if (payResponse != null) {
            response.setCheckoutUrl(payResponse.getCheckoutUrl());
            response.setPaymentQrCode(payResponse.getQrCode());
            response.setPaymentAccountNumber(payResponse.getAccountNumber());
            response.setPaymentAccountName(payResponse.getAccountName());
            response.setPaymentBin(payResponse.getBin());
            response.setPaymentDescription(payResponse.getDescription());
        }

        boolean requiresDeposit = savedBooking.getStatus() == BookingStatus.PENDING_PAYMENT;
        BookingEventType eventType = requiresDeposit ? BookingEventType.CREATED : BookingEventType.PAID_CONFIRMED;
        String eventMsg = requiresDeposit
                ? "Đơn đặt bàn mới (" + savedBooking.getBookingCode() + ") đang chờ thanh toán gói đồ uống trả trước."
                : "Đơn đặt bàn mới (" + savedBooking.getBookingCode() + ") đã được xác nhận tự động (Zero-Pay).";
        dispatchBookingNotification(savedBooking, eventType, eventMsg);

        return response;
    }

    private <T> T inTransaction(Supplier<T> action) {
        if (transactionManager == null) return action.get();
        return new TransactionTemplate(transactionManager).execute(status -> action.get());
    }

    private Booking doCreateBooking(CreateBookingRequest request) {
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

        ReservationFinancialSnapshot snapshot = resolveFinancialSnapshot(store, category, voucher);
        Deadlines deadlines = calculateDeadlines(store, config, request.getStartTime());
        long paymentOrderCode = paymentService.generateOrderCode();
        boolean requiresDeposit = snapshot.getCustomerPaidAmount().compareTo(BigDecimal.ZERO) > 0;

        String bookingCode = "MC" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        String qrToken = UUID.randomUUID().toString();

        Booking booking = Booking.builder()
                .bookingCode(bookingCode)
                .store(store)
                .tableCategory(category)
                .user(currentUser)
                .customerName(request.getCustomerName().trim())
                .customerPhone(request.getCustomerPhone().trim())
                .customerEmail(request.getCustomerEmail() != null ? request.getCustomerEmail().trim() : currentUser.getEmail())
                .guestCount(guestCount)
                .startTime(request.getStartTime())
                .endTime(endTime)
                .status(requiresDeposit ? BookingStatus.PENDING_PAYMENT : BookingStatus.PAID_CONFIRMED)
                .minSpendAmount(category.getMinSpendAmount() != null ? category.getMinSpendAmount() : 0L)
                .grossAmount(snapshot.getGrossAmount())
                .voucherDiscount(snapshot.getVoucherDiscount())
                .customerPaidAmount(snapshot.getCustomerPaidAmount())
                .commissionPolicyId(snapshot.getCommissionPolicyId())
                .commissionAmount(snapshot.getCommissionAmount())
                .merchantNetAmount(snapshot.getMerchantNetAmount())
                .voucher(voucher)
                .voucherRefundFundedAmount(snapshot.getVoucherRefundFundedAmount())
                .voucherRewardFundedAmount(snapshot.getVoucherRewardFundedAmount())
                .commissionRateSnapshot(snapshot.getCommissionRate())
                .actualPaidAmount(BigDecimal.ZERO)
                .paidAt(requiresDeposit ? null : Instant.now())
                .paymentOrderCode(paymentOrderCode)
                .paymentExpiresAt(Instant.now().plus(Duration.ofMinutes(15)))
                .freeCancelDeadline(deadlines.freeCancelDeadline())
                .graceDeadline(deadlines.graceDeadline())
                .serviceClosingDeadline(deadlines.serviceClosingDeadline())
                .qrCodeToken(qrToken)
                .note(request.getNote() != null ? request.getNote().trim() : null)
                .refundProcessed(false)
                .build();

        booking = bookingRepository.save(booking);

        if (voucher != null) {
            if (requiresDeposit) {
                voucher.setStatus("LOCKED");
                voucher.setLockedBookingId(booking.getBookingId());
                voucher.setHoldUntil(Instant.now().plus(Duration.ofMinutes(15)));
            } else {
                voucher.setStatus("USED");
                voucher.setLockedBookingId(booking.getBookingId());
                voucher.setUsedAt(Instant.now());
            }
            customerVoucherRepository.save(voucher);
        }

        if (!requiresDeposit) {
            if (commissionService.record(booking, "HOLD") && booking.getMerchantNetAmount() != null && booking.getMerchantNetAmount().compareTo(BigDecimal.ZERO) > 0) {
                if (merchantWalletService != null) {
                    merchantWalletService.holdPending(
                            booking.getStore().getStoreId(),
                            booking.getMerchantNetAmount(),
                            "BOOKING",
                            booking.getBookingId().toString(),
                            "Tạm giữ doanh thu gói đồ uống đặt bàn 100% voucher #" + booking.getBookingCode()
                    );
                }
            }
        }

        return booking;
    }

    private void creditBookingRefundPoints(Booking booking, long points, String businessKey, String description) {
        if (customerLoyaltyService != null && booking != null && booking.getUser() != null && points > 0) {
            customerLoyaltyService.creditRefundPoints(RefundPointCommand.builder()
                    .userId(booking.getUser().getUserId())
                    .points(points)
                    .refundId(booking.getBookingId() != null ? booking.getBookingId().toString() : null)
                    .businessKey(businessKey)
                    .description(description)
                    .build());
        }
    }

    @Override
    @Transactional
    public void confirmBookingPayment(Long paymentOrderCode) {
        confirmBookingPayment(paymentOrderCode, null);
    }

    @Override
    @Transactional
    public void confirmBookingPayment(Long paymentOrderCode, Long paidAmountVnd) {
        Booking booking = bookingRepository.findByPaymentOrderCodeForUpdate(paymentOrderCode)
                .or(() -> bookingRepository.findByPaymentOrderCode(paymentOrderCode))
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
                creditBookingRefundPoints(booking, delta.longValueExact(), businessKey,
                        "Hoàn phần tiền thực nhận tăng thêm đơn " + booking.getBookingCode());
                booking.setActualPaidAmount(cumulative);
                booking.setPaidAt(Instant.now());
                booking.setRefundProcessed(true);
                bookingRepository.save(booking);
            }
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
            if (commissionService.record(booking, "HOLD") && booking.getMerchantNetAmount() != null && booking.getMerchantNetAmount().compareTo(BigDecimal.ZERO) > 0) {
                if (merchantWalletService != null) merchantWalletService.holdPending(
                        booking.getStore().getStoreId(),
                        booking.getMerchantNetAmount(),
                        "BOOKING",
                        booking.getBookingId().toString(),
                        "Doanh thu gói đồ uống đặt bàn #" + booking.getBookingCode()
                );
            }

            log.info("Booking {} confirmed payment successfully", booking.getBookingCode());

            evictCachedPayOSResponse(booking.getBookingId());

            dispatchBookingNotification(
                    booking,
                    BookingEventType.PAID_CONFIRMED,
                    "Khách " + booking.getCustomerName() + " đã thanh toán gói đồ uống thành công cho đơn " + booking.getBookingCode()
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

        // Revert merchant pending balance and refund customer if booking was PAID_CONFIRMED
        if (booking.getStatus() == BookingStatus.PAID_CONFIRMED) {
            if (commissionService.record(booking, "REVERSE") && booking.getMerchantNetAmount() != null && booking.getMerchantNetAmount().compareTo(BigDecimal.ZERO) > 0) {
                if (merchantWalletService != null) {
                    merchantWalletService.revertPending(
                            booking.getStore().getStoreId(),
                            booking.getMerchantNetAmount(),
                            "BOOKING",
                            booking.getBookingId().toString(),
                            "Đảo hoàn doanh thu tạm giữ do khách hủy đơn #" + booking.getBookingCode()
                    );
                }
            }

            BigDecimal paid = booking.getActualPaidAmount() != null ? booking.getActualPaidAmount() : BigDecimal.ZERO;
            if (paid.compareTo(BigDecimal.ZERO) > 0) {
                creditBookingRefundPoints(booking, paid.longValue(), "REFUND_BOOKING_" + booking.getBookingId(),
                        "Hoàn gói đồ uống đơn đặt bàn " + booking.getBookingCode() + " thành MoodPoints (1:1)");
                booking.setRefundProcessed(true);
            }
        }

        booking.setStatus(BookingStatus.CANCELLED_REFUNDED);
        booking.setCancelledAt(Instant.now());
        if (request != null && request.getReason() != null) {
            booking.setCancelReason(request.getReason().trim());
        }

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
    public void timeoutPendingBooking(UUID bookingId) {
        Booking booking = bookingRepository.findByIdForUpdate(bookingId).orElse(null);
        if (booking == null || booking.getStatus() != BookingStatus.PENDING_PAYMENT) return;

        booking.setStatus(BookingStatus.PAYMENT_FAILED);
        if (booking.getVoucher() != null) {
            if (voucherLifecycleService != null) {
                voucherLifecycleService.restoreVoucherForRefund(bookingId, booking.getVoucher().getVoucherId());
            } else {
                CustomerVoucher v = customerVoucherRepository.findByIdWithLock(booking.getVoucher().getVoucherId()).orElse(null);
                if (v != null) {
                    v.setStatus("AVAILABLE");
                    v.setLockedBookingId(null);
                    v.setHoldUntil(null);
                    customerVoucherRepository.save(v);
                }
            }
        }
        bookingRepository.save(booking);
        evictCachedPayOSResponse(bookingId);

        dispatchBookingNotification(booking, BookingEventType.PAYMENT_FAILED, "Đơn đặt bàn đã hết hạn thanh toán");
    }

    @Override
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

        if (booking.getPaymentExpiresAt() != null && Instant.now().isAfter(booking.getPaymentExpiresAt())) {
            throw new AppException(ErrorCode.INVALID_BOOKING_STATUS_TRANSITION, "Đơn đặt bàn đã hết hạn thanh toán (quá 15 phút). Vui lòng tạo đơn mới.");
        }

        long remainingSeconds = booking.getPaymentExpiresAt() != null
                ? Duration.between(Instant.now(), booking.getPaymentExpiresAt()).getSeconds()
                : 900;
        if (remainingSeconds <= 15) {
            throw new AppException(ErrorCode.INVALID_BOOKING_STATUS_TRANSITION, "Thời gian giữ bàn sắp hết hạn. Vui lòng tạo đơn mới.");
        }

        StoreBookingConfig config = getOrCreateConfig(booking.getStore());

        Long currentOrderCode = booking.getPaymentOrderCode();
        PaymentLink linkInfo = null;
        if (currentOrderCode != null && currentOrderCode > 0) {
            try {
                linkInfo = paymentService.getPaymentLink(currentOrderCode);
            } catch (Exception e) {
                log.warn("Could not retrieve payment link status for orderCode {}: {}", currentOrderCode, e.getMessage());
            }
        }

        PayOSResponse payResponse = null;

        if (linkInfo != null && linkInfo.getStatus() == PaymentLinkStatus.PAID) {
            confirmBookingPayment(currentOrderCode, linkInfo.getAmountPaid());
            return enrichBookingResponse(booking, config);
        }

        boolean sessionIsAlive = linkInfo != null && linkInfo.getStatus() == PaymentLinkStatus.PENDING;

        if (sessionIsAlive) {
            payResponse = getCachedPayOSResponse(booking.getBookingId());
            if (payResponse == null) {
                String sanitizedDesc = "DATBAN " + currentOrderCode;
                double amount = (double) (booking.getCustomerPaidAmount() != null ? booking.getCustomerPaidAmount().longValue() : booking.getMinSpendAmount());
                payResponse = PayOSResponse.builder()
                        .checkoutUrl("https://pay.payos.vn/web/" + linkInfo.getId())
                        .orderCode(currentOrderCode)
                        .amount(amount)
                        .description(sanitizedDesc)
                        .status("PENDING")
                        .paymentLinkId(linkInfo.getId())
                        .build();
                cachePayOSResponse(booking.getBookingId(), payResponse);
            }
        } else {
            // Phiên cũ đã CANCELLED hoặc EXPIRED hoặc linkInfo == null -> Khởi tạo phiên thanh toán mới
            evictCachedPayOSResponse(booking.getBookingId());

            long newOrderCode = paymentService.generateOrderCode();
            booking.setPaymentOrderCode(newOrderCode);
            booking = bookingRepository.save(booking);

            Long expiredAtEpoch = booking.getPaymentExpiresAt() != null
                    ? booking.getPaymentExpiresAt().getEpochSecond()
                    : (Instant.now().getEpochSecond() + 900);

            try {
                payResponse = paymentService.createBookingPaymentLink(
                        booking.getBookingId(),
                        newOrderCode,
                        booking.getCustomerPaidAmount().longValueExact(),
                        "DATBAN " + newOrderCode,
                        expiredAtEpoch
                );
                if (payResponse != null) {
                    cachePayOSResponse(booking.getBookingId(), payResponse);
                }
            } catch (Exception e) {
                log.error("Failed to generate payment link for booking {}: {}", booking.getBookingCode(), e.getMessage());
                throw new AppException(ErrorCode.INTERNAL_ERROR, "Không thể tạo liên kết thanh toán PayOS: " + e.getMessage());
            }
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
        storeStaffService.requireStoreAccess(storeId, "OWNER", "MANAGER", "CASHIER");

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
        storeStaffService.requireStoreAccess(storeId, "OWNER", "MANAGER", "CASHIER");

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
        storeStaffService.requireStoreAccess(storeId, "OWNER", "MANAGER", "CASHIER");

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

        return bookingMapper.toDeskSummaryResponse(dayBookings.size(), confirmed, checkedIn, completed, tableExpired, claimedLate, totalQuota);
    }

    @Override
    @Transactional
    public BookingResponse deskCheckIn(UUID storeId, DeskCheckInRequest request) {
        User currentUser = currentUserService.getCurrentUser();
        storeStaffService.requireStoreAccess(storeId, "OWNER", "MANAGER", "CASHIER");

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

        booking = bookingRepository.findByIdForUpdate(booking.getBookingId()).orElseThrow();
        if (booking.getStatus() != BookingStatus.PAID_CONFIRMED) {
            throw new AppException(ErrorCode.INVALID_BOOKING_STATUS_TRANSITION);
        }
        StoreBookingConfig config = getOrCreateConfig(booking.getStore());
        Instant now = Instant.now();
        Instant grace = booking.getGraceDeadline() != null ? booking.getGraceDeadline()
                : booking.getStartTime().plus(Duration.ofMinutes(config.getGracePeriodMins()));

        boolean earlyEnabled = config.getIsEarlyCheckInEnabled() != null ? config.getIsEarlyCheckInEnabled() : true;
        int maxEarlyMins = (config.getMaxEarlyCheckInMins() != null && config.getMaxEarlyCheckInMins() > 0)
                ? config.getMaxEarlyCheckInMins() : 60;

        // 1. Kiểm tra nếu khách đến sớm hơn giờ hẹn
        if (now.isBefore(booking.getStartTime())) {
            if (!earlyEnabled) {
                throw new AppException(ErrorCode.BAD_REQUEST,
                        "Quán hiện không áp dụng nhận bàn sớm. Vui lòng quay lại vào đúng giờ hẹn");
            }
            if (now.isBefore(booking.getStartTime().minus(Duration.ofMinutes(maxEarlyMins)))) {
                throw new AppException(ErrorCode.BAD_REQUEST,
                        "Chưa đến thời gian nhận bàn (Quán chỉ hỗ trợ nhận bàn sớm tối đa trước " + maxEarlyMins + " phút so với giờ hẹn)");
            }
        }

        // 2. Kiểm tra nếu đến quá trễ (vượt quá grace period của quán)
        if (now.isAfter(grace)) {
            throw new AppException(ErrorCode.BAD_REQUEST,
                    "Đã quá thời gian nhận bàn quy định, vui lòng kiểm tra luồng nhận nước trễ (Claim Late)");
        }

        // 3. Time-Window Shift: Nếu check-in sớm, kiểm tra lịch bàn trống trước khi dịch chuyển
        if (now.isBefore(booking.getStartTime())) {
            Duration duration = Duration.between(booking.getStartTime(), booking.getEndTime());
            Instant shiftedStart = now;
            Instant shiftedEnd = now.plus(duration);

            TableCategory category = booking.getTableCategory();
            if (category != null) {
                long occupied = bookingRepository.countOverlappingBookingsExcluding(
                        category.getCategoryId(),
                        OCCUPIED_STATUSES,
                        booking.getBookingId(),
                        shiftedStart,
                        shiftedEnd
                );

                int quota = category.getAppQuota() != null ? category.getAppQuota() : 0;
                if (occupied >= quota) {
                    throw new AppException(ErrorCode.NO_TABLE_AVAILABLE,
                            "Không thể nhận bàn sớm do loại bàn này hiện đã kín chỗ trong khung giờ check-in sớm ("
                            + category.getName() + "). Vui lòng đợi đến giờ hẹn.");
                }
            }

            booking.setStartTime(shiftedStart);
            booking.setEndTime(shiftedEnd);
            booking.setIsEarlyCheckIn(true);
        }

        if (request.getPhysicalTableNumber() != null && !request.getPhysicalTableNumber().isBlank()) {
            booking.setPhysicalTableNumber(request.getPhysicalTableNumber().trim());
        }

        booking.setStatus(BookingStatus.CHECKED_IN);
        booking.setCheckedInAt(now);
        booking = bookingRepository.save(booking);

        // 1. Check-in Settlement: Giải ngân doanh thu về số dư khả dụng của Quán ngay tại bước Check-in
        if (commissionService.record(booking, "SETTLE") && booking.getMerchantNetAmount() != null && booking.getMerchantNetAmount().compareTo(BigDecimal.ZERO) > 0) {
            if (merchantWalletService != null) {
                merchantWalletService.settleToAvailable(
                        booking.getStore().getStoreId(),
                        booking.getMerchantNetAmount(),
                        "BOOKING",
                        booking.getBookingId().toString(),
                        "Giải ngân doanh thu đơn đặt bàn check-in #" + booking.getBookingCode()
                );
            }
        }

        // 2. Trao thưởng Loyalty MoodPoints (Spend Points + Thưởng đúng giờ) ngay tại bước Check-in
        try {
            if (loyaltyRewardService != null) {
                loyaltyRewardService.awardBookingSpendReward(booking);
                loyaltyRewardService.awardBookingOnTimeCheckIn(booking);
            }
        } catch (Exception ex) {
            log.warn("Failed to award loyalty rewards at check-in: {}", ex.getMessage());
        }

        try {
            if (questEventService != null) {
                questEventService.onBookingCheckedIn(booking);
            }
        } catch (Exception ex) {
            log.warn("Failed to trigger quest event at check-in: {}", ex.getMessage());
        }

        config = getOrCreateConfig(booking.getStore());
        BookingResponse res = enrichBookingResponse(booking, config);

        dispatchBookingNotification(
                booking,
                BookingEventType.CHECKED_IN,
                "Khách " + booking.getCustomerName() + " đã check-in thành công cho đơn " + booking.getBookingCode()
                        + (booking.getPhysicalTableNumber() != null ? " (" + booking.getPhysicalTableNumber() + ")" : "")
        );
        return res;
    }

    @Override
    @Transactional
    public BookingResponse deskClaimLate(UUID storeId, UUID bookingId) {
        User currentUser = currentUserService.getCurrentUser();
        storeStaffService.requireStoreAccess(storeId, "OWNER", "MANAGER", "CASHIER");

        Booking booking = bookingRepository.findByIdForUpdate(bookingId)
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

        if (commissionService.record(booking, "SETTLE") && booking.getMerchantNetAmount() != null && booking.getMerchantNetAmount().compareTo(BigDecimal.ZERO) > 0) {
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

        try {
            if (questEventService != null) {
                questEventService.onBookingClaimedLate(booking);
            }
        } catch (Exception ex) {
            log.warn("Failed to trigger quest event at claim late: {}", ex.getMessage());
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
        storeStaffService.requireStoreAccess(storeId, "OWNER", "MANAGER", "CASHIER");

        Booking booking = bookingRepository.findByIdForUpdate(bookingId)
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));

        if (!booking.getStore().getStoreId().equals(storeId)) {
            throw new AppException(ErrorCode.FORBIDDEN_STORE_ACCESS, "Đơn đặt bàn không thuộc cơ sở này");
        }

        if (booking.getStatus() != BookingStatus.CHECKED_IN && booking.getStatus() != BookingStatus.CHECK_OUT_PENDING) {
            throw new AppException(ErrorCode.INVALID_BOOKING_STATUS_TRANSITION,
                    "Chỉ đơn đang ngồi (CHECKED_IN) hoặc chờ trả bàn (CHECK_OUT_PENDING) mới có thể hoàn tất trả bàn");
        }

        booking.setStatus(BookingStatus.COMPLETED);
        booking.setCompletedAt(Instant.now());
        booking = bookingRepository.save(booking);

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
    public BookingResponse requestCheckout(UUID bookingId) {
        User currentUser = currentUserService.getCurrentUser();
        Booking booking = bookingRepository.findByIdForUpdate(bookingId)
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));

        if (!booking.getUser().getUserId().equals(currentUser.getUserId())) {
            throw new AppException(ErrorCode.FORBIDDEN_ACTION, "Bạn không thể thao tác trên đơn của người khác");
        }

        if (booking.getStatus() != BookingStatus.CHECKED_IN) {
            throw new AppException(ErrorCode.INVALID_BOOKING_STATUS_TRANSITION,
                    "Chỉ đơn đang ngồi (CHECKED_IN) mới có thể báo trả bàn");
        }

        booking.setStatus(BookingStatus.CHECK_OUT_PENDING);
        booking.setCheckOutRequestedAt(Instant.now());
        booking = bookingRepository.save(booking);

        StoreBookingConfig config = getOrCreateConfig(booking.getStore());
        BookingResponse res = enrichBookingResponse(booking, config);

        dispatchBookingNotification(
                booking,
                BookingEventType.CHECK_OUT_REQUESTED,
                "Khách hàng vừa bấm báo trả bàn cho đơn " + booking.getBookingCode()
        );
        return res;
    }

    @Override
    @Transactional
    public BookingResponse cancelCheckoutRequest(UUID bookingId) {
        User currentUser = currentUserService.getCurrentUser();
        Booking booking = bookingRepository.findByIdForUpdate(bookingId)
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));

        boolean isCustomer = booking.getUser().getUserId().equals(currentUser.getUserId());
        if (!isCustomer) {
            storeStaffService.requireStoreAccess(booking.getStore().getStoreId(), "OWNER", "MANAGER", "CASHIER");
        }

        if (booking.getStatus() != BookingStatus.CHECK_OUT_PENDING) {
            throw new AppException(ErrorCode.INVALID_BOOKING_STATUS_TRANSITION,
                    "Chỉ đơn đang chờ xác nhận trả bàn (CHECK_OUT_PENDING) mới có thể hủy yêu cầu");
        }

        booking.setStatus(BookingStatus.CHECKED_IN);
        booking.setCheckOutRequestedAt(null);
        booking = bookingRepository.save(booking);

        StoreBookingConfig config = getOrCreateConfig(booking.getStore());
        BookingResponse res = enrichBookingResponse(booking, config);

        dispatchBookingNotification(
                booking,
                BookingEventType.CHECK_OUT_CANCELLED,
                "Yêu cầu trả bàn của đơn " + booking.getBookingCode() + " đã được hủy."
        );
        return res;
    }

    @Override
    @Transactional
    public BookingResponse undoComplete(UUID storeId, UUID bookingId) {
        User currentUser = currentUserService.getCurrentUser();
        storeStaffService.requireStoreAccess(storeId, "OWNER", "MANAGER", "CASHIER");

        Booking booking = bookingRepository.findByIdForUpdate(bookingId)
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));

        if (!booking.getStore().getStoreId().equals(storeId)) {
            throw new AppException(ErrorCode.FORBIDDEN_STORE_ACCESS, "Đơn đặt bàn không thuộc cơ sở này");
        }

        if (booking.getStatus() != BookingStatus.COMPLETED) {
            throw new AppException(ErrorCode.INVALID_BOOKING_STATUS_TRANSITION,
                    "Chỉ đơn đã hoàn tất (COMPLETED) mới có thể hoàn tác");
        }

        Instant completedAt = booking.getCompletedAt() != null ? booking.getCompletedAt() : booking.getUpdatedAt();
        if (completedAt != null && Duration.between(completedAt, Instant.now()).toMinutes() > 10) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Đã quá thời hạn 10 phút để hoàn tác hoàn tất đơn");
        }

        booking.setStatus(BookingStatus.CHECKED_IN);
        booking.setCompletedAt(null);
        booking = bookingRepository.save(booking);

        StoreBookingConfig config = getOrCreateConfig(booking.getStore());
        BookingResponse res = enrichBookingResponse(booking, config);

        dispatchBookingNotification(
                booking,
                BookingEventType.CHECKED_IN,
                "Đơn đặt bàn " + booking.getBookingCode() + " đã được hoàn tác về trạng thái đang ngồi."
        );
        return res;
    }

    @Override
    @Transactional
    public BookingResponse updatePhysicalTable(UUID storeId, UUID bookingId, String physicalTableNumber) {
        User currentUser = currentUserService.getCurrentUser();
        storeStaffService.requireStoreAccess(storeId, "OWNER", "MANAGER", "CASHIER");

        Booking booking = bookingRepository.findByIdForUpdate(bookingId)
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));

        if (!booking.getStore().getStoreId().equals(storeId)) {
            throw new AppException(ErrorCode.FORBIDDEN_STORE_ACCESS, "Đơn đặt bàn không thuộc cơ sở này");
        }

        booking.setPhysicalTableNumber(physicalTableNumber != null ? physicalTableNumber.trim() : null);
        booking = bookingRepository.save(booking);

        StoreBookingConfig config = getOrCreateConfig(booking.getStore());
        return enrichBookingResponse(booking, config);
    }

    @Override
    @Transactional
    public BookingResponse createWalkInHold(UUID storeId, UUID categoryId, Integer durationMinutes, String physicalTableNumber) {
        User currentUser = currentUserService.getCurrentUser();
        storeStaffService.requireStoreAccess(storeId, "OWNER", "MANAGER", "CASHIER");

        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new AppException(ErrorCode.STORE_NOT_FOUND));

        TableCategory category = tableCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new AppException(ErrorCode.TABLE_CATEGORY_NOT_FOUND));

        if (!category.getStore().getStoreId().equals(storeId)) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Loại bàn không thuộc cơ sở này");
        }

        int duration = (durationMinutes != null && durationMinutes > 0) ? durationMinutes : 120;
        Instant now = Instant.now();
        Instant end = now.plus(Duration.ofMinutes(duration));
        String code = "WLK" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        String tableNum = (physicalTableNumber != null && !physicalTableNumber.isBlank())
                ? physicalTableNumber.trim()
                : null;

        Booking booking = Booking.builder()
                .bookingCode(code)
                .store(store)
                .tableCategory(category)
                .user(currentUser)
                .customerName("Khách vãng lai (" + category.getName() + ")")
                .customerPhone("N/A")
                .guestCount(category.getMaxCapacity() != null ? category.getMaxCapacity() : 2)
                .physicalTableNumber(tableNum)
                .startTime(now)
                .endTime(end)
                .status(BookingStatus.WALK_IN_HOLD)
                .minSpendAmount(0L)
                .grossAmount(BigDecimal.ZERO)
                .voucherDiscount(BigDecimal.ZERO)
                .customerPaidAmount(BigDecimal.ZERO)
                .actualPaidAmount(BigDecimal.ZERO)
                .commissionAmount(BigDecimal.ZERO)
                .merchantNetAmount(BigDecimal.ZERO)
                .refundProcessed(true)
                .build();
        booking = bookingRepository.save(booking);

        StoreBookingConfig config = getOrCreateConfig(store);
        BookingResponse res = enrichBookingResponse(booking, config);

        dispatchBookingNotification(
                booking,
                BookingEventType.WALK_IN_HELD,
                "Đã giữ tạm 1 slot bàn cho khách vãng lai (" + category.getName() + ")"
        );
        return res;
    }

    @Override
    @Transactional
    public BookingResponse releaseWalkInHold(UUID storeId, UUID bookingId) {
        User currentUser = currentUserService.getCurrentUser();
        storeStaffService.requireStoreAccess(storeId, "OWNER", "MANAGER", "CASHIER");

        Booking booking = bookingRepository.findByIdForUpdate(bookingId)
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));

        if (!booking.getStore().getStoreId().equals(storeId)) {
            throw new AppException(ErrorCode.FORBIDDEN_STORE_ACCESS, "Đơn không thuộc cơ sở này");
        }

        if (booking.getStatus() != BookingStatus.WALK_IN_HOLD) {
            throw new AppException(ErrorCode.INVALID_BOOKING_STATUS_TRANSITION,
                    "Chỉ đơn khóa bàn vãng lai (WALK_IN_HOLD) mới có thể nhả bàn");
        }

        booking.setStatus(BookingStatus.COMPLETED);
        booking.setCompletedAt(Instant.now());
        booking = bookingRepository.save(booking);

        StoreBookingConfig config = getOrCreateConfig(booking.getStore());
        return enrichBookingResponse(booking, config);
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
                        .isEarlyCheckInEnabled(true)
                        .maxEarlyCheckInMins(60)
                        .build());
    }

    private Booking reconcileBookingStatus(Booking booking) {
        // Scheduled transitions own status + settlement atomically. Reads never close a booking.
        return booking;
    }

    private void settleNoShowRevenue(Booking booking) {
        if (commissionService.record(booking, "SETTLE") && booking.getMerchantNetAmount() != null && booking.getMerchantNetAmount().compareTo(BigDecimal.ZERO) > 0) {
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
        if (bookingResponseEnricher != null) {
            return bookingResponseEnricher.enrich(booking, config);
        }
        return new BookingResponseEnricher(bookingMapper, storeImageRepository, redisTemplate, objectMapper, customerTitleService)
                .enrich(booking, config);
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
            case CHECK_OUT_REQUESTED -> {
                notifType = NotificationType.RESERVATION_UPDATED;
                customerTitle = "Đã gửi yêu cầu trả bàn (" + bookingCode + ")";
                customerMsg = message != null ? message : "Nhân viên đang tiến hành kiểm tra & dọn bàn. Cảm ơn bạn!";
                staffTitle = "Khách báo trả bàn (" + (booking.getPhysicalTableNumber() != null ? booking.getPhysicalTableNumber() : bookingCode) + ")";
                staffMsg = "Khách hàng " + customerName + " (" + (booking.getPhysicalTableNumber() != null ? booking.getPhysicalTableNumber() : bookingCode) + ") vừa bấm báo trả bàn. Hãy kiểm tra & dọn bàn.";
            }
            case CHECK_OUT_CANCELLED -> {
                notifType = NotificationType.RESERVATION_UPDATED;
                customerTitle = "Đã hủy yêu cầu trả bàn (" + bookingCode + ")";
                customerMsg = message != null ? message : "Yêu cầu trả bàn đã được hủy. Chúc bạn có thời gian tuyệt vời tại quán!";
                staffTitle = "Hủy yêu cầu trả bàn (" + (booking.getPhysicalTableNumber() != null ? booking.getPhysicalTableNumber() : bookingCode) + ")";
                staffMsg = "Đơn của khách " + customerName + " (" + (booking.getPhysicalTableNumber() != null ? booking.getPhysicalTableNumber() : bookingCode) + ") đã quay lại trạng thái đang ngồi.";
            }
            case OVERSTAY_ALERT -> {
                notifType = NotificationType.RESERVATION_UPDATED;
                customerTitle = "Hết giờ ngồi (" + bookingCode + ")";
                customerMsg = "Thời gian sử dụng bàn tại " + storeName + " đã kết thúc. Vui lòng bấm 'Trả bàn' nếu bạn chuẩn bị rời quán nhé!";
                staffTitle = "Hết giờ ngồi: " + (booking.getPhysicalTableNumber() != null ? booking.getPhysicalTableNumber() : bookingCode);
                staffMsg = "Đơn đặt bàn của khách " + customerName + " (" + (booking.getPhysicalTableNumber() != null ? booking.getPhysicalTableNumber() : bookingCode) + ") đã hết giờ ngồi quy định.";
            }
            case WALK_IN_HELD -> {
                notifType = NotificationType.RESERVATION_UPDATED;
                staffTitle = "Khóa bàn tạm cho khách vãng lai";
                staffMsg = message != null ? message : "Đã khóa 1 slot bàn cho khách vãng lai.";
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
                customerMsg = message != null ? message : "Giao dịch thanh toán gói đồ uống cho đơn " + bookingCode + " đã thất bại hoặc quá hạn.";
            }
            case CREATED -> {
                notifType = NotificationType.RESERVATION_CREATED;
                customerTitle = "Tạo đơn đặt bàn (" + bookingCode + ")";
                customerMsg = "Vui lòng thanh toán gói đồ uống trong vòng 15 phút để hoàn tất giữ chỗ tại " + storeName + ".";
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
                        "/bookings"
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

    @Override
    public void evictBookingPaymentCache(UUID bookingId) {
        evictCachedPayOSResponse(bookingId);
    }

    private void evictCachedPayOSResponse(UUID bookingId) {
        if (bookingId == null || redisTemplate == null) {
            return;
        }
        try {
            redisTemplate.delete("booking:payos:" + bookingId);
        } catch (Exception ignored) {}
    }

    /**
     * Resolves the financial snapshot (gross, voucher discount, customer paid, commission, merchant net, funding split).
     */
    private ReservationFinancialSnapshot resolveFinancialSnapshot(
            Store store,
            TableCategory category,
            CustomerVoucher voucher
    ) {
        Long minSpend = category.getMinSpendAmount() != null ? category.getMinSpendAmount() : 0L;
        BigDecimal grossAmount = BigDecimal.valueOf(minSpend);

        BigDecimal voucherDiscount = BigDecimal.ZERO;
        BigDecimal refundFunded = BigDecimal.ZERO;
        BigDecimal rewardFunded = BigDecimal.ZERO;

        if (voucher != null) {
            if (voucher.getMinGrossAmount() != null && grossAmount.compareTo(voucher.getMinGrossAmount()) < 0) {
                throw new AppException(ErrorCode.BAD_REQUEST, "Đơn chưa đạt giá trị tối thiểu của voucher");
            }
            voucherDiscount = voucher.getFaceValueVnd() != null ? voucher.getFaceValueVnd() : BigDecimal.ZERO;
            List<VoucherPointAllocation> allocations = voucherPointAllocationRepository.findByVoucherId(voucher.getVoucherId());
            for (VoucherPointAllocation alloc : allocations) {
                BigDecimal allocAmount = BigDecimal.valueOf(alloc.getAllocatedPoints());
                if ("REFUND".equalsIgnoreCase(alloc.getFundingType())) {
                    refundFunded = refundFunded.add(allocAmount);
                } else {
                    rewardFunded = rewardFunded.add(allocAmount);
                }
            }

            // Fallback if allocations not explicitly set
            if (allocations.isEmpty() && voucherDiscount.compareTo(BigDecimal.ZERO) > 0) {
                rewardFunded = voucherDiscount;
            }
        }

        ReservationPricingInput pricingInput = ReservationPricingInput.builder()
                .storeId(store.getStoreId())
                .grossAmount(grossAmount)
                .voucherDiscount(voucherDiscount)
                .voucherRefundFunded(refundFunded)
                .voucherRewardFunded(rewardFunded)
                .build();

        return reservationPricingService.quote(pricingInput);
    }

    /**
     * Computes the 3 service deadlines based on store booking configuration and closing time.
     */
    private Deadlines calculateDeadlines(Store store, StoreBookingConfig config, Instant startTime) {
        int freeCancelHours = config.getFreeCancelHours() != null ? config.getFreeCancelHours() : 2;
        int gracePeriodMins = config.getGracePeriodMins() != null ? config.getGracePeriodMins() : 30;

        Instant freeCancelDeadline = startTime.minus(Duration.ofHours(freeCancelHours));
        Instant graceDeadline = startTime.plus(Duration.ofMinutes(gracePeriodMins));

        ZonedDateTime startVn = startTime.atZone(VN_ZONE);
        LocalDate startDate = startVn.toLocalDate();
        LocalTime closingTime = store.getClosingTime() != null ? store.getClosingTime() : LocalTime.of(22, 0);

        Instant closingDeadline;
        if (store.getOpeningTime() != null && closingTime.isBefore(store.getOpeningTime())) {
            // Over-midnight closing
            closingDeadline = startDate.plusDays(1).atTime(closingTime).atZone(VN_ZONE).toInstant();
        } else {
            closingDeadline = startDate.atTime(closingTime).atZone(VN_ZONE).toInstant();
        }

        return new Deadlines(freeCancelDeadline, graceDeadline, closingDeadline);
    }


    private record Deadlines(Instant freeCancelDeadline, Instant graceDeadline, Instant serviceClosingDeadline) {}
}
