package com.moodcafe.voucher;

import com.moodcafe.auth.abstraction.repository.UserRepository;
import com.moodcafe.auth.entity.User;
import com.moodcafe.finance.service.PlatformFinanceService;
import com.moodcafe.shared.exceptions.AppException;
import com.moodcafe.voucher.abstraction.repository.CustomerVoucherRepository;
import com.moodcafe.voucher.abstraction.repository.RevenueVoucherCampaignRepository;
import com.moodcafe.voucher.dto.CreateRevenueVoucherCampaignRequest;
import com.moodcafe.voucher.dto.RevenueVoucherCampaignResponse;
import com.moodcafe.voucher.entity.CustomerVoucher;
import com.moodcafe.voucher.entity.RevenueVoucherCampaign;
import com.moodcafe.voucher.entity.enums.RevenueCampaignStatus;
import com.moodcafe.voucher.entity.enums.RevenueFundingSource;
import com.moodcafe.voucher.entity.enums.RevenueVoucherDistributionType;
import com.moodcafe.voucher.service.RevenueVoucherCampaignService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RevenueVoucherCampaignServiceTest {

    @Mock
    private RevenueVoucherCampaignRepository campaignRepository;

    @Mock
    private CustomerVoucherRepository customerVoucherRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PlatformFinanceService platformFinanceService;

    @InjectMocks
    private RevenueVoucherCampaignService service;

    @Test
    @DisplayName("DIRECT_GRANT issues free REVENUE_GRANT vouchers (pointCost=0) to selected users and records budget expense")
    @SuppressWarnings("unchecked")
    void createDirectGrantCampaign_Success() {
        UUID adminId = UUID.randomUUID();
        UUID u1 = UUID.randomUUID();
        UUID u2 = UUID.randomUUID();

        when(userRepository.existsById(u1)).thenReturn(true);
        when(userRepository.existsById(u2)).thenReturn(true);
        when(platformFinanceService.getAvailableNetIncomeAllTime()).thenReturn(new BigDecimal("1000000"));
        when(platformFinanceService.getAvailableBalanceForSource(RevenueFundingSource.BOOKING_COMMISSION))
                .thenReturn(new BigDecimal("500000"));
        when(campaignRepository.save(any())).thenAnswer(inv -> {
            RevenueVoucherCampaign c = inv.getArgument(0);
            c.setCampaignId(UUID.randomUUID());
            return c;
        });
        when(customerVoucherRepository.findByCode(any())).thenReturn(Optional.empty());

        CreateRevenueVoucherCampaignRequest req = CreateRevenueVoucherCampaignRequest.builder()
                .title("Tri ân khách đặt bàn")
                .distributionType(RevenueVoucherDistributionType.DIRECT_GRANT)
                .fundingSource(RevenueFundingSource.BOOKING_COMMISSION)
                .faceValueVnd(new BigDecimal("50000"))
                .minGrossAmount(new BigDecimal("100000"))
                .validDays(30)
                .targetUserIds(List.of(u1, u2))
                .build();

        RevenueVoucherCampaignResponse res = service.createCampaign(req, adminId);

        assertThat(res.getTotalQuantity()).isEqualTo(2);
        assertThat(res.getIssuedQuantity()).isEqualTo(2);
        assertThat(res.getAllocatedBudgetVnd()).isEqualByComparingTo("100000");
        assertThat(res.getEffectiveExpenseVnd()).isEqualByComparingTo("100000");
        assertThat(res.getStatus()).isEqualTo(RevenueCampaignStatus.COMPLETED);

        ArgumentCaptor<List<CustomerVoucher>> captor = ArgumentCaptor.forClass(List.class);
        verify(customerVoucherRepository).saveAll(captor.capture());
        List<CustomerVoucher> savedVouchers = captor.getValue();
        assertThat(savedVouchers).hasSize(2);
        assertThat(savedVouchers).allSatisfy(v -> {
            assertThat(v.getSourceType()).isEqualTo("REVENUE_GRANT");
            assertThat(v.getPointCost()).isEqualTo(0L);
            assertThat(v.getFaceValueVnd()).isEqualByComparingTo("50000");
            assertThat(v.getStatus()).isEqualTo("AVAILABLE");
        });
    }

    @Test
    @DisplayName("Rejects campaign creation when required budget exceeds available Net Income")
    void createCampaign_InsufficientNetIncome_Throws() {
        when(platformFinanceService.getAvailableNetIncomeAllTime()).thenReturn(new BigDecimal("100000"));

        CreateRevenueVoucherCampaignRequest req = CreateRevenueVoucherCampaignRequest.builder()
                .title("Voucher quá ngân sách")
                .distributionType(RevenueVoucherDistributionType.CLAIMABLE_POOL)
                .fundingSource(RevenueFundingSource.ALL)
                .faceValueVnd(new BigDecimal("50000"))
                .totalQuantity(5) // 250,000 > 100,000
                .build();

        assertThatThrownBy(() -> service.createCampaign(req, UUID.randomUUID()))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("Net Income");
    }

    @Test
    @DisplayName("CLAIMABLE_POOL allows customer to claim free voucher with 0 MoodPoints and enforces maxClaimsPerUser")
    void claimVoucher_SuccessAndEnforcesLimit() {
        UUID campaignId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        RevenueVoucherCampaign campaign = RevenueVoucherCampaign.builder()
                .campaignId(campaignId)
                .title("Voucher Miễn Phí Tháng 11")
                .distributionType(RevenueVoucherDistributionType.CLAIMABLE_POOL)
                .fundingSource(RevenueFundingSource.SUBSCRIPTION_PAYMENT)
                .faceValueVnd(new BigDecimal("30000"))
                .minGrossAmount(BigDecimal.ZERO)
                .validDays(15)
                .totalQuantity(1)
                .issuedQuantity(0)
                .maxClaimsPerUser(1)
                .allocatedBudgetVnd(new BigDecimal("30000"))
                .releasedBudgetVnd(BigDecimal.ZERO)
                .status(RevenueCampaignStatus.ACTIVE)
                .build();

        when(campaignRepository.findByIdWithLock(campaignId)).thenReturn(Optional.of(campaign));
        when(customerVoucherRepository.countByCampaignIdAndUserId(campaignId, userId)).thenReturn(0L);
        when(customerVoucherRepository.findByCode(any())).thenReturn(Optional.empty());
        when(customerVoucherRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(campaignRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CustomerVoucher claimed = service.claimVoucher(userId, campaignId);

        assertThat(claimed.getSourceType()).isEqualTo("REVENUE_GRANT");
        assertThat(claimed.getPointCost()).isEqualTo(0L);
        assertThat(claimed.getFaceValueVnd()).isEqualByComparingTo("30000");
        assertThat(claimed.getStatus()).isEqualTo("AVAILABLE");
        assertThat(campaign.getIssuedQuantity()).isEqualTo(1);
        assertThat(campaign.getStatus()).isEqualTo(RevenueCampaignStatus.COMPLETED);
    }

    @Test
    @DisplayName("Cancelling CLAIMABLE_POOL releases unused budget back into Net Income")
    void cancelCampaign_ReleasesUnusedBudget() {
        UUID campaignId = UUID.randomUUID();
        RevenueVoucherCampaign campaign = RevenueVoucherCampaign.builder()
                .campaignId(campaignId)
                .title("Pool 10 voucher")
                .distributionType(RevenueVoucherDistributionType.CLAIMABLE_POOL)
                .fundingSource(RevenueFundingSource.ALL)
                .faceValueVnd(new BigDecimal("20000"))
                .totalQuantity(10)
                .issuedQuantity(3)
                .allocatedBudgetVnd(new BigDecimal("200000"))
                .releasedBudgetVnd(BigDecimal.ZERO)
                .status(RevenueCampaignStatus.ACTIVE)
                .build();

        when(campaignRepository.findByIdWithLock(campaignId)).thenReturn(Optional.of(campaign));
        when(campaignRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(customerVoucherRepository.countByCampaignIdAndStatus(campaignId, "USED")).thenReturn(1L);

        RevenueVoucherCampaignResponse res = service.updateCampaignStatus(campaignId, RevenueCampaignStatus.CANCELLED);

        assertThat(res.getStatus()).isEqualTo(RevenueCampaignStatus.CANCELLED);
        assertThat(res.getReleasedBudgetVnd()).isEqualByComparingTo("140000"); // 7 unclaimed * 20,000
        assertThat(res.getEffectiveExpenseVnd()).isEqualByComparingTo("60000"); // 3 issued * 20,000
    }
}

