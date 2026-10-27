package com.moodcafe.wallet.service;

import com.moodcafe.shared.error.ErrorCode;
import com.moodcafe.shared.exceptions.AppException;
import com.moodcafe.wallet.abstraction.repository.PayoutBookingAllocationRepository;
import com.moodcafe.wallet.entity.WithdrawalRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service managing payout attribution breakdown for store withdrawals.
 * Database operations are delegated to PayoutBookingAllocationRepository.
 */
@Service
@RequiredArgsConstructor
public class PayoutBreakdownService {

    private final PayoutBookingAllocationRepository allocationRepository;

    @Transactional(propagation = Propagation.MANDATORY)
    public void snapshot(WithdrawalRequest request) {
        BigDecimal legacyUsed = allocationRepository.findLegacyUsedAmount(request.getStoreId(), request.getWithdrawalId());
        var credits = allocationRepository.findEligibleCredits(request.getStoreId());

        BigDecimal remaining = request.getAmount();
        for (var row : credits) {
            BigDecimal credit = ((BigDecimal) row.get("remaining")).max(BigDecimal.ZERO);
            BigDecimal skip = legacyUsed.min(credit);
            legacyUsed = legacyUsed.subtract(skip);
            credit = credit.subtract(skip);

            BigDecimal used = credit.min(remaining);
            if (used.signum() > 0) {
                allocationRepository.insertAllocation(
                        request.getWithdrawalId(),
                        row.get("transaction_id"),
                        row.get("booking_id"),
                        row.get("booking_code"),
                        row.get("reference_type"),
                        row.get("reference_id"),
                        used
                );
                remaining = remaining.subtract(used);
            }
            if (remaining.signum() == 0) {
                return;
            }
        }
        throw new AppException(ErrorCode.CONFLICT, "Số dư chưa có đủ bảng kê nguồn tiền, cần Admin đối soát trước khi rút");
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> get(UUID id) {
        if (!allocationRepository.existsWithdrawal(id)) {
            throw new AppException(ErrorCode.NOT_FOUND);
        }
        return allocationRepository.findAllocationsByWithdrawalId(id);
    }
}
