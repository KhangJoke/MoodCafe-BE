package com.moodcafe.payment.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PayOSWebhookRequest {
    private String code; // "00" là thành công
    private String desc;
    private PayOSWebhookData data;
    private String signature;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PayOSWebhookData {
        private Long orderCode;
        private Double amount;
        private String description;
        private String accountNumber;
        private String reference;
        private String transactionDateTime;
        private String currency;
        private String paymentLinkId;
        private String code;
        private String desc;
    }
}
