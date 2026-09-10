package com.moodcafe.store.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
    private String status;
    private List<StoreImageResponse> images;
    private List<AmenityResponse> amenities;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
