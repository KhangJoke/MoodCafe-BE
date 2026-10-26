package com.moodcafe.booking.dto.request;

import jakarta.validation.constraints.NotBlank;
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
public class DeskCheckInRequest {

    @NotBlank(message = "Mã đặt chỗ hoặc token QR không được để trống")
    private String codeOrToken;

    private String physicalTableNumber;
}
