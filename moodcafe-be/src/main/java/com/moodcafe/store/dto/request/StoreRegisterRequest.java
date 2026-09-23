package com.moodcafe.store.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoreRegisterRequest {

    @NotBlank(message = "Tên quán không được để trống")
    @Size(max = 255, message = "Tên quán tối đa 255 ký tự")
    private String name;

    private String description;

    @NotBlank(message = "Địa chỉ quán không được để trống")
    @Size(max = 500, message = "Địa chỉ quán tối đa 500 ký tự")
    private String address;

    @Size(max = 100, message = "Quận/huyện tối đa 100 ký tự")
    private String district;

    @NotNull(message = "Tọa độ vĩ độ (latitude) là bắt buộc")
    private BigDecimal latitude;

    @NotNull(message = "Tọa độ kinh độ (longitude) là bắt buộc")
    private BigDecimal longitude;

    private LocalTime openingTime;

    private LocalTime closingTime;


    private Long priceFrom;

    private Long priceTo;

    @Size(max = 20, message = "Số điện thoại tối đa 20 ký tự")
    private String phone;

    @Email(message = "Định dạng email không hợp lệ")
    @Size(max = 255, message = "Email tối đa 255 ký tự")
    private String email;

    @Builder.Default
    private List<String> imageUrls = new ArrayList<>();

    @NotEmpty(message = "Vui lòng chọn ít nhất 1 thẻ vibe khi đăng ký quán")
    @Valid
    @Builder.Default
    private List<RegisterStoreTagItem> tags = new ArrayList<>();
}
