package com.moodcafe.wallet.service;

import com.moodcafe.wallet.entity.WithdrawalRequest;
import com.moodcafe.shared.error.ErrorCode;
import com.moodcafe.shared.exceptions.AppException;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.math.BigDecimal;
import java.util.*;

@Service
@RequiredArgsConstructor
public class PayoutBreakdownService {
    private final JdbcTemplate jdbc;
    // Caller holds the store wallet lock. Persist an immutable attribution at request time.
    @Transactional(propagation=Propagation.MANDATORY)
    public void snapshot(WithdrawalRequest request) {
        BigDecimal legacyUsed = jdbc.queryForObject("""
            select coalesce(sum(amount),0) from merchant_withdrawals w
            where store_id=? and withdrawal_id<>? and status not in ('REJECTED','CANCELLED')
            and not exists(select 1 from payout_booking_allocations a where a.withdrawal_id=w.withdrawal_id)
            """,BigDecimal.class,request.getStoreId(),request.getWithdrawalId());
        var credits=jdbc.queryForList("""
            select t.transaction_id,t.reference_type,t.reference_id,b.booking_id,
              coalesce(b.booking_code,t.reference_id,'LEGACY') booking_code,
              t.amount-coalesce((select sum(a.amount) from payout_booking_allocations a
                join merchant_withdrawals w on w.withdrawal_id=a.withdrawal_id
                where a.source_transaction_id=t.transaction_id and w.status not in ('REJECTED','CANCELLED')),0) remaining
            from merchant_wallet_transactions t
            join merchant_wallets mw on mw.wallet_id=t.wallet_id
            left join bookings b on t.reference_type='BOOKING' and t.reference_id=b.booking_id::text
            where mw.store_id=? and t.tx_type='RELEASE_TO_AVAILABLE' and t.amount>0
            order by t.created_at,t.transaction_id
            """,request.getStoreId());
        BigDecimal remaining=request.getAmount();
        for(var row:credits) {
            BigDecimal credit=((BigDecimal)row.get("remaining")).max(BigDecimal.ZERO);
            BigDecimal skip=legacyUsed.min(credit); legacyUsed=legacyUsed.subtract(skip); credit=credit.subtract(skip);
            BigDecimal used=credit.min(remaining);
            if(used.signum()>0) {
                jdbc.update("insert into payout_booking_allocations(withdrawal_id,source_transaction_id,booking_id,booking_code,source_reference_type,source_reference_id,amount) values(?,?,?,?,?,?,?)",
                    request.getWithdrawalId(),row.get("transaction_id"),row.get("booking_id"),row.get("booking_code"),row.get("reference_type"),row.get("reference_id"),used);
                remaining=remaining.subtract(used);
            }
            if(remaining.signum()==0) return;
        }
        throw new AppException(ErrorCode.CONFLICT,"Số dư chưa có đủ bảng kê nguồn tiền, cần Admin đối soát trước khi rút");
    }
    @Transactional(readOnly=true)
    public List<Map<String,Object>> get(UUID id) {
        if(jdbc.queryForObject("select count(*) from merchant_withdrawals where withdrawal_id=?",Long.class,id)==0)
            throw new AppException(ErrorCode.NOT_FOUND);
        return jdbc.queryForList("select source_transaction_id as \"sourceTransactionId\", booking_id as \"bookingId\", booking_code as \"bookingCode\", amount from payout_booking_allocations where withdrawal_id=? order by booking_code",id);
    }
}
