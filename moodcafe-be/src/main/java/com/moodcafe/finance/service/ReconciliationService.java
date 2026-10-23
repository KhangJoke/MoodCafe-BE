package com.moodcafe.finance.service;

import com.moodcafe.reservation.abstraction.repository.ReservationRepository;
import com.moodcafe.voucher.abstraction.repository.CustomerVoucherRepository;
import com.moodcafe.wallet.abstraction.repository.CustomerWalletRepository;
import com.moodcafe.wallet.abstraction.repository.MerchantWalletRepository;
import com.moodcafe.wallet.abstraction.repository.WithdrawalRequestRepository;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReconciliationService {

    private final ReservationRepository reservationRepository;
    private final CustomerWalletRepository customerWalletRepository;
    private final MerchantWalletRepository merchantWalletRepository;
    private final WithdrawalRequestRepository withdrawalRequestRepository;

    @Transactional(readOnly = true)
    public ReconciliationSummary getSummary() {
        // Aggregates across wallets and reservations
        long totalCustomerPoints = customerWalletRepository.findAll().stream()
                .mapToLong(w -> w.getPointsBalance() != null ? w.getPointsBalance() : 0L)
                .sum();

        BigDecimal totalMerchantPending = merchantWalletRepository.findAll().stream()
                .map(w -> w.getPendingBalance() != null ? w.getPendingBalance() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalMerchantAvailable = merchantWalletRepository.findAll().stream()
                .map(w -> w.getAvailableBalance() != null ? w.getAvailableBalance() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalMerchantLocked = merchantWalletRepository.findAll().stream()
                .map(w -> w.getLockedBalance() != null ? w.getLockedBalance() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return ReconciliationSummary.builder()
                .totalCustomerPointsObligation(totalCustomerPoints)
                .totalMerchantPendingVnd(totalMerchantPending)
                .totalMerchantAvailableVnd(totalMerchantAvailable)
                .totalMerchantLockedVnd(totalMerchantLocked)
                .build();
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ReconciliationSummary {
        private Long totalCustomerPointsObligation; // 1 point = 1 VND value
        private BigDecimal totalMerchantPendingVnd;
        private BigDecimal totalMerchantAvailableVnd;
        private BigDecimal totalMerchantLockedVnd;
    }
}
