package com.moodcafe.finance.service;

import com.moodcafe.finance.abstraction.repository.BookingCommissionEntryRepository;
import com.moodcafe.finance.dto.PlatformFinanceSummaryResponse;
import com.moodcafe.finance.dto.PlatformFinanceTrendItemResponse;
import com.moodcafe.finance.dto.PlatformRevenueEntryResponse;
import com.moodcafe.shared.error.ErrorCode;
import com.moodcafe.shared.exceptions.AppException;
import com.moodcafe.shared.response.PageResponse;
import com.moodcafe.subscription.abstraction.repository.SubscriptionPaymentRepository;
import com.moodcafe.subscription.entity.SubscriptionPayment;
import com.moodcafe.subscription.entity.enums.SubscriptionPaymentStatus;
import com.moodcafe.voucher.abstraction.repository.CustomerVoucherRepository;
import com.moodcafe.voucher.abstraction.repository.RevenueVoucherCampaignRepository;
import com.moodcafe.voucher.entity.RevenueVoucherCampaign;
import com.moodcafe.voucher.entity.enums.RevenueFundingSource;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PlatformFinanceService {

    public static final String SOURCE_SUBSCRIPTION = "SUBSCRIPTION_PAYMENT";
    public static final String SOURCE_BOOKING_COMMISSION = "BOOKING_COMMISSION";
    public static final String VOUCHER_SOURCE_REVENUE_GRANT = "REVENUE_GRANT";

    private static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM");

    private final SubscriptionPaymentRepository subscriptionPaymentRepository;
    private final BookingCommissionEntryRepository bookingCommissionEntryRepository;
    private final RevenueVoucherCampaignRepository campaignRepository;
    private final CustomerVoucherRepository customerVoucherRepository;

    @Transactional(readOnly = true)
    public PlatformFinanceSummaryResponse getSummary(LocalDate fromDate, LocalDate toDate) {
        LocalDate end = toDate != null ? toDate : LocalDate.now(VN_ZONE);
        LocalDate start = fromDate != null ? fromDate : end.withDayOfMonth(1);
        if (end.isBefore(start)) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Ngày kết thúc phải lớn hơn hoặc bằng ngày bắt đầu");
        }

        Instant startInstant = start.atStartOfDay(VN_ZONE).toInstant();
        Instant endExclusive = end.plusDays(1).atStartOfDay(VN_ZONE).toInstant();

        PlatformFinanceSummaryResponse.FinancialSnapshot allTime = buildAllTimeSnapshot();
        PlatformFinanceSummaryResponse.FinancialSnapshot period = buildPeriodSnapshot(startInstant, endExclusive);

        return PlatformFinanceSummaryResponse.builder()
                .from(start)
                .to(end)
                .allTime(allTime)
                .period(period)
                .build();
    }

    @Transactional(readOnly = true)
    public List<PlatformFinanceTrendItemResponse> getTrends(LocalDate fromDate, LocalDate toDate, String groupBy) {
        String mode = groupBy != null ? groupBy.trim().toUpperCase() : "DAILY";
        if (!"DAILY".equals(mode) && !"MONTHLY".equals(mode)) {
            throw new AppException(ErrorCode.BAD_REQUEST, "groupBy chỉ hỗ trợ DAILY hoặc MONTHLY");
        }

        LocalDate end = toDate != null ? toDate : LocalDate.now(VN_ZONE);
        LocalDate start = fromDate != null ? fromDate : ("MONTHLY".equals(mode) ? end.minusMonths(5).withDayOfMonth(1) : end.minusDays(29));
        if (end.isBefore(start)) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Ngày kết thúc phải lớn hơn hoặc bằng ngày bắt đầu");
        }

        Instant startInstant = start.atStartOfDay(VN_ZONE).toInstant();
        Instant endExclusive = end.plusDays(1).atStartOfDay(VN_ZONE).toInstant();

        Map<String, TrendAccumulator> buckets = new LinkedHashMap<>();
        if ("MONTHLY".equals(mode)) {
            YearMonth cur = YearMonth.from(start);
            YearMonth last = YearMonth.from(end);
            while (!cur.isAfter(last)) {
                buckets.put(cur.format(MONTH_FMT), new TrendAccumulator());
                cur = cur.plusMonths(1);
            }
        } else {
            LocalDate cur = start;
            while (!cur.isAfter(end)) {
                buckets.put(cur.format(DAY_FMT), new TrendAccumulator());
                cur = cur.plusDays(1);
            }
        }

        // 1. Subscription payments
        List<SubscriptionPayment> subPayments = subscriptionPaymentRepository
                .findSuccessfulPaymentsBetween(startInstant, endExclusive.minusNanos(1));
        for (SubscriptionPayment sp : subPayments) {
            Instant ts = sp.getPaidAt() != null ? sp.getPaidAt() : sp.getCreatedAt();
            String key = formatKey(ts, mode);
            TrendAccumulator acc = buckets.get(key);
            if (acc != null && sp.getAmount() != null) {
                acc.subRevenue = acc.subRevenue.add(sp.getAmount());
            }
        }

        // 2. Settled booking commissions
        List<BookingCommissionEntryRepository.SettledCommissionPoint> commissions =
                bookingCommissionEntryRepository.findSettledPointsBetween(startInstant, endExclusive);
        for (BookingCommissionEntryRepository.SettledCommissionPoint c : commissions) {
            String key = formatKey(c.occurredAt(), mode);
            TrendAccumulator acc = buckets.get(key);
            if (acc != null && c.amount() != null) {
                acc.bookingRevenue = acc.bookingRevenue.add(c.amount());
            }
        }

        // 3. Revenue voucher campaign expenses
        List<RevenueVoucherCampaign> campaigns = campaignRepository.findAllCreatedBetween(startInstant, endExclusive);
        for (RevenueVoucherCampaign camp : campaigns) {
            String key = formatKey(camp.getCreatedAt(), mode);
            TrendAccumulator acc = buckets.get(key);
            if (acc != null) {
                acc.voucherExpenses = acc.voucherExpenses.add(camp.getEffectiveExpenseVnd());
            }
        }

        List<PlatformFinanceTrendItemResponse> result = new ArrayList<>(buckets.size());
        for (Map.Entry<String, TrendAccumulator> entry : buckets.entrySet()) {
            TrendAccumulator acc = entry.getValue();
            BigDecimal totalRev = acc.subRevenue.add(acc.bookingRevenue);
            BigDecimal net = totalRev.subtract(acc.voucherExpenses);
            result.add(PlatformFinanceTrendItemResponse.builder()
                    .label(entry.getKey())
                    .subscriptionRevenue(acc.subRevenue)
                    .bookingCommissionRevenue(acc.bookingRevenue)
                    .totalRevenue(totalRev)
                    .voucherExpenses(acc.voucherExpenses)
                    .netIncome(net)
                    .build());
        }
        return result;
    }

    @Transactional(readOnly = true)
    public PageResponse<PlatformRevenueEntryResponse> getRevenueEntries(
            String source,
            LocalDate fromDate,
            LocalDate toDate,
            int page,
            int size
    ) {
        if (page < 0 || size < 1 || size > 100) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Phân trang không hợp lệ (size 1-100)");
        }
        String src = source != null ? source.trim().toUpperCase() : "ALL";
        if (!"ALL".equals(src) && !SOURCE_SUBSCRIPTION.equals(src) && !SOURCE_BOOKING_COMMISSION.equals(src)) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Nguồn doanh thu chỉ hỗ trợ ALL, SUBSCRIPTION_PAYMENT hoặc BOOKING_COMMISSION");
        }

        LocalDate end = toDate != null ? toDate : LocalDate.now(VN_ZONE);
        LocalDate start = fromDate != null ? fromDate : end.minusDays(29);
        if (end.isBefore(start)) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Ngày kết thúc phải lớn hơn hoặc bằng ngày bắt đầu");
        }

        Instant startInstant = start.atStartOfDay(VN_ZONE).toInstant();
        Instant endExclusive = end.plusDays(1).atStartOfDay(VN_ZONE).toInstant();

        List<PlatformRevenueEntryResponse> entries = new ArrayList<>();

        if ("ALL".equals(src) || SOURCE_SUBSCRIPTION.equals(src)) {
            List<SubscriptionPayment> subPayments = subscriptionPaymentRepository
                    .findSuccessfulPaymentsBetween(startInstant, endExclusive.minusNanos(1));
            for (SubscriptionPayment sp : subPayments) {
                String planName = sp.getSubscriptionPlan() != null ? sp.getSubscriptionPlan().getDisplayName() : "Gói dịch vụ";
                String payer = sp.getUser() != null ? sp.getUser().getFullName() : null;
                entries.add(PlatformRevenueEntryResponse.builder()
                        .entryId(sp.getPaymentId())
                        .source(SOURCE_SUBSCRIPTION)
                        .referenceCode(sp.getTransactionCode())
                        .description("Thanh toán gói " + planName)
                        .amount(nz(sp.getAmount()))
                        .payerName(payer)
                        .occurredAt(sp.getPaidAt() != null ? sp.getPaidAt() : sp.getCreatedAt())
                        .build());
            }
        }

        if ("ALL".equals(src) || SOURCE_BOOKING_COMMISSION.equals(src)) {
            List<BookingCommissionEntryRepository.SettledCommissionLedgerRow> rows =
                    bookingCommissionEntryRepository.findSettledLedgerRowsBetween(startInstant, endExclusive);
            for (BookingCommissionEntryRepository.SettledCommissionLedgerRow row : rows) {
                entries.add(PlatformRevenueEntryResponse.builder()
                        .entryId(row.entryId())
                        .source(SOURCE_BOOKING_COMMISSION)
                        .referenceCode(row.bookingCode())
                        .description("Hoa hồng đặt bàn đơn " + (row.bookingCode() != null ? row.bookingCode() : row.bookingId()))
                        .amount(nz(row.amount()))
                        .storeName(row.storeName())
                        .payerName(row.customerName())
                        .occurredAt(row.occurredAt())
                        .build());
            }
        }

        entries.sort(Comparator.comparing(PlatformRevenueEntryResponse::getOccurredAt,
                Comparator.nullsLast(Comparator.reverseOrder())));

        long total = entries.size();
        int fromIdx = Math.min(page * size, entries.size());
        int toIdx = Math.min(fromIdx + size, entries.size());
        List<PlatformRevenueEntryResponse> slice = entries.subList(fromIdx, toIdx);
        int totalPages = size > 0 ? (int) Math.ceil((double) total / size) : 0;

        return PageResponse.<PlatformRevenueEntryResponse>builder()
                .items(slice)
                .page(page)
                .size(size)
                .totalElements(total)
                .totalPages(totalPages)
                .last(page + 1 >= totalPages)
                .build();
    }

    @Transactional(readOnly = true)
    public BigDecimal getAvailableNetIncomeAllTime() {
        BigDecimal subRev = nz(subscriptionPaymentRepository.sumTotalRevenue());
        BigDecimal bookingRev = nz(bookingCommissionEntryRepository.sumSettledCommissionAllTime());
        BigDecimal totalExpenses = nz(campaignRepository.sumTotalEffectiveExpenses());
        return subRev.add(bookingRev).subtract(totalExpenses);
    }

    @Transactional(readOnly = true)
    public BigDecimal getAvailableBalanceForSource(RevenueFundingSource fundingSource) {
        if (fundingSource == null || fundingSource == RevenueFundingSource.ALL) {
            return getAvailableNetIncomeAllTime();
        }
        if (fundingSource == RevenueFundingSource.SUBSCRIPTION_PAYMENT) {
            BigDecimal subRev = nz(subscriptionPaymentRepository.sumTotalRevenue());
            BigDecimal subExp = nz(campaignRepository.sumEffectiveExpensesByFundingSource(RevenueFundingSource.SUBSCRIPTION_PAYMENT));
            return subRev.subtract(subExp);
        }
        BigDecimal bookingRev = nz(bookingCommissionEntryRepository.sumSettledCommissionAllTime());
        BigDecimal bookingExp = nz(campaignRepository.sumEffectiveExpensesByFundingSource(RevenueFundingSource.BOOKING_COMMISSION));
        return bookingRev.subtract(bookingExp);
    }

    private PlatformFinanceSummaryResponse.FinancialSnapshot buildAllTimeSnapshot() {
        BigDecimal subRev = nz(subscriptionPaymentRepository.sumTotalRevenue());
        BigDecimal bookingRev = nz(bookingCommissionEntryRepository.sumSettledCommissionAllTime());
        BigDecimal pendingComm = nz(bookingCommissionEntryRepository.sumPendingCommissionAllTime());
        BigDecimal reversedComm = nz(bookingCommissionEntryRepository.sumReversedCommissionAllTime());
        long subCount = subscriptionPaymentRepository.countByStatus(SubscriptionPaymentStatus.SUCCESS);
        long bookingCount = bookingCommissionEntryRepository.countSettledBookingsAllTime();

        BigDecimal totalExp = nz(campaignRepository.sumTotalEffectiveExpenses());
        BigDecimal expSub = nz(campaignRepository.sumEffectiveExpensesByFundingSource(RevenueFundingSource.SUBSCRIPTION_PAYMENT));
        BigDecimal expBooking = nz(campaignRepository.sumEffectiveExpensesByFundingSource(RevenueFundingSource.BOOKING_COMMISSION));
        BigDecimal expGeneral = nz(campaignRepository.sumEffectiveExpensesByFundingSource(RevenueFundingSource.ALL));

        long vouchersIssued = customerVoucherRepository.countBySourceType(VOUCHER_SOURCE_REVENUE_GRANT);
        long vouchersUsed = customerVoucherRepository.countBySourceTypeAndStatus(VOUCHER_SOURCE_REVENUE_GRANT, "USED");
        BigDecimal vouchersUsedVal = nz(customerVoucherRepository.sumFaceValueBySourceTypeAndStatus(VOUCHER_SOURCE_REVENUE_GRANT, "USED"));

        return assembleSnapshot(subRev, bookingRev, pendingComm, reversedComm, subCount, bookingCount,
                totalExp, expSub, expBooking, expGeneral, vouchersIssued, vouchersUsed, vouchersUsedVal);
    }

    private PlatformFinanceSummaryResponse.FinancialSnapshot buildPeriodSnapshot(Instant from, Instant to) {
        BigDecimal subRev = nz(subscriptionPaymentRepository.sumRevenueBetween(from, to));
        BigDecimal bookingRev = nz(bookingCommissionEntryRepository.sumSettledCommissionBetween(from, to));
        BigDecimal pendingComm = nz(bookingCommissionEntryRepository.sumPendingCommissionAllTime());
        BigDecimal reversedComm = nz(bookingCommissionEntryRepository.sumReversedCommissionBetween(from, to));
        long subCount = subscriptionPaymentRepository.countSuccessfulPaymentsBetween(from, to);
        long bookingCount = bookingCommissionEntryRepository.countSettledBookingsBetween(from, to);

        BigDecimal totalExp = nz(campaignRepository.sumEffectiveExpensesBetween(from, to));
        BigDecimal expSub = nz(campaignRepository.sumEffectiveExpensesByFundingSourceBetween(RevenueFundingSource.SUBSCRIPTION_PAYMENT, from, to));
        BigDecimal expBooking = nz(campaignRepository.sumEffectiveExpensesByFundingSourceBetween(RevenueFundingSource.BOOKING_COMMISSION, from, to));
        BigDecimal expGeneral = nz(campaignRepository.sumEffectiveExpensesByFundingSourceBetween(RevenueFundingSource.ALL, from, to));

        long vouchersIssued = customerVoucherRepository.countBySourceTypeBetween(VOUCHER_SOURCE_REVENUE_GRANT, from, to);
        long vouchersUsed = customerVoucherRepository.countBySourceTypeAndStatusUsedBetween(VOUCHER_SOURCE_REVENUE_GRANT, "USED", from, to);
        BigDecimal vouchersUsedVal = nz(customerVoucherRepository.sumFaceValueBySourceTypeAndStatusUsedBetween(VOUCHER_SOURCE_REVENUE_GRANT, "USED", from, to));

        return assembleSnapshot(subRev, bookingRev, pendingComm, reversedComm, subCount, bookingCount,
                totalExp, expSub, expBooking, expGeneral, vouchersIssued, vouchersUsed, vouchersUsedVal);
    }

    private PlatformFinanceSummaryResponse.FinancialSnapshot assembleSnapshot(
            BigDecimal subRev,
            BigDecimal bookingRev,
            BigDecimal pendingComm,
            BigDecimal reversedComm,
            long subCount,
            long bookingCount,
            BigDecimal totalExp,
            BigDecimal expSub,
            BigDecimal expBooking,
            BigDecimal expGeneral,
            long vouchersIssued,
            long vouchersUsed,
            BigDecimal vouchersUsedVal
    ) {
        BigDecimal totalRev = subRev.add(bookingRev);
        BigDecimal netIncome = totalRev.subtract(totalExp);

        BigDecimal subShare = percent(subRev, totalRev);
        BigDecimal bookingShare = percent(bookingRev, totalRev);
        BigDecimal profitMargin = percent(netIncome, totalRev);

        return PlatformFinanceSummaryResponse.FinancialSnapshot.builder()
                .totalRevenue(totalRev)
                .subscriptionRevenue(subRev)
                .bookingCommissionRevenue(bookingRev)
                .pendingBookingCommission(pendingComm)
                .reversedBookingCommission(reversedComm)
                .subscriptionSharePercent(subShare)
                .bookingCommissionSharePercent(bookingShare)
                .subscriptionPaymentCount(subCount)
                .settledBookingCount(bookingCount)
                .totalExpenses(totalExp)
                .expensesFromSubscription(expSub)
                .expensesFromBookingCommission(expBooking)
                .expensesFromGeneralRevenue(expGeneral)
                .revenueVouchersIssuedCount(vouchersIssued)
                .revenueVouchersUsedCount(vouchersUsed)
                .revenueVouchersUsedValueVnd(vouchersUsedVal)
                .netIncome(netIncome)
                .profitMarginPercent(profitMargin)
                .availableSubscriptionNetBalance(subRev.subtract(expSub))
                .availableBookingCommissionNetBalance(bookingRev.subtract(expBooking))
                .build();
    }

    private String formatKey(Instant instant, String mode) {
        LocalDate date = instant.atZone(VN_ZONE).toLocalDate();
        return "MONTHLY".equals(mode) ? YearMonth.from(date).format(MONTH_FMT) : date.format(DAY_FMT);
    }

    private BigDecimal percent(BigDecimal part, BigDecimal total) {
        if (total == null || total.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return part.multiply(BigDecimal.valueOf(100)).divide(total, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal nz(BigDecimal val) {
        return val != null ? val : BigDecimal.ZERO;
    }

    private static class TrendAccumulator {
        BigDecimal subRevenue = BigDecimal.ZERO;
        BigDecimal bookingRevenue = BigDecimal.ZERO;
        BigDecimal voucherExpenses = BigDecimal.ZERO;
    }
}

