package com.moodcafe.booking.dto.response;

import com.moodcafe.booking.entity.enums.BookingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingResponse {

    private UUID bookingId;
    private String bookingCode;
    private UUID storeId;
    private String storeName;
    private String storeAddress;
    private String storePhone;
    private String storeImageUrl;
    private UUID categoryId;
    private String categoryName;
    private String categoryImageUrl;
    private Integer maxCapacity;
    private UUID userId;
    private String customerName;
    private String customerPhone;
    private String customerEmail;
    private Instant startTime;
    private Instant endTime;
    private BookingStatus status;
    private Long minSpendAmount;
    private Long paymentOrderCode;
    private String checkoutUrl;
    private String qrCodeToken;
    private Instant checkedInAt;
    private Instant completedAt;
    private Instant claimedLateAt;
    private Instant cancelledAt;
    private String cancelReason;
    private Boolean refundProcessed;
    private String note;
    private Integer gracePeriodMins;
    private Integer freeCancelHours;
    private Boolean isCancelable;
    private Instant createdAt;
}
