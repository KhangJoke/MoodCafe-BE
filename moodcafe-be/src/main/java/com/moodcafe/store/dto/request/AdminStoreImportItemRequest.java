package com.moodcafe.store.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
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
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminStoreImportItemRequest {

    private Integer rowNumber;

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
    @DecimalMin(value = "-90.0", message = "Vĩ độ phải nằm trong khoảng -90 đến 90")
    @DecimalMax(value = "90.0", message = "Vĩ độ phải nằm trong khoảng -90 đến 90")
    private BigDecimal latitude;

    @NotNull(message = "Tọa độ kinh độ (longitude) là bắt buộc")
    @DecimalMin(value = "-180.0", message = "Kinh độ phải nằm trong khoảng -180 đến 180")
    @DecimalMax(value = "180.0", message = "Kinh độ phải nằm trong khoảng -180 đến 180")
    private BigDecimal longitude;

    private LocalTime openingTime;

    private LocalTime closingTime;

    private Long priceFrom;

    private Long priceTo;

    @Size(max = 20, message = "Số điện thoại tối đa 20 ký tự")
    private String phone;

    @Email(message = "Định dạng email quán không hợp lệ")
    @Size(max = 255, message = "Email quán tối đa 255 ký tự")
    private String email;

    @Email(message = "Định dạng email chủ quán không hợp lệ")
    @Size(max = 255, message = "Email chủ quán tối đa 255 ký tự")
    private String ownerEmail;

    @Builder.Default
    private List<String> imageUrls = new ArrayList<>();

    @Builder.Default
    private List<String> tagNames = new ArrayList<>();

    @Builder.Default
    private List<UUID> tagIds = new ArrayList<>();
}
