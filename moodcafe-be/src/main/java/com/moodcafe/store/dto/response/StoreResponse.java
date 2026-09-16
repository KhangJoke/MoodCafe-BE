package com.moodcafe.store.dto.response;

import com.moodcafe.store.entity.enums.StoreStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoreResponse {

    private UUID storeId;
    private String name;
    private String description;
    private String address;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private LocalTime openingTime;
    private LocalTime closingTime;
    private String priceRange;
    private String phone;
    private String email;
    private StoreStatus status;
    private List<StoreImageResponse> images;
    private List<AmenityResponse> amenities;
    private Instant createdAt;
    private Instant updatedAt;
}
