package com.moodcafe.tag.dto.request;

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
public class RevokeStoreTagAdminRequest {

    @NotBlank(message = "Lý do thu hồi thẻ không được để trống")
    private String reason;
}
