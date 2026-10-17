package com.moodcafe.booking.dto.request;

import jakarta.validation.constraints.NotBlank;
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
public class CancelBookingRequest {

    @NotBlank(message = "Lý do hủy đơn không được để trống")
    @Size(max = 500, message = "Lý do hủy không quá 500 ký tự")
    private String reason;
}
