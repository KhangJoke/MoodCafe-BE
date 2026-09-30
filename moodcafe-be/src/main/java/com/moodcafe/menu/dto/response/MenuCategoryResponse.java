package com.moodcafe.menu.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MenuCategoryResponse {

    private UUID categoryId;
    private UUID storeId;
    private String name;
    private Integer displayOrder;
    private long itemCount;
    private Instant createdAt;
    private Instant updatedAt;
}
