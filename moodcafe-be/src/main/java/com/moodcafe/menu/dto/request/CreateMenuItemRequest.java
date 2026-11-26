package com.moodcafe.menu.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateMenuItemRequest {

    @NotNull(message = "Mã danh mục không được để trống")
    private UUID categoryId;

    @NotBlank(message = "Tên món không được để trống")
    @Size(max = 150, message = "Tên món không được vượt quá 150 ký tự")
    private String name;

    private String description;

    /**
     * Required when sizes is empty/null. When sizes are provided, optional (auto-filled from default or min size price).
     */
    @Min(value = 0, message = "Giá không được nhỏ hơn 0")
    private BigDecimal price;

    private String imageUrl;

    @Builder.Default
    private Boolean isAvailable = true;

    @Valid
    private List<MenuItemSizeRequest> sizes;
}
