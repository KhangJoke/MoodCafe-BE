package com.moodcafe.finance;

import com.moodcafe.finance.abstraction.repository.CommissionPolicyRepository;
import com.moodcafe.finance.dto.ReservationFinancialSnapshot;
import com.moodcafe.finance.dto.ReservationPricingInput;
import com.moodcafe.finance.entity.CommissionPolicy;
import com.moodcafe.finance.service.ReservationPricingService;
import com.moodcafe.shared.exceptions.AppException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservationPricingServiceTest {

    @Mock
    private CommissionPolicyRepository commissionPolicyRepository;

    @InjectMocks
    private ReservationPricingService pricingService;

    private UUID storeId;
    private CommissionPolicy defaultGlobalPolicy;

    @BeforeEach
    void setUp() {
        storeId = UUID.randomUUID();
        defaultGlobalPolicy = CommissionPolicy.builder()
                .commissionRate(new BigDecimal("0.1500"))
                .isActive(true)
                .build();

        // By default, no store-specific override; return default global policy (15%)
        when(commissionPolicyRepository.findActiveStorePolicies(any(), any()))
                .thenReturn(Collections.emptyList());
        when(commissionPolicyRepository.findActiveGlobalPolicies(any()))
                .thenReturn(List.of(defaultGlobalPolicy));
    }

    @Test
    @DisplayName("Case 1: Gross 100k, Rate 15%, Voucher REWARD 10k -> PayOS 90k, Net 85k, Platform Net Revenue 5k")
    void testRewardFundedVoucherWithinCommission() {
        ReservationPricingInput input = ReservationPricingInput.builder()
                .storeId(storeId)
                .grossAmount(new BigDecimal("100000"))
                .voucherDiscount(new BigDecimal("10000"))
                .voucherRewardFunded(new BigDecimal("10000"))
                .voucherRefundFunded(BigDecimal.ZERO)
                .build();

        ReservationFinancialSnapshot snapshot = pricingService.quote(input);

        assertNotNull(snapshot);
        assertEquals(new BigDecimal("100000"), snapshot.getGrossAmount());
        assertEquals(new BigDecimal("10000"), snapshot.getVoucherDiscount());
        assertEquals(new BigDecimal("90000"), snapshot.getCustomerPaidAmount()); // PayOS to collect
        assertEquals(new BigDecimal("15000"), snapshot.getCommissionAmount());   // 15% of 100k
        assertEquals(new BigDecimal("85000"), snapshot.getMerchantNetAmount());  // Store receives full 85k
        assertEquals(new BigDecimal("5000"), snapshot.getPlatformNetRevenue());  // 15k commission - 10k reward = 5k
    }

    @Test
    @DisplayName("Case 2: Gross 100k, Rate 15%, Voucher REFUND 90k -> PayOS 10k, Net 85k, Platform Net Revenue 15k")
    void testRefundFundedVoucherExceedingCommissionAllowed() {
        ReservationPricingInput input = ReservationPricingInput.builder()
                .storeId(storeId)
                .grossAmount(new BigDecimal("100000"))
                .voucherDiscount(new BigDecimal("90000"))
                .voucherRefundFunded(new BigDecimal("90000"))
                .voucherRewardFunded(BigDecimal.ZERO)
                .build();

        ReservationFinancialSnapshot snapshot = pricingService.quote(input);

        assertNotNull(snapshot);
        assertEquals(new BigDecimal("100000"), snapshot.getGrossAmount());
        assertEquals(new BigDecimal("90000"), snapshot.getVoucherDiscount());
        assertEquals(new BigDecimal("10000"), snapshot.getCustomerPaidAmount()); // Customer only pays remaining 10k via PayOS
        assertEquals(new BigDecimal("15000"), snapshot.getCommissionAmount());
        assertEquals(new BigDecimal("85000"), snapshot.getMerchantNetAmount());  // Store STILL receives 85k
        assertEquals(new BigDecimal("15000"), snapshot.getPlatformNetRevenue()); // Platform keeps 15k commission (refund came from prior collected funds)
    }

    @Test
    @DisplayName("Case 3: Gross 100k, Rate 15%, Mixed Voucher (80k REFUND + 10k REWARD) -> PayOS 10k, Net 85k, Platform 5k")
    void testMixedFundedVoucher() {
        ReservationPricingInput input = ReservationPricingInput.builder()
                .storeId(storeId)
                .grossAmount(new BigDecimal("100000"))
                .voucherDiscount(new BigDecimal("90000"))
                .voucherRefundFunded(new BigDecimal("80000"))
                .voucherRewardFunded(new BigDecimal("10000"))
                .build();

        ReservationFinancialSnapshot snapshot = pricingService.quote(input);

        assertNotNull(snapshot);
        assertEquals(new BigDecimal("100000"), snapshot.getGrossAmount());
        assertEquals(new BigDecimal("90000"), snapshot.getVoucherDiscount());
        assertEquals(new BigDecimal("10000"), snapshot.getCustomerPaidAmount());
        assertEquals(new BigDecimal("15000"), snapshot.getCommissionAmount());
        assertEquals(new BigDecimal("85000"), snapshot.getMerchantNetAmount());
        assertEquals(new BigDecimal("5000"), snapshot.getPlatformNetRevenue()); // 15k commission - 10k reward = 5k
    }

    @Test
    @DisplayName("Case 4: REWARD 20k exceeding commission 15k is BLOCKED by Anti-loss Rule")
    void testRewardVoucherExceedingCommissionIsBlocked() {
        ReservationPricingInput input = ReservationPricingInput.builder()
                .storeId(storeId)
                .grossAmount(new BigDecimal("100000"))
                .voucherDiscount(new BigDecimal("20000"))
                .voucherRewardFunded(new BigDecimal("20000"))
                .voucherRefundFunded(BigDecimal.ZERO)
                .build();

        AppException ex = assertThrows(AppException.class, () -> pricingService.quote(input));
        assertTrue(ex.getMessage().contains("vượt quá giới hạn trợ giá"));
    }

    @Test
    @DisplayName("Case 5: REFUND 20k exceeding commission 15k is NOT blocked by commission")
    void testRefundVoucherExceedingCommissionIsNotBlocked() {
        ReservationPricingInput input = ReservationPricingInput.builder()
                .storeId(storeId)
                .grossAmount(new BigDecimal("100000"))
                .voucherDiscount(new BigDecimal("20000"))
                .voucherRefundFunded(new BigDecimal("20000"))
                .voucherRewardFunded(BigDecimal.ZERO)
                .build();

        ReservationFinancialSnapshot snapshot = pricingService.quote(input);

        assertNotNull(snapshot);
        assertEquals(new BigDecimal("80000"), snapshot.getCustomerPaidAmount());
        assertEquals(new BigDecimal("85000"), snapshot.getMerchantNetAmount());
        assertEquals(new BigDecimal("15000"), snapshot.getPlatformNetRevenue());
    }

    @Test
    @DisplayName("Case 6: Voucher discount exceeding gross amount is BLOCKED")
    void testVoucherExceedingGrossIsBlocked() {
        ReservationPricingInput input = ReservationPricingInput.builder()
                .storeId(storeId)
                .grossAmount(new BigDecimal("100000"))
                .voucherDiscount(new BigDecimal("120000"))
                .build();

        AppException ex = assertThrows(AppException.class, () -> pricingService.quote(input));
        assertTrue(ex.getMessage().contains("không được vượt quá"));
    }

    @Test
    @DisplayName("Case 7: Rounding to integer VND with HALF_UP")
    void testRoundingToVndHalfUp() {
        // Gross 75.333, rate 15% -> 11.299,95 -> 11.300
        ReservationPricingInput input = ReservationPricingInput.builder()
                .storeId(storeId)
                .grossAmount(new BigDecimal("75333"))
                .build();

        ReservationFinancialSnapshot snapshot = pricingService.quote(input);

        assertNotNull(snapshot);
        assertEquals(new BigDecimal("75333"), snapshot.getGrossAmount());
        assertEquals(new BigDecimal("11300"), snapshot.getCommissionAmount());
        assertEquals(new BigDecimal("64033"), snapshot.getMerchantNetAmount()); // 75333 - 11300 = 64033
    }
}
