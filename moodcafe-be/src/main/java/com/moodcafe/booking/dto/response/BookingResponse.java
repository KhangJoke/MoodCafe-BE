package com.moodcafe.booking.dto.response;

import com.moodcafe.booking.entity.enums.BookingStatus;
import com.moodcafe.gamification.dto.response.EquippedTitleDto;
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
    private Integer guestCount;
    private UUID userId;
    private String customerName;
    private String customerPhone;
    private String customerEmail;
    private EquippedTitleDto customerEquippedTitle;
    private Instant startTime;
    private Instant endTime;
    private BookingStatus status;
    private Long minSpendAmount;
    private BigDecimal grossAmount;
    private BigDecimal voucherDiscount;
    private BigDecimal customerPaidAmount;
    private BigDecimal actualPaidAmount;
    private Long paymentOrderCode;
    private Instant paymentExpiresAt;
    private String paymentSessionStatus;
    private String checkoutUrl;
    private String paymentQrCode;
    private String paymentAccountNumber;
    private String paymentAccountName;
    private String paymentBin;
    private String paymentDescription;
    private String qrCodeToken;
    private String physicalTableNumber;
    private Instant checkedInAt;
    private Instant checkOutRequestedAt;
    private Instant completedAt;
    private Boolean isEarlyCheckIn;
    private Instant claimedLateAt;
    private Instant cancelledAt;
    private String cancelReason;
    private Boolean refundProcessed;
    private RefundOutcomeResponse refundOutcome;
    private List<String> allowedActions;
    private String note;
    private Integer gracePeriodMins;
    private Integer freeCancelHours;
    private Boolean isCancelable;
    private Instant freeCancelDeadline;
    private Instant graceDeadline;
    private Instant serviceClosingDeadline;
    private Instant createdAt;
}
