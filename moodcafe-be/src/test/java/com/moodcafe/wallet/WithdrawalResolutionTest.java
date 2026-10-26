package com.moodcafe.wallet;

import com.moodcafe.configuration.abstraction.repository.SystemConfigurationRepository;
import com.moodcafe.notification.abstraction.service.NotificationDispatcherService;
import com.moodcafe.shared.error.ErrorCode;
import com.moodcafe.shared.exceptions.AppException;
import com.moodcafe.wallet.abstraction.repository.StoreBankAccountRepository;
import com.moodcafe.wallet.abstraction.repository.WithdrawalRequestRepository;
import com.moodcafe.wallet.entity.WithdrawalRequest;
import com.moodcafe.wallet.service.MerchantWalletService;
import com.moodcafe.wallet.service.PayoutBreakdownService;
import com.moodcafe.wallet.service.StoreFinancialSecurityService;
import com.moodcafe.wallet.service.WithdrawalService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WithdrawalResolutionTest {

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

    private WithdrawalService withdrawalService;

    private UUID storeId;
    private UUID ownerId;
    private UUID adminId;

    @BeforeEach
    void setUp() {
        storeId = UUID.randomUUID();
        ownerId = UUID.randomUUID();
        adminId = UUID.randomUUID();

        withdrawalService = new WithdrawalService(
                withdrawalRequestRepository,
                configRepository,
                bankAccountRepository,
                merchantWalletService,
                financialSecurityService,
                breakdownService,
                notifications
        );
    }

    @Test
    @DisplayName("confirmPaid retry with same transaction reference returns existing PAID record without double debit")
    void confirmPaidIdempotentRetry() {
        UUID withdrawalId = UUID.randomUUID();
        String txRef = "FT26102199887766";

        WithdrawalRequest existingPaid = WithdrawalRequest.builder()
                .withdrawalId(withdrawalId)
                .storeId(storeId)
                .ownerId(ownerId)
                .amount(new BigDecimal("500000.00"))
                .status("PAID")
                .transactionReference(txRef)
                .build();

        when(withdrawalRequestRepository.findByIdWithLock(withdrawalId)).thenReturn(Optional.of(existingPaid));

        WithdrawalRequest result = withdrawalService.confirmPaid(adminId, withdrawalId, txRef, null);

        assertThat(result.getStatus()).isEqualTo("PAID");
        assertThat(result.getTransactionReference()).isEqualTo(txRef);
        // Ensure completeWithdrawal was NOT called again (no double debit)
        verify(merchantWalletService, never()).completeWithdrawal(any(), any(), any());
    }

    @Test
    @DisplayName("confirmPaid rejects duplicate transaction reference already used by another withdrawal")
    void confirmPaidRejectsDuplicateRef() {
        UUID withdrawalId = UUID.randomUUID();
        UUID otherWithdrawalId = UUID.randomUUID();
        String txRef = "FT26102199887766";

        WithdrawalRequest request = WithdrawalRequest.builder()
                .withdrawalId(withdrawalId)
                .storeId(storeId)
                .ownerId(ownerId)
                .amount(new BigDecimal("500000.00"))
                .status("PROCESSING")
                .build();

        WithdrawalRequest otherRequest = WithdrawalRequest.builder()
                .withdrawalId(otherWithdrawalId)
                .transactionReference(txRef)
                .build();

        when(withdrawalRequestRepository.findByIdWithLock(withdrawalId)).thenReturn(Optional.of(request));
        when(withdrawalRequestRepository.findByTransactionReferenceIgnoreCase(txRef)).thenReturn(Optional.of(otherRequest));

        assertThatThrownBy(() -> withdrawalService.confirmPaid(adminId, withdrawalId, txRef, null))
                .isInstanceOf(AppException.class)
                .matches(e -> ((AppException) e).getErrorCode() == ErrorCode.BAD_REQUEST);

        verify(merchantWalletService, never()).completeWithdrawal(any(), any(), any());
    }

    @Test
    @DisplayName("Directly rejecting a PROCESSING withdrawal is blocked; must use reconciliation")
    void directRejectProcessingBlocked() {
        UUID withdrawalId = UUID.randomUUID();

        WithdrawalRequest processingReq = WithdrawalRequest.builder()
                .withdrawalId(withdrawalId)
                .storeId(storeId)
                .ownerId(ownerId)
                .status("PROCESSING")
                .build();

        when(withdrawalRequestRepository.findByIdWithLock(withdrawalId)).thenReturn(Optional.of(processingReq));

        assertThatThrownBy(() -> withdrawalService.rejectWithdrawal(adminId, withdrawalId, "Bank failed"))
                .isInstanceOf(AppException.class)
                .matches(e -> ((AppException) e).getErrorCode() == ErrorCode.BAD_REQUEST);

        verify(merchantWalletService, never()).unlockWithdrawal(any(), any(), any(), any());
    }

    @Test
    @DisplayName("markForReconciliation transitions status and keeps money locked")
    void markForReconciliationKeepsLocked() {
        UUID withdrawalId = UUID.randomUUID();

        WithdrawalRequest processingReq = WithdrawalRequest.builder()
                .withdrawalId(withdrawalId)
                .storeId(storeId)
                .ownerId(ownerId)
                .status("PROCESSING")
                .build();

        when(withdrawalRequestRepository.findByIdWithLock(withdrawalId)).thenReturn(Optional.of(processingReq));
        when(withdrawalRequestRepository.save(any(WithdrawalRequest.class))).thenAnswer(i -> i.getArgument(0));

        WithdrawalRequest result = withdrawalService.markForReconciliation(adminId, withdrawalId, "Ambiguous gateway timeout");

        assertThat(result.getStatus()).isEqualTo("RECONCILIATION_REQUIRED");
        verify(merchantWalletService, never()).unlockWithdrawal(any(), any(), any(), any());
        verify(merchantWalletService, never()).completeWithdrawal(any(), any(), any());
    }

    @Test
    @DisplayName("resolveReconciliation with PAID debits locked balance and records audit")
    void resolveReconciliationPaid() {
        UUID withdrawalId = UUID.randomUUID();
        String txRef = "FT_RESOLVED_123";

        WithdrawalRequest req = WithdrawalRequest.builder()
                .withdrawalId(withdrawalId)
                .storeId(storeId)
                .ownerId(ownerId)
                .amount(new BigDecimal("200000.00"))
                .status("RECONCILIATION_REQUIRED")
                .build();

        when(withdrawalRequestRepository.findByIdWithLock(withdrawalId)).thenReturn(Optional.of(req));
        when(withdrawalRequestRepository.findByTransactionReferenceIgnoreCase(txRef)).thenReturn(Optional.empty());
        when(withdrawalRequestRepository.save(any(WithdrawalRequest.class))).thenAnswer(i -> i.getArgument(0));

        WithdrawalRequest resolved = withdrawalService.resolveReconciliation(
                adminId, withdrawalId, "PAID", txRef, "Bank statement confirmed transfer"
        );

        assertThat(resolved.getStatus()).isEqualTo("PAID");
        assertThat(resolved.getResolutionStatus()).isEqualTo("CONFIRMED_PAID");
        assertThat(resolved.getResolvedBy()).isEqualTo(adminId);
        assertThat(resolved.getTransactionReference()).isEqualTo(txRef);

        verify(merchantWalletService).completeWithdrawal(storeId, new BigDecimal("200000.00"), withdrawalId.toString());
        verify(merchantWalletService, never()).unlockWithdrawal(any(), any(), any(), any());
    }

    @Test
    @DisplayName("resolveReconciliation with REJECTED unlocks funds back to available and records audit")
    void resolveReconciliationRejected() {
        UUID withdrawalId = UUID.randomUUID();

        WithdrawalRequest req = WithdrawalRequest.builder()
                .withdrawalId(withdrawalId)
                .storeId(storeId)
                .ownerId(ownerId)
                .amount(new BigDecimal("200000.00"))
                .status("RECONCILIATION_REQUIRED")
                .build();

        when(withdrawalRequestRepository.findByIdWithLock(withdrawalId)).thenReturn(Optional.of(req));
        when(withdrawalRequestRepository.save(any(WithdrawalRequest.class))).thenAnswer(i -> i.getArgument(0));

        WithdrawalRequest resolved = withdrawalService.resolveReconciliation(
                adminId, withdrawalId, "REJECTED", null, "Bank verified money never left account"
        );

        assertThat(resolved.getStatus()).isEqualTo("REJECTED");
        assertThat(resolved.getResolutionStatus()).isEqualTo("CONFIRMED_FAILED");
        assertThat(resolved.getResolvedBy()).isEqualTo(adminId);

        verify(merchantWalletService).unlockWithdrawal(eq(storeId), eq(new BigDecimal("200000.00")), eq(withdrawalId.toString()), any());
        verify(merchantWalletService, never()).completeWithdrawal(any(), any(), any());
    }
}
