package com.moodcafe.booking.enricher;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moodcafe.booking.dto.response.BookingResponse;
import com.moodcafe.booking.dto.response.RefundOutcomeResponse;
import com.moodcafe.booking.entity.Booking;
import com.moodcafe.booking.entity.StoreBookingConfig;
import com.moodcafe.booking.entity.enums.BookingStatus;
import com.moodcafe.booking.mapper.BookingMapper;
import com.moodcafe.gamification.abstraction.service.CustomerTitleService;
import com.moodcafe.payment.dto.response.PayOSResponse;
import com.moodcafe.store.abstraction.repository.StoreImageRepository;
import com.moodcafe.store.entity.StoreImage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Dedicated enricher for BookingResponse DTOs.
 * Leverages MapStruct for automatic property mapping and enriches dynamic fields
 * (store images, titles, action lists, deadlines, and cached PayOS payment info).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BookingResponseEnricher {

    private final BookingMapper bookingMapper;
    private final StoreImageRepository storeImageRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    @Autowired(required = false)
    private CustomerTitleService customerTitleService;

    public BookingResponseEnricher(BookingMapper bookingMapper,
                                  StoreImageRepository storeImageRepository,
                                  RedisTemplate<String, String> redisTemplate,
                                  ObjectMapper objectMapper,
                                  CustomerTitleService customerTitleService) {
        this.bookingMapper = bookingMapper;
        this.storeImageRepository = storeImageRepository;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.customerTitleService = customerTitleService;
    }

    public BookingResponse enrich(Booking booking, StoreBookingConfig config) {
        if (booking == null) {
            return null;
        }

        // 1. Auto-map base entity fields through MapStruct
        BookingResponse res = bookingMapper.toBookingResponse(booking);
        if (res == null) {
            return null;
        }

        if (config != null) {
            res.setGracePeriodMins(config.getGracePeriodMins());
            res.setFreeCancelHours(config.getFreeCancelHours());
        }

        // 2. Resolve primary store image
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

        // 3. Resolve cancellation eligibility & allowed customer actions
        boolean cancelable = (booking.getStatus() == BookingStatus.PAID_CONFIRMED || booking.getStatus() == BookingStatus.PENDING_PAYMENT)
                && (booking.getFreeCancelDeadline() == null || Instant.now().isBefore(booking.getFreeCancelDeadline()));
        res.setIsCancelable(cancelable);
        res.setAllowedActions(calculateCustomerAllowedActions(booking));

        // 4. Resolve cached PayOS payment checkout details if PENDING_PAYMENT
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

        // 5. Resolve refund outcome if refund processed
        if (Boolean.TRUE.equals(booking.getRefundProcessed())) {
            res.setRefundOutcome(RefundOutcomeResponse.builder()
                    .actualPaidRefundPoints(booking.getActualPaidAmount() != null ? booking.getActualPaidAmount().longValue() : 0L)
                    .voucherRestorationStatus(booking.getVoucher() != null ? "RESTORED" : "NO_VOUCHER")
                    .processedAt(booking.getCancelledAt())
                    .eventId("EVT-" + booking.getBookingCode())
                    .build());
        }

        // 6. Resolve equipped title for customer
        if (booking.getUser() != null && booking.getUser().getUserId() != null && customerTitleService != null) {
            try {
                res.setCustomerEquippedTitle(customerTitleService.getEquippedTitle(booking.getUser().getUserId()));
            } catch (Exception ex) {
                log.warn("Failed to get equipped title for booking customer: {}", ex.getMessage());
            }
        }

        return res;
    }

    public List<String> calculateCustomerAllowedActions(Booking booking) {
        if (booking == null || booking.getStatus() == null) {
            return List.of();
        }
        Instant now = Instant.now();
        List<String> actions = new ArrayList<>();

        switch (booking.getStatus()) {
            case PENDING_PAYMENT -> {
                actions.add("PAY");
                actions.add("CANCEL");
            }
            case PAID_CONFIRMED -> {
                if (booking.getFreeCancelDeadline() != null && now.isBefore(booking.getFreeCancelDeadline())) {
                    actions.add("CANCEL");
                }
            }
            case CHECKED_IN -> actions.add("REQUEST_CHECKOUT");
            case CHECK_OUT_PENDING -> actions.add("CANCEL_CHECKOUT_REQUEST");
            case COMPLETED, CLAIMED_LATE -> actions.add("REVIEW");
            default -> {}
        }

        return actions;
    }

    public List<String> calculateStaffAllowedActions(Booking booking) {
        if (booking == null || booking.getStatus() == null) {
            return List.of();
        }
        Instant now = Instant.now();
        List<String> actions = new ArrayList<>();

        switch (booking.getStatus()) {
            case PAID_CONFIRMED -> {
                actions.add("CHECK_IN");
                actions.add("CANCEL");
            }
            case CHECKED_IN, CHECK_OUT_PENDING -> actions.add("COMPLETE");
            case TABLE_EXPIRED -> {
                if (booking.getServiceClosingDeadline() == null || now.isBefore(booking.getServiceClosingDeadline())) {
                    actions.add("CLAIM_LATE");
                }
            }
            default -> {}
        }

        return actions;
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
}
