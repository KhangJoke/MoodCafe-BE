package com.moodcafe.voucher;

import com.moodcafe.voucher.abstraction.repository.CustomerVoucherRepository;
import com.moodcafe.voucher.abstraction.repository.VoucherPointAllocationRepository;
import com.moodcafe.voucher.abstraction.repository.VoucherRedemptionRepository;
import com.moodcafe.voucher.abstraction.repository.VoucherTemplateRepository;
import com.moodcafe.voucher.entity.CustomerVoucher;
import com.moodcafe.voucher.entity.VoucherRedemption;
import com.moodcafe.voucher.entity.VoucherTemplate;
import com.moodcafe.voucher.service.VoucherRedemptionService;
import com.moodcafe.loyalty.dto.PointAllocation;
import com.moodcafe.loyalty.service.CustomerLoyaltyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VoucherRedemptionIdempotencyTest {

    @Mock
    private VoucherTemplateRepository voucherTemplateRepository;
    @Mock
    private CustomerVoucherRepository customerVoucherRepository;
    @Mock
    private VoucherPointAllocationRepository allocationRepository;
    @Mock
    private VoucherRedemptionRepository voucherRedemptionRepository;
    @Mock
    private CustomerLoyaltyService customerLoyaltyService;

    private VoucherRedemptionService redemptionService;

    private UUID userId;
    private UUID templateId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        templateId = UUID.randomUUID();

        redemptionService = new VoucherRedemptionService(
                voucherTemplateRepository,
                customerVoucherRepository,
                allocationRepository,
                voucherRedemptionRepository,
                customerLoyaltyService
        );
    }

    @Test
    @DisplayName("Initial redemption deducts points, mints voucher and records idempotency entry")
    void initialRedemptionCreatesVoucherAndAudit() {
        String intentKey = "INTENT_REDEEM_100";

        VoucherTemplate template = VoucherTemplate.builder()
                .templateId(templateId)
                .title("Giảm 50k")
                .faceValueVnd(new BigDecimal("50000"))
                .pointCost(50000L)
                .isActive(true)
                .build();
        when(voucherTemplateRepository.findById(templateId)).thenReturn(Optional.of(template));
        when(voucherRedemptionRepository.findByUserIdAndIdempotencyKey(userId, intentKey)).thenReturn(Optional.empty());

        PointAllocation allocation = PointAllocation.builder()
                .totalAllocatedPoints(50000L)
                .refundPointsAllocated(50000L)
                .rewardPointsAllocated(0L)
                .items(List.of(PointAllocation.LotAllocationItem.builder()
                        .lotId(UUID.randomUUID())
                        .allocatedPoints(50000L)
                        .fundingType("REFUND")
                        .build()))
                .build();
        when(customerLoyaltyService.allocatePoints(eq(userId), eq(50000L), eq(intentKey), anyString())).thenReturn(allocation);

        UUID voucherId = UUID.randomUUID();
        when(customerVoucherRepository.save(any(CustomerVoucher.class))).thenAnswer(i -> {
            CustomerVoucher v = i.getArgument(0);
            v.setVoucherId(voucherId);
            return v;
        });

        CustomerVoucher result = redemptionService.redeemVoucher(userId, templateId, intentKey);

        assertThat(result.getVoucherId()).isEqualTo(voucherId);
        assertThat(result.getFaceValueVnd()).isEqualTo(new BigDecimal("50000"));

        // Verify allocation was recorded
        verify(customerLoyaltyService).allocatePoints(eq(userId), eq(50000L), eq(intentKey), anyString());
        // Verify idempotency redemption record was saved
        verify(voucherRedemptionRepository).save(any(VoucherRedemption.class));
    }

    @Test
    @DisplayName("Retry with same intentKey returns existing voucher without deducting points or minting duplicate")
    void retryWithSameIntentKeyReplaysExistingVoucher() {
        String intentKey = "INTENT_REDEEM_100";
        UUID existingVoucherId = UUID.randomUUID();

        VoucherRedemption existingRedemption = VoucherRedemption.builder()
                .userId(userId)
                .idempotencyKey(intentKey)
                .payloadHash("a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2")
                .templateId(templateId)
                .voucherId(existingVoucherId)
                .build();

        CustomerVoucher existingVoucher = CustomerVoucher.builder()
                .voucherId(existingVoucherId)
                .userId(userId)
                .templateId(templateId)
                .code("MOOD50K123")
                .faceValueVnd(new BigDecimal("50000"))
                .status("AVAILABLE")
                .expiresAt(Instant.now().plusSeconds(86400 * 60))
                .build();

        when(voucherRedemptionRepository.findByUserIdAndIdempotencyKey(userId, intentKey)).thenReturn(Optional.of(existingRedemption));
        when(customerVoucherRepository.findById(existingVoucherId)).thenReturn(Optional.of(existingVoucher));

        CustomerVoucher result = redemptionService.redeemVoucher(userId, templateId, intentKey);

        assertThat(result.getVoucherId()).isEqualTo(existingVoucherId);
        assertThat(result.getCode()).isEqualTo("MOOD50K123");

        // Never deduct points again
        verify(customerLoyaltyService, never()).allocatePoints(any(), anyLong(), any(), any());
        // Never save new voucher
        verify(customerVoucherRepository, never()).save(any());
    }
}
