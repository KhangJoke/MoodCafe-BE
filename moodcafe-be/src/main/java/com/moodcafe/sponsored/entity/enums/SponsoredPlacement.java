package com.moodcafe.sponsored.entity.enums;

import lombok.Getter;

import java.math.BigDecimal;

@Getter
public enum SponsoredPlacement {

    TOP_BANNER_VIP(
            "Top Banner VIP (Featured Cafés)",
            "Hiển thị tại Carousel Banner VIP đầu trang chủ và đầu danh sách kết quả tìm kiếm",
            new BigDecimal("150000"),
            new BigDecimal("480000") // Tiết kiệm 20%
    ),
    NEW_OPENING(
            "Quán mới nổi bật (New Opening)",
            "Xuất hiện ưu tiên trong danh mục Quán mới khai trương và địa điểm đáng trải nghiệm",
            new BigDecimal("100000"),
            new BigDecimal("320000") // Tiết kiệm 20%
    ),
    WEEKEND_PICKS(
            "Cuối tuần Hot (Weekend Picks)",
            "Gợi ý hàng đầu trong danh mục Điểm đến Cuối tuần sôi động thu hút nhóm bạn & gia đình",
            new BigDecimal("120000"),
            new BigDecimal("384000") // Tiết kiệm 20%
    ),
    TRENDING_NEAR_YOU(
            "Xu hướng gần bạn (Trending Near You)",
            "Ưu tiên vị trí trong danh sách Xu hướng cho khách hàng truy cập tại cùng khu vực địa lý",
            new BigDecimal("90000"),
            new BigDecimal("288000") // Tiết kiệm 20%
    );

    private final String displayName;
    private final String description;
    private final BigDecimal weeklyPrice;
    private final BigDecimal monthlyPrice;

    SponsoredPlacement(String displayName, String description, BigDecimal weeklyPrice, BigDecimal monthlyPrice) {
        this.displayName = displayName;
        this.description = description;
        this.weeklyPrice = weeklyPrice;
        this.monthlyPrice = monthlyPrice;
    }

    public BigDecimal getPriceByDuration(SponsoredDurationType durationType) {
        return SponsoredDurationType.ONE_MONTH.equals(durationType) ? monthlyPrice : weeklyPrice;
    }
}
