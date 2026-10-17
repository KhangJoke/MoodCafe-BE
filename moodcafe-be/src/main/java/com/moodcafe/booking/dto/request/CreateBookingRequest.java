package com.moodcafe.booking.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateBookingRequest {

    @NotNull(message = "storeId is required")
    private UUID storeId;

    @NotNull(message = "categoryId is required")
    private UUID categoryId;

    @NotNull(message = "startTime is required")
    private Instant startTime;

    @NotBlank(message = "Tên khách hàng không được để trống")
    @Size(max = 255, message = "Tên khách hàng không quá 255 ký tự")
    private String customerName;

    @NotBlank(message = "Số điện thoại không được để trống")
    @Size(max = 50, message = "Số điện thoại không quá 50 ký tự")
    private String customerPhone;

    @Email(message = "Email không đúng định dạng")
    @Size(max = 255, message = "Email không quá 255 ký tự")
    private String customerEmail;

    @Size(max = 1000, message = "Ghi chú không quá 1000 ký tự")
    private String note;
}
