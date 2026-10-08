package com.moodcafe.store.dto.request;

import com.moodcafe.store.entity.enums.ReviewReportReason;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
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
public class ReportReviewRequest {

    @Schema(description = "Lý do báo cáo", example = "FAKE_REVIEW")
    @NotNull(message = "Lý do báo cáo không được để trống")
    private ReviewReportReason reason;

    @Schema(description = "Mô tả chi tiết (bắt buộc khi reason = OTHER)")
    @Size(max = 1000, message = "Chi tiết báo cáo tối đa 1000 ký tự")
    private String details;

    @Schema(hidden = true)
    @AssertTrue(message = "Vui lòng mô tả chi tiết khi chọn lý do OTHER")
    public boolean isDetailsProvidedWhenOther() {
        return reason != ReviewReportReason.OTHER || (details != null && !details.isBlank());
    }
}
