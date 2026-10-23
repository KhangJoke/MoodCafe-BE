package com.moodcafe.payment.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GatewayPaymentObservation {

    public enum GatewayStatus {
        PAID,
        PENDING,
        CANCELLED,
        EXPIRED,
        UNKNOWN
    }

    private GatewayStatus status;
    private Long orderCode;
    private BigDecimal amount;
    private String currency;
    private String gatewayPaymentLinkId;
    private String description;
}
