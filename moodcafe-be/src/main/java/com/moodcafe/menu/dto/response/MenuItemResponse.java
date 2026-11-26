package com.moodcafe.menu.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
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
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private Boolean hasSizes;
    @Builder.Default
    private List<MenuItemSizeResponse> sizes = new ArrayList<>();
    private String imageUrl;
    private Boolean isAvailable;
    private Instant createdAt;
    private Instant updatedAt;
}
