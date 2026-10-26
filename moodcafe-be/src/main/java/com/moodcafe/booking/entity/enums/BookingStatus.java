package com.moodcafe.booking.entity.enums;

public enum BookingStatus {
    /**
     * Khách vừa bấm đặt bàn, đang chờ thanh toán qua PayOS (giữ slot tạm thời 10 phút).
     */
    PENDING_PAYMENT,

    /**
     * Giao dịch thanh toán thất bại hoặc quá hạn 10 phút. Tự động giải phóng slot bàn.
     */
    PAYMENT_FAILED,

    /**
     * Khách đã thanh toán 100% hạn mức đồ uống (F&B Credit). Quán cam kết giữ bàn.
     */
    PAID_CONFIRMED,

    /**
     * Khách chủ động hủy đơn hợp lệ trước mốc free_cancel_hours. Giải phóng slot và ghi nhận hoàn tiền.
     */
    CANCELLED_REFUNDED,

    /**
     * Khách đến đúng hạn trong grace_period_mins, thu ngân đã xác nhận nhận bàn và cấn trừ F&B credit.
     */
    CHECKED_IN,

    /**
     * Khách đã báo trả bàn trên Mobile App (Slide-to-Checkout), đang chờ nhân viên kiểm tra & dọn bàn. Vẫn chiếm slot bàn.
     */
    CHECK_OUT_PENDING,

    /**
     * Khóa giữ chỗ tạm thời cho khách vãng lai (Walk-in Quick Hold) tại quầy để bảo vệ quỹ bàn trên App.
     */
    WALK_IN_HOLD,

    /**
     * Khách rời đi, thu ngân bấm trả bàn hoặc hệ thống tự động hoàn tất khi hết giờ ngồi quy định.
     */
    COMPLETED,

    /**
     * Quá thời gian grace_period_mins khách chưa check-in. Tự động thu hồi slot bàn, bảo lưu tiền nước trong ngày.
     */
    TABLE_EXPIRED,

    /**
     * Khách của đơn TABLE_EXPIRED đã đến quầy trước giờ đóng cửa và sử dụng hạn mức đổi nước thành công.
     */
    CLAIMED_LATE,

    /**
     * Kết thúc ngày tại store_closing_time mà khách của đơn TABLE_EXPIRED vẫn không đến lấy nước. Đóng đơn vĩnh viễn.
     */
    EXPIRED_NO_SHOW;

    /**
     * Kiểm tra trạng thái đơn có đang chiếm giữ slot bàn trong app_quota hay không.
     */
    public boolean occupiesTableSlot() {
        return this == PENDING_PAYMENT || this == PAID_CONFIRMED || this == CHECKED_IN
                || this == CHECK_OUT_PENDING || this == WALK_IN_HOLD;
    }
}
