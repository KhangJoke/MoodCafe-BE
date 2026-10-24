package com.moodcafe.booking.service;

import com.moodcafe.booking.abstraction.repository.BookingRepository;
import com.moodcafe.booking.entity.enums.BookingStatus;
import com.moodcafe.store.abstraction.service.StoreStaffService;
import com.moodcafe.shared.error.ErrorCode;
import com.moodcafe.shared.exceptions.AppException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BookingCheckoutChallengeService {
    private final StringRedisTemplate redis;
    private final BookingRepository bookings;
    private final StoreStaffService staff;
    public record Challenge(String token,Instant expiresAt) {}
    public Challenge issue(UUID storeId,UUID bookingId) {
        staff.requireStoreAccess(storeId,"OWNER","MANAGER","CASHIER");
        var booking=bookings.findById(bookingId).orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));
        if(!booking.getStore().getStoreId().equals(storeId)) throw new AppException(ErrorCode.FORBIDDEN_STORE_ACCESS);
        if(booking.getStatus()!=BookingStatus.CHECKED_IN) throw new AppException(ErrorCode.INVALID_BOOKING_STATUS_TRANSITION);
        String token=UUID.randomUUID().toString()+UUID.randomUUID();
        redis.opsForValue().set("booking:checkout:"+token,bookingId.toString(),Duration.ofMinutes(2));
        return new Challenge(token,Instant.now().plusSeconds(120));
    }
    public void verify(UUID bookingId,String token) {
        if(token==null || token.length()>100 || !bookingId.toString().equals(redis.opsForValue().get("booking:checkout:"+token)))
            throw new AppException(ErrorCode.FORBIDDEN_ACTION,"Mã trả bàn không hợp lệ hoặc đã hết hạn, vui lòng quét mã tại quầy");
    }
}
