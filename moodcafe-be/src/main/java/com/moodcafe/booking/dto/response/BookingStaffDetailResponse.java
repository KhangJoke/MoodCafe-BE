package com.moodcafe.booking.dto.response;

import com.moodcafe.booking.entity.enums.BookingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingStaffDetailResponse {

    private UUID bookingId;
    private String bookingCode;
    private String customerName;
    private String customerPhone;
    private Integer guestCount;
    private String categoryName;
    private Instant startTime;
    private Instant endTime;
    private BookingStatus status;
    private BigDecimal grossAmount;
    private Instant graceDeadline;
    private Instant serviceClosingDeadline;
    private List<String> allowedActions;
}
