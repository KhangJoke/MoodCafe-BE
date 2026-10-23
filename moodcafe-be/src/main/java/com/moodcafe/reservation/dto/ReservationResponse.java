package com.moodcafe.reservation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservationResponse {
    private UUID reservationId;
    private UUID storeId;
    private UUID userId;
    private UUID categoryId;
    private Instant bookingTime;
    private Integer guestCount;
    private String status;

    private BigDecimal grossAmount;
    private BigDecimal voucherDiscount;
    private BigDecimal customerPaidAmount;
    private BigDecimal commissionAmount;
    private BigDecimal merchantNetAmount;

    private UUID voucherId;
    private Long payosOrderCode;
    private String paymentUrl;
    private Instant checkInTime;
    private Instant checkOutTime;
    private Instant graceDeadline;
    private String notes;
}
