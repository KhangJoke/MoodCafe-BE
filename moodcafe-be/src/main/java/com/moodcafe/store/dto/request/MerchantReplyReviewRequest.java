package com.moodcafe.store.dto.request;

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
public class MerchantReplyReviewRequest {

    @NotBlank(message = "Nội dung phản hồi không được để trống")
    @Size(max = 500, message = "Phản hồi tối đa 500 ký tự")
    private String reply;
}
