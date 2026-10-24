package com.moodcafe.booking.service;

import com.moodcafe.booking.dto.request.BookingQuoteRequest;
import com.moodcafe.booking.dto.response.BookingCustomerDetailResponse;
import com.moodcafe.booking.dto.response.BookingQuoteResponse;
import com.moodcafe.booking.dto.response.BookingStaffDetailResponse;
import com.moodcafe.booking.entity.Booking;
import com.moodcafe.booking.entity.BookingQuote;
import com.moodcafe.booking.entity.StoreBookingConfig;
import com.moodcafe.booking.entity.TableCategory;
import com.moodcafe.booking.entity.enums.BookingStatus;
import com.moodcafe.finance.dto.ReservationFinancialSnapshot;
import com.moodcafe.finance.dto.ReservationPricingInput;
import com.moodcafe.finance.service.ReservationPricingService;
import com.moodcafe.shared.error.ErrorCode;
import com.moodcafe.shared.exceptions.AppException;
import com.moodcafe.store.entity.Store;
import com.moodcafe.voucher.abstraction.repository.CustomerVoucherRepository;
import com.moodcafe.voucher.abstraction.repository.VoucherPointAllocationRepository;
import com.moodcafe.voucher.entity.CustomerVoucher;
import com.moodcafe.voucher.entity.VoucherPointAllocation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingFinanceService {

    private static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final Duration QUOTE_TTL = Duration.ofMinutes(15);

    private final ReservationPricingService reservationPricingService;
    private final CustomerVoucherRepository customerVoucherRepository;
    private final VoucherPointAllocationRepository voucherPointAllocationRepository;

    /**
     * Calculates the selection fingerprint to detect tampering or changed selection between quote and booking.
     */
    public String calculateFingerprint(UUID storeId, UUID categoryId, Instant startTime, Integer guestCount, UUID voucherId) {
        String raw = storeId + ":" + categoryId + ":" + startTime.toEpochMilli() + ":" + guestCount + ":"
                + (voucherId != null ? voucherId.toString() : "NONE");
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Resolves the financial snapshot (gross, voucher discount, customer paid, commission, merchant net, funding split).
     */
    public ReservationFinancialSnapshot resolveFinancialSnapshot(
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
    public Deadlines calculateDeadlines(Store store, StoreBookingConfig config, Instant startTime) {
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

    /**
     * Determines allowed actions for a customer on a booking.
     */
    public List<String> calculateCustomerAllowedActions(Booking booking) {
        if (booking == null || booking.getStatus() == null) {
            return Collections.emptyList();
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
            case CHECKED_IN -> {
                actions.add("SELF_CHECK_OUT");
            }
            case COMPLETED, CLAIMED_LATE -> {
                actions.add("REVIEW");
            }
            default -> {}
        }

        return actions;
    }

    /**
     * Determines allowed actions for staff/cashier on a booking.
     */
    public List<String> calculateStaffAllowedActions(Booking booking) {
        if (booking == null || booking.getStatus() == null) {
            return Collections.emptyList();
        }
        Instant now = Instant.now();
        List<String> actions = new ArrayList<>();

        switch (booking.getStatus()) {
            case PAID_CONFIRMED -> {
                actions.add("CHECK_IN");
                actions.add("CANCEL");
            }
            case CHECKED_IN -> {
                actions.add("COMPLETE");
            }
            case TABLE_EXPIRED -> {
                if (booking.getServiceClosingDeadline() == null || now.isBefore(booking.getServiceClosingDeadline())) {
                    actions.add("CLAIM_LATE");
                }
            }
            default -> {}
        }

        return actions;
    }

    public record Deadlines(Instant freeCancelDeadline, Instant graceDeadline, Instant serviceClosingDeadline) {}
}
