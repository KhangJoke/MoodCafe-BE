package com.moodcafe.gamification.dto.request;

import com.moodcafe.gamification.entity.enums.TitleTier;
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
public class UpdateTitleAdminRequest {

    @NotBlank(message = "Tên danh hiệu không được để trống")
    @Size(max = 150, message = "Tên danh hiệu tối đa 150 ký tự")
    private String name;

    private String description;

    @NotNull(message = "Bậc danh hiệu không được để trống")
    private TitleTier tier;

    @NotBlank(message = "Mã màu không được để trống")
    @Size(max = 10, message = "Mã màu tối đa 10 ký tự")
    private String colorHex;

    private String iconUrl;

    private Boolean isActive;
}

