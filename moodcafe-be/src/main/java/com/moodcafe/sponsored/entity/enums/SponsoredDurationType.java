package com.moodcafe.sponsored.entity.enums;

import lombok.Getter;

@Getter
public enum SponsoredDurationType {

    ONE_WEEK("1 Tuần", 7),
    ONE_MONTH("1 Tháng (Tiết kiệm 20%)", 30);

    private final String label;
    private final int days;

    SponsoredDurationType(String label, int days) {
        this.label = label;
        this.days = days;
    }
}
