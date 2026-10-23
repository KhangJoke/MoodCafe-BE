package com.moodcafe.wallet;

import com.moodcafe.shared.exceptions.AppException;
import com.moodcafe.wallet.abstraction.repository.CustomerWalletRepository;
import com.moodcafe.wallet.abstraction.repository.PointLotRepository;
import com.moodcafe.wallet.abstraction.repository.PointTransactionRepository;
import com.moodcafe.wallet.dto.PointAllocation;
import com.moodcafe.wallet.dto.RefundPointCommand;
import com.moodcafe.wallet.dto.RewardPointCommand;
import com.moodcafe.wallet.entity.CustomerWallet;
import com.moodcafe.wallet.entity.PointLot;
import com.moodcafe.wallet.entity.PointTransaction;
import com.moodcafe.wallet.service.CustomerWalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PointWalletServiceTest {

    @Mock
    private CustomerWalletRepository customerWalletRepository;

    @Mock
    private PointLotRepository pointLotRepository;

    @Mock
    private PointTransactionRepository pointTransactionRepository;

    @InjectMocks
    private CustomerWalletService walletService;

    private UUID userId;
    private CustomerWallet wallet;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        wallet = CustomerWallet.builder()
                .walletId(UUID.randomUUID())
                .userId(userId)
                .pointsBalance(0L)
                .build();
    }

    @Test
    @DisplayName("Credit Refund Points (1:1): +90.000đ creates REFUND lot with NULL expiry and increases balance")
    void testCreditRefundPointsSuccess() {
        when(pointTransactionRepository.findByBusinessKey("REFUND_123")).thenReturn(Optional.empty());
        when(customerWalletRepository.findByUserIdWithLock(userId)).thenReturn(Optional.of(wallet));
        when(customerWalletRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(pointTransactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RefundPointCommand cmd = RefundPointCommand.builder()
                .userId(userId)
                .points(90000L)
                .refundId("RES_001")
                .businessKey("REFUND_123")
                .description("Hoàn tiền đơn RES_001")
                .build();

        PointTransaction tx = walletService.creditRefundPoints(cmd);

        assertNotNull(tx);
        assertEquals(90000L, tx.getAmount());
        assertEquals(90000L, tx.getBalanceAfter());
        assertEquals("REFUND_CREDIT", tx.getTxType());
        assertEquals(90000L, wallet.getPointsBalance());

        verify(pointLotRepository).save(argThat(lot ->
                lot.getSourceType().equals("REFUND") &&
                lot.getRemainingPoints().equals(90000L) &&
                lot.getExpiresAt() == null // Never expires
        ));
    }

    @Test
    @DisplayName("Idempotency: Replaying creditRefundPoints with same businessKey returns existing without double crediting")
    void testCreditRefundPointsIdempotency() {
        PointTransaction existingTx = PointTransaction.builder()
                .businessKey("REFUND_123")
                .amount(90000L)
                .balanceAfter(90000L)
                .build();

        when(pointTransactionRepository.findByBusinessKey("REFUND_123")).thenReturn(Optional.of(existingTx));

        RefundPointCommand cmd = RefundPointCommand.builder()
                .userId(userId)
                .points(90000L)
                .businessKey("REFUND_123")
                .build();

        PointTransaction result = walletService.creditRefundPoints(cmd);

        assertSame(existingTx, result);
        verify(customerWalletRepository, never()).findByUserIdWithLock(any());
        verify(pointLotRepository, never()).save(any());
    }

    @Test
    @DisplayName("Credit Reward Points: +1.000 points creates REWARD lot with 365-day expiry and updates balance")
    void testCreditRewardPointsSuccess() {
        wallet.setPointsBalance(90000L);
        when(pointTransactionRepository.findByBusinessKey("REWARD_CHECKIN_1")).thenReturn(Optional.empty());
        when(customerWalletRepository.findByUserIdWithLock(userId)).thenReturn(Optional.of(wallet));
        when(customerWalletRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(pointTransactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RewardPointCommand cmd = RewardPointCommand.builder()
                .userId(userId)
                .points(1000L)
                .rewardType("ON_TIME_CHECK_IN")
                .sourceId("RES_001")
                .businessKey("REWARD_CHECKIN_1")
                .build();

        PointTransaction tx = walletService.creditRewardPoints(cmd);

        assertNotNull(tx);
        assertEquals(1000L, tx.getAmount());
        assertEquals(91000L, tx.getBalanceAfter());
        assertEquals("REWARD_CREDIT", tx.getTxType());
        assertEquals(91000L, wallet.getPointsBalance());

        verify(pointLotRepository).save(argThat(lot ->
                lot.getSourceType().equals("REWARD") &&
                lot.getRemainingPoints().equals(1000L) &&
                lot.getExpiresAt() != null // 365-day expiry
        ));
    }

    @Test
    @DisplayName("Allocate Points: Prioritizes REFUND lots before REWARD lots according to Policy P10")
    void testAllocatePointsPrioritizesRefundLots() {
        wallet.setPointsBalance(100000L); // 90k refund + 10k reward
        when(customerWalletRepository.findByUserIdWithLock(userId)).thenReturn(Optional.of(wallet));

        PointLot refundLot = PointLot.builder()
                .lotId(UUID.randomUUID())
                .userId(userId)
                .sourceType("REFUND")
                .remainingPoints(90000L)
                .expiresAt(null)
                .build();

        PointLot rewardLot = PointLot.builder()
                .lotId(UUID.randomUUID())
                .userId(userId)
                .sourceType("REWARD")
                .remainingPoints(10000L)
                .expiresAt(Instant.now().plus(100, ChronoUnit.DAYS))
                .build();

        // Spendable lots ordered by SQL: REFUND first, then REWARD
        when(pointLotRepository.findSpendableLotsWithLock(eq(userId), any()))
                .thenReturn(new ArrayList<>(List.of(refundLot, rewardLot)));
        when(customerWalletRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(pointTransactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Customer redeems a 50k voucher -> should consume 50k from REFUND lot, 0 from REWARD
        PointAllocation allocation = walletService.allocatePoints(userId, 50000L, "REDEEM_VOUCHER_50K", "Đổi voucher 50k");

        assertNotNull(allocation);
        assertEquals(50000L, allocation.getTotalAllocatedPoints());
        assertEquals(50000L, allocation.getRefundPointsAllocated());
        assertEquals(0L, allocation.getRewardPointsAllocated());
        assertEquals(40000L, refundLot.getRemainingPoints());
        assertEquals(10000L, rewardLot.getRemainingPoints());
        assertEquals(50000L, wallet.getPointsBalance());
    }

    @Test
    @DisplayName("Allocate Points: Consumes mixed lots (90k REFUND + 5k REWARD) when redeeming 95k")
    void testAllocatePointsMixedLots() {
        wallet.setPointsBalance(100000L);
        when(customerWalletRepository.findByUserIdWithLock(userId)).thenReturn(Optional.of(wallet));

        PointLot refundLot = PointLot.builder()
                .lotId(UUID.randomUUID())
                .userId(userId)
                .sourceType("REFUND")
                .remainingPoints(90000L)
                .build();

        PointLot rewardLot = PointLot.builder()
                .lotId(UUID.randomUUID())
                .userId(userId)
                .sourceType("REWARD")
                .remainingPoints(10000L)
                .build();

        when(pointLotRepository.findSpendableLotsWithLock(eq(userId), any()))
                .thenReturn(new ArrayList<>(List.of(refundLot, rewardLot)));
        when(customerWalletRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(pointTransactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PointAllocation allocation = walletService.allocatePoints(userId, 95000L, "REDEEM_95K", "Đổi voucher 95k");

        assertNotNull(allocation);
        assertEquals(95000L, allocation.getTotalAllocatedPoints());
        assertEquals(90000L, allocation.getRefundPointsAllocated());
        assertEquals(5000L, allocation.getRewardPointsAllocated());
        assertEquals(0L, refundLot.getRemainingPoints());
        assertEquals(5000L, rewardLot.getRemainingPoints());
        assertEquals(5000L, wallet.getPointsBalance());
    }

    @Test
    @DisplayName("Allocate Points: Insufficient balance throws AppException")
    void testAllocatePointsInsufficientThrows() {
        wallet.setPointsBalance(10000L);
        when(customerWalletRepository.findByUserIdWithLock(userId)).thenReturn(Optional.of(wallet));

        AppException ex = assertThrows(AppException.class, () ->
                walletService.allocatePoints(userId, 50000L, "REDEEM_FAIL", "Đổi voucher 50k"));

        assertTrue(ex.getMessage().contains("Số dư MoodPoints không đủ"));
    }
}
