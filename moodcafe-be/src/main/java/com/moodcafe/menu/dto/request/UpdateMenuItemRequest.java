package com.moodcafe.menu.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
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
public class UpdateMenuItemRequest {

    private UUID categoryId;

    @Size(max = 150, message = "Tên món không được vượt quá 150 ký tự")
    private String name;

    private String description;

    @Min(value = 0, message = "Giá không được nhỏ hơn 0")
    private BigDecimal price;

    private String imageUrl;

    private Boolean isAvailable;
}
