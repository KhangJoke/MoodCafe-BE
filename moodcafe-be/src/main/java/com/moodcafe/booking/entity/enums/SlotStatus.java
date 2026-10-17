package com.moodcafe.booking.entity.enums;

/**
 * Trạng thái của từng mốc thời gian (Slot) trên Lịch Trạng Thái Bàn.
 */
public enum SlotStatus {
    /**
     * Còn từ 2 bàn trống trở lên và thời gian ngồi trọn vẹn.
     */
    AVAILABLE,

    /**
     * Chỉ còn đúng 1 bàn trống cuối cùng (thời gian ngồi trọn vẹn).
     */
    ALMOST_FULL,

    /**
     * Còn bàn trống nhưng thời gian ngồi khả dụng bị giới hạn do ca sau đã có người đặt trước.
     * Cần hiển thị cảnh báo thời gian rảnh tối đa.
     */
    LIMITED_TIME,

    /**
     * Kín chỗ (hết bàn hoặc khoảng trống rảnh quá ngắn không đủ thời lượng tối thiểu).
     */
    FULL,

    /**
     * Mốc thời gian đã qua so với giờ hiện tại hoặc vi phạm thời gian đặt trước tối thiểu (minAdvanceMins).
     */
    PAST
}
