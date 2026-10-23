package com.moodcafe.reservation.dto;

import com.moodcafe.reservation.entity.Reservation;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class ReservationStaffResponse {
    private UUID reservationId;
    private UUID storeId;
    private UUID userId;
    private UUID categoryId;
    private UUID tableId;
    private Integer guestCount;
    private Instant bookingTime;
    private Instant graceDeadline;
    private Instant serviceClosingDeadline;
    private BigDecimal grossAmount;
    private String status;
    private Instant checkInTime;
    private Instant checkOutTime;
    private Instant claimedLateTime;
    private String notes;
    private Instant createdAt;

    public static ReservationStaffResponse fromEntity(Reservation r) {
        return ReservationStaffResponse.builder()
                .reservationId(r.getReservationId())
                .storeId(r.getStoreId())
                .userId(r.getUserId())
                .categoryId(r.getCategoryId())
                .tableId(r.getTableId())
                .guestCount(r.getGuestCount())
                .bookingTime(r.getBookingTime())
                .graceDeadline(r.getGraceDeadline())
                .serviceClosingDeadline(r.getServiceClosingDeadline())
                .grossAmount(r.getGrossAmount())
                .status(r.getStatus())
                .checkInTime(r.getCheckInTime())
                .checkOutTime(r.getCheckOutTime())
                .claimedLateTime(r.getClaimedLateTime())
                .notes(r.getNotes())
                .createdAt(r.getCreatedAt())
                .build();
    }

    public static ReservationStaffResponse fromBooking(com.moodcafe.booking.entity.Booking b) {
        return ReservationStaffResponse.builder()
                .reservationId(b.getBookingId())
                .storeId(b.getStore() != null ? b.getStore().getStoreId() : null)
                .userId(b.getUser() != null ? b.getUser().getUserId() : null)
                .categoryId(b.getTableCategory() != null ? b.getTableCategory().getCategoryId() : null)
                .tableId(null)
                .guestCount(b.getGuestCount())
                .bookingTime(b.getStartTime())
                .graceDeadline(b.getGraceDeadline())
                .serviceClosingDeadline(b.getServiceClosingDeadline())
                .grossAmount(b.getGrossAmount())
                .status(b.getStatus() != null ? b.getStatus().name() : "PENDING_PAYMENT")
                .checkInTime(b.getCheckedInAt())
                .checkOutTime(b.getCompletedAt())
                .claimedLateTime(b.getClaimedLateAt())
                .notes(b.getNote())
                .createdAt(b.getCreatedAt())
                .build();
    }
}
