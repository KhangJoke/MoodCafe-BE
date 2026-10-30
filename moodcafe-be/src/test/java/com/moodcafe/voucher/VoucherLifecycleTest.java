package com.moodcafe.voucher;

import com.moodcafe.loyalty.dto.PointAllocation;
import com.moodcafe.loyalty.dto.RefundPointCommand;
import com.moodcafe.loyalty.service.CustomerLoyaltyService;
import com.moodcafe.voucher.abstraction.repository.CustomerVoucherRepository;
import com.moodcafe.voucher.abstraction.repository.VoucherPointAllocationRepository;
import com.moodcafe.voucher.abstraction.repository.VoucherRedemptionRepository;
import com.moodcafe.voucher.abstraction.repository.VoucherTemplateRepository;
import com.moodcafe.voucher.entity.CustomerVoucher;
import com.moodcafe.voucher.entity.VoucherTemplate;
import com.moodcafe.voucher.service.VoucherLifecycleService;
import com.moodcafe.voucher.service.VoucherRedemptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VoucherLifecycleTest {

        @Mock
        private VoucherTemplateRepository voucherTemplateRepository;

        @Mock
        private CustomerVoucherRepository customerVoucherRepository;

        @Mock
        private VoucherPointAllocationRepository allocationRepository;

        @Mock
        private CustomerLoyaltyService customerLoyaltyService;

        @Mock
        private VoucherRedemptionRepository voucherRedemptionRepository;

    private VoucherRedemptionService redemptionService;
    private VoucherLifecycleService lifecycleService;

        private UUID userId;
        private UUID templateId;
        private VoucherTemplate template;

    @BeforeEach
    void setUp() {
        redemptionService = new VoucherRedemptionService(
                voucherTemplateRepository,
                customerVoucherRepository,
                allocationRepository,
                voucherRedemptionRepository,
                customerLoyaltyService
        );

        lifecycleService = new VoucherLifecycleService(
                customerVoucherRepository,
                allocationRepository,
                customerLoyaltyService
        );

        userId = UUID.randomUUID();
        templateId = UUID.randomUUID();
        template = VoucherTemplate.builder()
                .templateId(templateId)
                .title("Voucher 20k")
                .faceValueVnd(new BigDecimal("20000"))
                .pointCost(20000L)
                .isActive(true)
                .build();
    }

        @Test
        @DisplayName("Redeem Voucher: Deducts 20.000 MoodPoints, creates AVAILABLE voucher and saves lot allocations")
        void testRedeemVoucherSuccess() {
                when(voucherTemplateRepository.findById(templateId)).thenReturn(Optional.of(template));

                PointAllocation mockAlloc = PointAllocation.builder()
                                .totalAllocatedPoints(20000L)
                                .refundPointsAllocated(20000L)
                                .rewardPointsAllocated(0L)
                                .items(List.of(PointAllocation.LotAllocationItem.builder()
                                                .lotId(UUID.randomUUID())
                                                .allocatedPoints(20000L)
                                                .fundingType("REFUND")
                                                .build()))
                                .build();

        when(customerLoyaltyService.allocatePoints(eq(userId), eq(20000L), anyString(), anyString()))
                .thenReturn(mockAlloc);
        when(customerVoucherRepository.findByCode(any())).thenReturn(Optional.empty());
        when(customerVoucherRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

                CustomerVoucher voucher = redemptionService.redeemVoucher(userId, templateId, "KEY_1");

                assertNotNull(voucher);
                assertEquals(userId, voucher.getUserId());
                assertEquals("AVAILABLE", voucher.getStatus());
                assertEquals(new BigDecimal("20000"), voucher.getFaceValueVnd());
                assertEquals(20000L, voucher.getPointCost());

        verify(customerLoyaltyService).allocatePoints(eq(userId), eq(20000L), anyString(), anyString());
        verify(allocationRepository).save(any());
    }

        @Test
        @DisplayName("Lock Voucher: Transitions AVAILABLE to LOCKED with reservationId and holdUntil")
        void testLockVoucherSuccess() {
                UUID voucherId = UUID.randomUUID();
                UUID reservationId = UUID.randomUUID();

                CustomerVoucher voucher = CustomerVoucher.builder()
                                .voucherId(voucherId)
                                .userId(userId)
                                .status("AVAILABLE")
                                .expiresAt(Instant.now().plus(10, ChronoUnit.DAYS))
                                .build();

                when(customerVoucherRepository.findByIdWithLock(voucherId)).thenReturn(Optional.of(voucher));
                when(customerVoucherRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

                lifecycleService.lockVoucher(userId, voucherId, reservationId, Instant.now().plusSeconds(900));

                assertEquals("LOCKED", voucher.getStatus());
                assertEquals(reservationId, voucher.getLockedReservationId());
                assertNotNull(voucher.getHoldUntil());
        }

        @Test
        @DisplayName("Capture Voucher: Transitions LOCKED to USED when payment succeeds")
        void testCaptureVoucherSuccess() {
                UUID reservationId = UUID.randomUUID();
                CustomerVoucher voucher = CustomerVoucher.builder()
                                .voucherId(UUID.randomUUID())
                                .status("LOCKED")
                                .lockedReservationId(reservationId)
                                .build();

                when(customerVoucherRepository.findByLockedReservationIdWithLock(reservationId))
                                .thenReturn(Optional.of(voucher));
                when(customerVoucherRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

                lifecycleService.captureVoucher(reservationId);

                assertEquals("USED", voucher.getStatus());
                assertNotNull(voucher.getUsedAt());
                assertNull(voucher.getHoldUntil());
        }

        @Test
        @DisplayName("Release Voucher: Unlocks voucher back to AVAILABLE if not expired")
        void testReleaseVoucherSuccess() {
                UUID reservationId = UUID.randomUUID();
                CustomerVoucher voucher = CustomerVoucher.builder()
                                .voucherId(UUID.randomUUID())
                                .status("LOCKED")
                                .lockedReservationId(reservationId)
                                .expiresAt(Instant.now().plus(5, ChronoUnit.DAYS))
                                .build();

                when(customerVoucherRepository.findByLockedReservationIdWithLock(reservationId))
                                .thenReturn(Optional.of(voucher));
                when(customerVoucherRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

                lifecycleService.releaseVoucher(reservationId);

                assertEquals("AVAILABLE", voucher.getStatus());
                assertNull(voucher.getLockedReservationId());
        }

        @Test
        @DisplayName("Policy P3: When cancelled reservation voucher has expired, refund its REFUND allocation back to MoodPoints")
        void testRestoreExpiredVoucherRefundsPoints() {
                UUID refundId = UUID.randomUUID();
                UUID voucherId = UUID.randomUUID();

                CustomerVoucher voucher = CustomerVoucher.builder()
                                .voucherId(voucherId)
                                .userId(userId)
                                .status("USED")
                                .expiresAt(Instant.now().minus(1, ChronoUnit.DAYS)) // Expired!
                                .build();

                when(customerVoucherRepository.findByIdWithLock(voucherId)).thenReturn(Optional.of(voucher));
                when(allocationRepository.sumAllocatedPointsByVoucherIdAndFundingType(voucherId, "REFUND"))
                                .thenReturn(20000L); // 20k was funded from REFUND
                when(customerVoucherRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

                lifecycleService.restoreVoucherForRefund(refundId, voucherId);

                assertEquals("EXPIRED", voucher.getStatus());
                // Verify 20.000 REFUND points are credited back to customer wallet
                verify(customerLoyaltyService).creditRefundPoints(argThat(cmd -> cmd.getUserId().equals(userId) &&
                                cmd.getPoints().equals(20000L) &&
                                cmd.getBusinessKey().contains("REFUND_EXPIRED_VOUCHER")));
        }
}
