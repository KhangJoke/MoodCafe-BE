package com.moodcafe.wallet;

import com.moodcafe.configuration.abstraction.repository.SystemConfigurationRepository;
import com.moodcafe.notification.abstraction.service.NotificationDispatcherService;
import com.moodcafe.shared.exceptions.AppException;
import com.moodcafe.wallet.abstraction.repository.StoreBankAccountRepository;
import com.moodcafe.wallet.abstraction.repository.WithdrawalRequestRepository;
import com.moodcafe.wallet.entity.StoreBankAccount;
import com.moodcafe.wallet.entity.WithdrawalRequest;
import com.moodcafe.wallet.service.MerchantWalletService;
import com.moodcafe.wallet.service.PayoutBreakdownService;
import com.moodcafe.wallet.service.StoreFinancialSecurityService;
import com.moodcafe.wallet.service.WithdrawalService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WithdrawalServiceTest {

    @Mock
    private WithdrawalRequestRepository withdrawalRequestRepository;

    @Mock
    private SystemConfigurationRepository configRepository;

    @Mock
    private StoreBankAccountRepository bankAccountRepository;

    @Mock
    private MerchantWalletService merchantWalletService;

    @Mock
    private StoreFinancialSecurityService financialSecurityService;

    @Mock
    private PayoutBreakdownService breakdownService;

    @Mock
    private NotificationDispatcherService notifications;

    @InjectMocks
    private WithdrawalService withdrawalService;

    private UUID ownerId;
    private UUID storeId;
    private UUID bankAccountId;
    private StoreBankAccount bankAccount;

    @BeforeEach
    void setUp() {
        ownerId = UUID.randomUUID();
        storeId = UUID.randomUUID();
        bankAccountId = UUID.randomUUID();
        bankAccount = StoreBankAccount.builder()
                .bankAccountId(bankAccountId)
                .storeId(storeId)
                .bankName("Vietcombank")
                .accountNumber("123456789")
                .accountHolderName("NGUYEN VAN A")
                .isDefault(true)
                .isDeleted(false)
                .build();
    }

    @Test
    @DisplayName("Request Withdrawal: 100.000đ passes minimum threshold and locks funds")
    void testRequestWithdrawalMinimum100kPassed() {
        when(bankAccountRepository.findById(bankAccountId)).thenReturn(Optional.of(bankAccount));
        when(withdrawalRequestRepository.saveAndFlush(any())).thenAnswer(inv -> {
            WithdrawalRequest r = inv.getArgument(0);
            r.setWithdrawalId(UUID.randomUUID());
            return r;
        });

        WithdrawalRequest req = withdrawalService.requestWithdrawal(
                ownerId,
                storeId,
                new BigDecimal("100000.00"),
                bankAccountId
        );

        assertNotNull(req);
        assertEquals(new BigDecimal("100000.00"), req.getAmount());
        assertEquals("REQUESTED", req.getStatus());
        assertEquals("Vietcombank", req.getBankName());

        verify(financialSecurityService).requireActiveStoreOwner(storeId);
        verify(merchantWalletService).lockForWithdrawal(eq(storeId), eq(new BigDecimal("100000.00")), any());
    }

    @Test
    @DisplayName("Request Withdrawal: 99.999đ fails minimum threshold (Policy P8)")
    void testRequestWithdrawalUnder100kFails() {
        AppException ex = assertThrows(AppException.class, () ->
                withdrawalService.requestWithdrawal(
                        ownerId,
                        storeId,
                        new BigDecimal("99999.00"),
                        bankAccountId
                ));

        assertTrue(ex.getMessage().contains("Số tiền rút tối thiểu"));
        verify(merchantWalletService, never()).lockForWithdrawal(any(), any(), any());
    }

    @Test
    @DisplayName("Claim Withdrawal: Transitions REQUESTED to PROCESSING")
    void testClaimWithdrawalSuccess() {
        UUID withdrawalId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();

        WithdrawalRequest req = WithdrawalRequest.builder()
                .withdrawalId(withdrawalId)
                .ownerId(ownerId)
                .status("REQUESTED")
                .build();

        when(withdrawalRequestRepository.findByIdWithLock(withdrawalId)).thenReturn(Optional.of(req));
        when(withdrawalRequestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        WithdrawalRequest updated = withdrawalService.claimWithdrawal(adminId, withdrawalId);

        assertEquals("PROCESSING", updated.getStatus());
        assertEquals(adminId, updated.getClaimedBy());
    }

    @Test
    @DisplayName("Reject Withdrawal: Unlocks funds back to available and sets status to REJECTED")
    void testRejectWithdrawalSuccess() {
        UUID withdrawalId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();

        WithdrawalRequest req = WithdrawalRequest.builder()
                .withdrawalId(withdrawalId)
                .storeId(storeId)
                .ownerId(ownerId)
                .amount(new BigDecimal("200000.00"))
                .status("REQUESTED")
                .claimedBy(adminId)
                .build();

        when(withdrawalRequestRepository.findByIdWithLock(withdrawalId)).thenReturn(Optional.of(req));
        when(withdrawalRequestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        WithdrawalRequest updated = withdrawalService.rejectWithdrawal(adminId, withdrawalId, "Sai thông tin tài khoản");

        assertEquals("REJECTED", updated.getStatus());
        assertEquals("Sai thông tin tài khoản", updated.getAdminNotes());

        verify(merchantWalletService).unlockWithdrawal(
                eq(storeId),
                eq(new BigDecimal("200000.00")),
                eq(withdrawalId.toString()),
                eq("Sai thông tin tài khoản")
        );
    }

    @Test
    @DisplayName("Confirm Paid: Debits locked funds and transitions PROCESSING to PAID")
    void testConfirmPaidSuccess() {
        UUID withdrawalId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();

        WithdrawalRequest req = WithdrawalRequest.builder()
                .withdrawalId(withdrawalId)
                .storeId(storeId)
                .ownerId(ownerId)
                .amount(new BigDecimal("500000.00"))
                .status("PROCESSING")
                .claimedBy(adminId)
                .build();

        when(withdrawalRequestRepository.findByIdWithLock(withdrawalId)).thenReturn(Optional.of(req));
        when(withdrawalRequestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        WithdrawalRequest updated = withdrawalService.confirmPaid(
                adminId,
                withdrawalId,
                "FT261021998877",
                "https://cdn.moodcafe.vn/evidence/123.jpg"
        );

        assertEquals("PAID", updated.getStatus());
        assertEquals("FT261021998877", updated.getTransactionReference());

        verify(merchantWalletService).completeWithdrawal(
                eq(storeId),
                eq(new BigDecimal("500000.00")),
                eq(withdrawalId.toString())
        );
    }
}
