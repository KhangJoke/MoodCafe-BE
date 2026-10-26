package com.moodcafe.booking.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateTableCategoryRequest {

    @NotBlank(message = "Tên loại bàn không được để trống")
    @Size(max = 255, message = "Tên loại bàn không quá 255 ký tự")
    private String name;

    private String description;

    @NotNull(message = "Sức chứa tối đa không được để trống")
    @Min(value = 1, message = "Sức chứa tối thiểu 1 người")
    @Max(value = 100, message = "Sức chứa tối đa 100 người")
    private Integer maxCapacity;

    @NotNull(message = "Quota số lượng bàn trên App không được để trống")
    @Min(value = 0, message = "Số lượng bàn không thể âm")
    @Max(value = 1000, message = "Số lượng bàn tối đa 1000")
    private Integer appQuota;

    @NotNull(message = "Giá trị gói đồ uống trả trước không được để trống")
    @Min(value = 0, message = "Giá trị gói đồ uống trả trước không thể âm")
    private Long minSpendAmount;

    @Size(max = 1000, message = "Đường dẫn ảnh không quá 1000 ký tự")
    private String imageUrl;

    @Builder.Default
    private Boolean isActive = true;
}
