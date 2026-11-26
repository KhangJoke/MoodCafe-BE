package com.moodcafe.menu.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MenuItemSizeRequest {

    @NotBlank(message = "Tên size không được để trống")
    @Size(max = 50, message = "Tên size không được vượt quá 50 ký tự")
    private String sizeName;

    @NotNull(message = "Giá của size không được để trống")
    @DecimalMin(value = "0", message = "Giá của size không được nhỏ hơn 0")
    private BigDecimal price;

    @Builder.Default
    private Integer displayOrder = 0;

    @Builder.Default
    private Boolean isDefault = false;

    @Builder.Default
    private Boolean isAvailable = true;
}
