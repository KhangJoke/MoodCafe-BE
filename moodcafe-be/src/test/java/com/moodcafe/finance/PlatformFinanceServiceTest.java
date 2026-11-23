package com.moodcafe.finance;

import com.moodcafe.auth.entity.User;
import com.moodcafe.finance.abstraction.repository.BookingCommissionEntryRepository;
import com.moodcafe.finance.dto.PlatformFinanceSummaryResponse;
import com.moodcafe.finance.dto.PlatformFinanceTrendItemResponse;
import com.moodcafe.finance.dto.PlatformRevenueEntryResponse;
import com.moodcafe.finance.service.PlatformFinanceService;
import com.moodcafe.shared.response.PageResponse;
import com.moodcafe.subscription.abstraction.repository.SubscriptionPaymentRepository;
import com.moodcafe.subscription.entity.SubscriptionPayment;
import com.moodcafe.subscription.entity.SubscriptionPlan;
import com.moodcafe.subscription.entity.enums.SubscriptionPaymentStatus;
import com.moodcafe.voucher.abstraction.repository.CustomerVoucherRepository;
import com.moodcafe.voucher.abstraction.repository.RevenueVoucherCampaignRepository;
import com.moodcafe.voucher.entity.RevenueVoucherCampaign;
import com.moodcafe.voucher.entity.enums.RevenueFundingSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlatformFinanceServiceTest {

    @Mock
    private SubscriptionPaymentRepository subscriptionPaymentRepository;

    @Mock
    private BookingCommissionEntryRepository bookingCommissionEntryRepository;

    @Mock
    private RevenueVoucherCampaignRepository campaignRepository;

    @Mock
    private CustomerVoucherRepository customerVoucherRepository;

    @InjectMocks
    private PlatformFinanceService platformFinanceService;

    @Test
    @DisplayName("getSummary separates Subscription & Booking Commission revenue, Voucher expenses, and Net Income")
    void getSummary_ComputesAllMetricsAccurately() {
        // All-time mocks
        when(subscriptionPaymentRepository.sumTotalRevenue()).thenReturn(new BigDecimal("10000000"));
        when(bookingCommissionEntryRepository.sumSettledCommissionAllTime()).thenReturn(new BigDecimal("5000000"));
        when(bookingCommissionEntryRepository.sumPendingCommissionAllTime()).thenReturn(new BigDecimal("400000"));
        when(bookingCommissionEntryRepository.sumReversedCommissionAllTime()).thenReturn(new BigDecimal("200000"));
        when(subscriptionPaymentRepository.countByStatus(SubscriptionPaymentStatus.SUCCESS)).thenReturn(20L);
        when(bookingCommissionEntryRepository.countSettledBookingsAllTime()).thenReturn(100L);

        when(campaignRepository.sumTotalEffectiveExpenses()).thenReturn(new BigDecimal("3000000"));
        when(campaignRepository.sumEffectiveExpensesByFundingSource(RevenueFundingSource.SUBSCRIPTION_PAYMENT))
                .thenReturn(new BigDecimal("1000000"));
        when(campaignRepository.sumEffectiveExpensesByFundingSource(RevenueFundingSource.BOOKING_COMMISSION))
                .thenReturn(new BigDecimal("1500000"));
        when(campaignRepository.sumEffectiveExpensesByFundingSource(RevenueFundingSource.ALL))
                .thenReturn(new BigDecimal("500000"));

        when(customerVoucherRepository.countBySourceType("REVENUE_GRANT")).thenReturn(60L);
        when(customerVoucherRepository.countBySourceTypeAndStatus("REVENUE_GRANT", "USED")).thenReturn(25L);
        when(customerVoucherRepository.sumFaceValueBySourceTypeAndStatus("REVENUE_GRANT", "USED"))
                .thenReturn(new BigDecimal("1250000"));

        // Period mocks
        when(subscriptionPaymentRepository.sumRevenueBetween(any(), any())).thenReturn(new BigDecimal("2000000"));
        when(bookingCommissionEntryRepository.sumSettledCommissionBetween(any(), any())).thenReturn(new BigDecimal("1000000"));
        when(bookingCommissionEntryRepository.sumReversedCommissionBetween(any(), any())).thenReturn(new BigDecimal("50000"));
        when(subscriptionPaymentRepository.countSuccessfulPaymentsBetween(any(), any())).thenReturn(4L);
        when(bookingCommissionEntryRepository.countSettledBookingsBetween(any(), any())).thenReturn(20L);

        when(campaignRepository.sumEffectiveExpensesBetween(any(), any())).thenReturn(new BigDecimal("600000"));
        when(campaignRepository.sumEffectiveExpensesByFundingSourceBetween(eq(RevenueFundingSource.SUBSCRIPTION_PAYMENT), any(), any()))
                .thenReturn(new BigDecimal("200000"));
        when(campaignRepository.sumEffectiveExpensesByFundingSourceBetween(eq(RevenueFundingSource.BOOKING_COMMISSION), any(), any()))
                .thenReturn(new BigDecimal("400000"));
        when(campaignRepository.sumEffectiveExpensesByFundingSourceBetween(eq(RevenueFundingSource.ALL), any(), any()))
                .thenReturn(BigDecimal.ZERO);

        when(customerVoucherRepository.countBySourceTypeBetween(eq("REVENUE_GRANT"), any(), any())).thenReturn(12L);
        when(customerVoucherRepository.countBySourceTypeAndStatusUsedBetween(eq("REVENUE_GRANT"), eq("USED"), any(), any())).thenReturn(5L);
        when(customerVoucherRepository.sumFaceValueBySourceTypeAndStatusUsedBetween(eq("REVENUE_GRANT"), eq("USED"), any(), any()))
                .thenReturn(new BigDecimal("250000"));

        PlatformFinanceSummaryResponse res = platformFinanceService.getSummary(
                LocalDate.of(2026, 11, 1),
                LocalDate.of(2026, 11, 23)
        );

        assertThat(res.getAllTime().getTotalRevenue()).isEqualByComparingTo("15000000");
        assertThat(res.getAllTime().getSubscriptionRevenue()).isEqualByComparingTo("10000000");
        assertThat(res.getAllTime().getBookingCommissionRevenue()).isEqualByComparingTo("5000000");
        assertThat(res.getAllTime().getTotalExpenses()).isEqualByComparingTo("3000000");
        assertThat(res.getAllTime().getNetIncome()).isEqualByComparingTo("12000000");
        assertThat(res.getAllTime().getProfitMarginPercent()).isEqualByComparingTo("80.00");
        assertThat(res.getAllTime().getAvailableSubscriptionNetBalance()).isEqualByComparingTo("9000000");
        assertThat(res.getAllTime().getAvailableBookingCommissionNetBalance()).isEqualByComparingTo("3500000");

        assertThat(res.getPeriod().getTotalRevenue()).isEqualByComparingTo("3000000");
        assertThat(res.getPeriod().getTotalExpenses()).isEqualByComparingTo("600000");
        assertThat(res.getPeriod().getNetIncome()).isEqualByComparingTo("2400000");
    }

    @Test
    @DisplayName("getTrends aggregates daily subscription revenue, booking commission, voucher expenses and net income")
    void getTrends_DailyAggregation() {
        ZoneId vn = ZoneId.of("Asia/Ho_Chi_Minh");
        LocalDate d1 = LocalDate.of(2026, 11, 20);
        LocalDate d2 = LocalDate.of(2026, 11, 21);
        Instant t1 = d1.atTime(10, 0).atZone(vn).toInstant();
        Instant t2 = d2.atTime(14, 0).atZone(vn).toInstant();

        SubscriptionPayment sp = SubscriptionPayment.builder()
                .amount(new BigDecimal("500000"))
                .paidAt(t1)
                .build();
        when(subscriptionPaymentRepository.findSuccessfulPaymentsBetween(any(), any())).thenReturn(List.of(sp));

        when(bookingCommissionEntryRepository.findSettledPointsBetween(any(), any())).thenReturn(List.of(
                new BookingCommissionEntryRepository.SettledCommissionPoint(new BigDecimal("150000"), t1),
                new BookingCommissionEntryRepository.SettledCommissionPoint(new BigDecimal("200000"), t2)
        ));

        RevenueVoucherCampaign camp = RevenueVoucherCampaign.builder()
                .allocatedBudgetVnd(new BigDecimal("100000"))
                .releasedBudgetVnd(BigDecimal.ZERO)
                .createdAt(t1)
                .build();
        when(campaignRepository.findAllCreatedBetween(any(), any())).thenReturn(List.of(camp));

        List<PlatformFinanceTrendItemResponse> trends = platformFinanceService.getTrends(d1, d2, "DAILY");

        assertThat(trends).hasSize(2);
        assertThat(trends.get(0).getLabel()).isEqualTo("2026-11-20");
        assertThat(trends.get(0).getSubscriptionRevenue()).isEqualByComparingTo("500000");
        assertThat(trends.get(0).getBookingCommissionRevenue()).isEqualByComparingTo("150000");
        assertThat(trends.get(0).getTotalRevenue()).isEqualByComparingTo("650000");
        assertThat(trends.get(0).getVoucherExpenses()).isEqualByComparingTo("100000");
        assertThat(trends.get(0).getNetIncome()).isEqualByComparingTo("550000");

        assertThat(trends.get(1).getLabel()).isEqualTo("2026-11-21");
        assertThat(trends.get(1).getBookingCommissionRevenue()).isEqualByComparingTo("200000");
        assertThat(trends.get(1).getNetIncome()).isEqualByComparingTo("200000");
    }

    @Test
    @DisplayName("getRevenueEntries combines and sorts Subscription and Booking Commission entries")
    void getRevenueEntries_AllSources() {
        Instant t1 = Instant.parse("2026-11-20T05:00:00Z");
        Instant t2 = Instant.parse("2026-11-21T05:00:00Z");

        SubscriptionPlan plan = SubscriptionPlan.builder().displayName("Gói Pro").build();
        User owner = User.builder().fullName("Chủ Quán A").build();
        SubscriptionPayment sp = SubscriptionPayment.builder()
                .paymentId(UUID.randomUUID())
                .transactionCode("SUB-001")
                .amount(new BigDecimal("499000"))
                .subscriptionPlan(plan)
                .user(owner)
                .paidAt(t1)
                .build();
        when(subscriptionPaymentRepository.findSuccessfulPaymentsBetween(any(), any())).thenReturn(List.of(sp));

        BookingCommissionEntryRepository.SettledCommissionLedgerRow commRow =
                new BookingCommissionEntryRepository.SettledCommissionLedgerRow(
                        UUID.randomUUID(), UUID.randomUUID(), "MC12345678", "The Workshop", "Khách B",
                        new BigDecimal("45000"), t2
                );
        when(bookingCommissionEntryRepository.findSettledLedgerRowsBetween(any(), any())).thenReturn(List.of(commRow));

        PageResponse<PlatformRevenueEntryResponse> res = platformFinanceService.getRevenueEntries(
                "ALL", LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 23), 0, 10
        );

        assertThat(res.getTotalElements()).isEqualTo(2);
        assertThat(res.getItems().get(0).getSource()).isEqualTo("BOOKING_COMMISSION");
        assertThat(res.getItems().get(0).getAmount()).isEqualByComparingTo("45000");
        assertThat(res.getItems().get(1).getSource()).isEqualTo("SUBSCRIPTION_PAYMENT");
        assertThat(res.getItems().get(1).getAmount()).isEqualByComparingTo("499000");
    }
}

