package com.moodcafe.menu.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MenuItemSizeResponse {

    private UUID sizeId;
    private String sizeName;
    private BigDecimal price;
    private Integer displayOrder;
    private Boolean isDefault;
    private Boolean isAvailable;
}
