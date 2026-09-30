package com.moodcafe.menu.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MenuItemResponse {

    private UUID itemId;
    private UUID storeId;
    private UUID categoryId;
    private String categoryName;
    private String name;
    private String description;
    private BigDecimal price;
    private String imageUrl;
    private Boolean isAvailable;
    private Instant createdAt;
    private Instant updatedAt;
}
