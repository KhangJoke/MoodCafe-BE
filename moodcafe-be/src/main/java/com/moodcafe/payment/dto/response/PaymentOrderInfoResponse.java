package com.moodcafe.payment.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentOrderInfoResponse {

    private Long orderCode;

    private String paymentType; // "BOOKING", "SUBSCRIPTION", "SPONSORED", or "UNKNOWN"

    private String status;      // e.g. "PENDING", "PAID", "CANCELLED", "FAILED"

    private Long amount;

    private String title;

    private String referenceCode; // bookingCode or transactionCode

    private String redirectUrl;   // Suggested URL on frontend
}
