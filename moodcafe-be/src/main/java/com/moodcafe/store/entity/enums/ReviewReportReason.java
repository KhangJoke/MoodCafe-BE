package com.moodcafe.store.entity.enums;

/**
 * Lý do người dùng báo cáo một bài đánh giá.
 * Khi chọn {@link #OTHER}, người dùng bắt buộc phải nhập mô tả chi tiết (details).
 */
public enum ReviewReportReason {
    SPAM,
    FAKE_REVIEW,
    INAPPROPRIATE_CONTENT,
    ABUSIVE_LANGUAGE,
    HARASSMENT,
    OTHER
}
