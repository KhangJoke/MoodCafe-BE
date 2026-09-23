package com.moodcafe.store.dto.response;

import com.moodcafe.store.entity.enums.StoreStatus;
import com.moodcafe.tag.dto.response.StoreTagResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoreRegistrationStatusResponse {

    private UUID storeId;
    private String name;
    private String description;
    private String address;
    private String district;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private LocalTime openingTime;
    private LocalTime closingTime;
    private Long priceFrom;
    private Long priceTo;
    private String phone;
    private String email;
    private StoreStatus status;
    private String rejectReason;

    @Builder.Default
    private boolean allowResubmit = true;

    @Builder.Default
    private List<StoreImageResponse> images = new ArrayList<>();

    @Builder.Default
    private List<StoreTagResponse> tags = new ArrayList<>();

    private Instant createdAt;
    private Instant updatedAt;
}
