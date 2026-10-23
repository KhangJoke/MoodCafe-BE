package com.moodcafe.finance.service;

import com.moodcafe.finance.abstraction.repository.CommissionPolicyRepository;
import com.moodcafe.finance.dto.ReservationFinancialSnapshot;
import com.moodcafe.finance.dto.ReservationPricingInput;
import com.moodcafe.finance.entity.CommissionPolicy;
import com.moodcafe.shared.error.ErrorCode;
import com.moodcafe.shared.exceptions.AppException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationPricingService {

    private static final BigDecimal DEFAULT_COMMISSION_RATE = new BigDecimal("0.1500");

    private final CommissionPolicyRepository commissionPolicyRepository;

    /**
     * Calculates the guaranteed immutable 5 financial amounts for a reservation.
     * Enforces anti-loss rules:
     * 1. Voucher discount cannot exceed gross amount.
     * 2. Reward-funded discount cannot exceed platform gross commission.
     * 3. Refund-funded discount is guaranteed from prior verified customer funds and NOT blocked by commission.
     * 4. Merchant net amount is guaranteed: merchant_net = gross - commission.
     */
    public ReservationFinancialSnapshot quote(ReservationPricingInput input) {
        if (input == null || input.getGrossAmount() == null || input.getGrossAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Giá trị đơn hàng (gross_amount) phải lớn hơn 0");
        }

        BigDecimal grossAmount = input.getGrossAmount().setScale(0, RoundingMode.HALF_UP);
        Instant now = Instant.now();

        // 1. Resolve commission rate (store override -> global policy -> default 15%)
        BigDecimal commissionRate = resolveCommissionRate(input, now);

        // 2. Gross commission amount
        BigDecimal commissionAmount = grossAmount.multiply(commissionRate).setScale(0, RoundingMode.HALF_UP);

        // 3. Resolve voucher discount & funding breakdown
        BigDecimal voucherDiscount = input.getVoucherDiscount() != null
                ? input.getVoucherDiscount().setScale(0, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        if (voucherDiscount.compareTo(BigDecimal.ZERO) < 0) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Giá trị voucher không hợp lệ");
        }

        if (voucherDiscount.compareTo(grossAmount) > 0) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Giá trị voucher (" + voucherDiscount + "đ) không được vượt quá giá trị đơn đặt bàn (" + grossAmount + "đ)");
        }

        BigDecimal refundFunded = input.getVoucherRefundFunded() != null
                ? input.getVoucherRefundFunded().setScale(0, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        BigDecimal rewardFunded = input.getVoucherRewardFunded() != null
                ? input.getVoucherRewardFunded().setScale(0, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // Auto-balance funding if discount is provided without explicit split
        if (voucherDiscount.compareTo(BigDecimal.ZERO) > 0 && refundFunded.add(rewardFunded).compareTo(BigDecimal.ZERO) == 0) {
            rewardFunded = voucherDiscount;
        }

        // Anti-loss guard: REWARD funded discount must NOT exceed commission
        if (rewardFunded.compareTo(commissionAmount) > 0) {
            throw new AppException(ErrorCode.BAD_REQUEST,
                    "Mức giảm giá từ điểm thưởng (" + rewardFunded + "đ) vượt quá giới hạn trợ giá của sàn (tối đa bằng hoa hồng " + commissionAmount + "đ)");
        }

        // 4. Calculate Customer Paid Amount and Merchant Net Amount
        BigDecimal customerPaidAmount = grossAmount.subtract(voucherDiscount).setScale(0, RoundingMode.HALF_UP);
        BigDecimal merchantNetAmount = grossAmount.subtract(commissionAmount).setScale(0, RoundingMode.HALF_UP);
        BigDecimal platformNetRevenue = commissionAmount.subtract(rewardFunded).setScale(0, RoundingMode.HALF_UP);

        return ReservationFinancialSnapshot.builder()
                .grossAmount(grossAmount)
                .voucherDiscount(voucherDiscount)
                .customerPaidAmount(customerPaidAmount)
                .commissionRate(commissionRate)
                .commissionAmount(commissionAmount)
                .merchantNetAmount(merchantNetAmount)
                .platformNetRevenue(platformNetRevenue)
                .voucherRefundFundedAmount(refundFunded)
                .voucherRewardFundedAmount(rewardFunded)
                .build();
    }

    private BigDecimal resolveCommissionRate(ReservationPricingInput input, Instant now) {
        if (input.getStoreId() != null) {
            List<CommissionPolicy> storePolicies = commissionPolicyRepository.findActiveStorePolicies(input.getStoreId(), now);
            if (!storePolicies.isEmpty()) {
                return storePolicies.get(0).getCommissionRate();
            }
        }

        List<CommissionPolicy> globalPolicies = commissionPolicyRepository.findActiveGlobalPolicies(now);
        if (!globalPolicies.isEmpty()) {
            return globalPolicies.get(0).getCommissionRate();
        }

        return DEFAULT_COMMISSION_RATE;
    }
}
