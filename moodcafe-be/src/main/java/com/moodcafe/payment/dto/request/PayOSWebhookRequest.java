package com.moodcafe.payment.dto.request;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PayOSWebhookRequest {
    private String code; // "00" là thành công
    private String desc;
    private PayOSWebhookData data;
    private String signature;

    // Preserve gateway fields that participate in signature verification, including
    // counterAccount* / virtualAccount* fields and future SDK additions.
    @JsonIgnore
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private final Map<String, Object> additionalFields = new LinkedHashMap<>();

    @JsonAnySetter
    public void putAdditionalField(String name, Object value) {
        additionalFields.put(name, value);
    }

    @JsonAnyGetter
    public Map<String, Object> additionalFields() {
        return additionalFields;
    }

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

        // Preserve gateway fields that participate in signature verification, including
        // counterAccount* / virtualAccount* fields and future SDK additions.
        @JsonIgnore
        @Getter(AccessLevel.NONE)
        @Setter(AccessLevel.NONE)
        private final Map<String, Object> additionalFields = new LinkedHashMap<>();

        @JsonAnySetter
        public void putAdditionalField(String name, Object value) {
            additionalFields.put(name, value);
        }

        @JsonAnyGetter
        public Map<String, Object> additionalFields() {
            return additionalFields;
        }

    }
}
