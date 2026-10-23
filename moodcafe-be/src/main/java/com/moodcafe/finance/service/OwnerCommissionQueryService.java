package com.moodcafe.finance.service;

import com.moodcafe.shared.error.ErrorCode;
import com.moodcafe.shared.exceptions.AppException;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OwnerCommissionQueryService {
    private final JdbcTemplate jdbc;
    private static final String STATE = """
        case when (select count(*) from booking_commission_entries e where e.booking_id=b.booking_id and e.event_type in ('SETTLE','REVERSE')) > 1 then 'RECONCILIATION_REQUIRED'
        when b.status in ('COMPLETED','CLAIMED_LATE','EXPIRED_NO_SHOW') and not exists(select 1 from booking_commission_entries e where e.booking_id=b.booking_id and e.event_type='SETTLE') then 'RECONCILIATION_REQUIRED'
        when b.status='CANCELLED_REFUNDED' and exists(select 1 from booking_commission_entries e where e.booking_id=b.booking_id and e.event_type='HOLD') and not exists(select 1 from booking_commission_entries e where e.booking_id=b.booking_id and e.event_type='REVERSE') then 'RECONCILIATION_REQUIRED'
        when exists(select 1 from booking_commission_entries e where e.booking_id=b.booking_id and e.event_type='REVERSE') then 'REVERSED'
        when exists(select 1 from booking_commission_entries e where e.booking_id=b.booking_id and e.event_type='SETTLE') then 'SETTLED'
        when exists(select 1 from booking_commission_entries e where e.booking_id=b.booking_id and e.event_type='HOLD') then 'PENDING'
        when b.status in ('PENDING_PAYMENT','PAYMENT_FAILED') and b.paid_at is null then 'UNPAID'
        when b.status='CANCELLED_REFUNDED' and b.actual_paid_amount=0 then 'UNPAID'
        else 'RECONCILIATION_REQUIRED' end
        """;
    private static final String ROW = """
        select b.booking_id as "bookingId", b.booking_code as "bookingCode", b.status as "bookingStatus",
        b.paid_at as "paidAt", b.gross_amount as "grossAmount", b.commission_rate_snapshot as "commissionRateSnapshot",
        b.commission_amount as "commissionAmount", b.merchant_net_amount as "merchantNetAmount",
        b.voucher_discount as "voucherDiscount", b.customer_paid_amount as "customerPaidAmount",
        b.voucher_refund_funded_amount as "voucherRefundFundedAmount", b.voucher_reward_funded_amount as "voucherRewardFundedAmount",
        b.commission_policy_id as "commissionPolicyId",
        (select max(e.occurred_at) from booking_commission_entries e where e.booking_id=b.booking_id and e.event_type='SETTLE') as "settledAt",
        """ + STATE + " as \"commissionState\" from bookings b ";
    public record Period(Instant from, Instant to) {}
    public Period period(Instant from, Instant to) {
        ZoneId zone=ZoneId.of("Asia/Ho_Chi_Minh");
        Instant start=from==null ? LocalDate.now(zone).withDayOfMonth(1).atStartOfDay(zone).toInstant() : from;
        Instant end=to==null ? LocalDate.now(zone).plusDays(1).atStartOfDay(zone).toInstant() : to;
        if (!end.isAfter(start) || Duration.between(start,end).compareTo(Duration.ofDays(366))>0)
            throw new AppException(ErrorCode.BAD_REQUEST,"Khoảng ngày phải lớn hơn 0 và tối đa 366 ngày");
        return new Period(start,end);
    }
    public Map<String,Object> summary(UUID storeId, Period p) {
        Map<String,Object> result=new LinkedHashMap<>(jdbc.queryForMap("""
            select coalesce(sum(gross_amount),0) as "paidGrossAmount", coalesce(sum(merchant_net_amount),0) as "paidMerchantNetAmount",
            count(*) as "paidBookingCount" from bookings where store_id=? and paid_at>=? and paid_at<? and is_deleted=false
            """,storeId,Timestamp.from(p.from()),Timestamp.from(p.to())));
        result.putAll(jdbc.queryForMap("""
            select coalesce(sum(case when event_type='HOLD' then amount else 0 end),0) as "commissionHeldInPeriod",
            coalesce(sum(case when event_type='SETTLE' then amount else 0 end),0) as "commissionSettledInPeriod",
            coalesce(sum(case when event_type='REVERSE' then amount else 0 end),0) as "commissionReversedInPeriod"
            from booking_commission_entries where store_id=? and occurred_at>=? and occurred_at<?
            """,storeId,Timestamp.from(p.from()),Timestamp.from(p.to())));
        result.putAll(jdbc.queryForMap("""
            select coalesce(sum(case when event_type='HOLD' then amount when event_type in ('SETTLE','REVERSE') then -amount else 0 end),0) as "commissionPendingNow",
            coalesce(sum(case when event_type='SETTLE' then amount else 0 end),0) as "commissionSettledAllTime"
            from booking_commission_entries where store_id=?
            """,storeId));
        result.put("reconciliationBookingCount",jdbc.queryForObject("select count(*) from bookings b where b.store_id=? and b.is_deleted=false and b.paid_at is not null and ("+STATE+")='RECONCILIATION_REQUIRED'",Long.class,storeId));
        result.put("from",p.from());result.put("to",p.to());
        return result;
    }
    public Map<String,Object> bookings(UUID storeId, Period p, String status, String code, int page, int size) {
        if(page<0 || size<1 || size>100) throw new AppException(ErrorCode.BAD_REQUEST,"Phân trang không hợp lệ (size 1–100)");
        if(status!=null && !Set.of("PENDING","SETTLED","REVERSED","UNPAID","RECONCILIATION_REQUIRED").contains(status))
            throw new AppException(ErrorCode.BAD_REQUEST,"Trạng thái hoa hồng không hợp lệ");
        String where=" where b.store_id=? and b.is_deleted=false and b.paid_at>=? and b.paid_at<?";
        List<Object> args=new ArrayList<>(List.of(storeId,Timestamp.from(p.from()),Timestamp.from(p.to())));
        if(status!=null){where+=" and ("+STATE+")=?";args.add(status);}
        if(code!=null && !code.isBlank()) {if(code.length()>40) throw new AppException(ErrorCode.BAD_REQUEST,"Mã đơn quá dài");where+=" and position(lower(?) in lower(b.booking_code))>0";args.add(code.trim());}
        long total=Objects.requireNonNull(jdbc.queryForObject("select count(*) from bookings b"+where,Long.class,args.toArray()));
        args.add(size);args.add((long)page*size);
        List<Map<String,Object>> rows=jdbc.queryForList(ROW+where+" order by b.paid_at desc,b.booking_id desc limit ? offset ?",args.toArray());
        return Map.of("content",rows,"totalElements",total,"totalPages",(total+size-1)/size,"page",page,"size",size);
    }
    public Map<String,Object> detail(UUID storeId,UUID bookingId) {
        List<Map<String,Object>> rows=jdbc.queryForList(ROW+" where b.store_id=? and b.booking_id=? and b.is_deleted=false",storeId,bookingId);
        if(rows.isEmpty())throw new AppException(ErrorCode.NOT_FOUND,"Không tìm thấy đơn thuộc cơ sở này");
        Map<String,Object> detail=new LinkedHashMap<>(rows.get(0));
        detail.put("entries",jdbc.queryForList("""
            select entry_id as "entryId", event_type as "eventType", amount, currency, source, occurred_at as "occurredAt",
            original_entry_id as "originalEntryId" from booking_commission_entries where store_id=? and booking_id=? order by occurred_at,entry_id
            """,storeId,bookingId));
        return detail;
    }
}
