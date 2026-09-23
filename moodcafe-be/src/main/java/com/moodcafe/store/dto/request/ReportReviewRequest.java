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
public class ReportReviewRequest {

    @NotBlank(message = "Lý do báo cáo không được để trống")
    @Size(max = 100, message = "Lý do báo cáo tối đa 100 ký tự")
    private String reason;

    @Size(max = 1000, message = "Chi tiết báo cáo tối đa 1000 ký tự")
    private String details;
}
