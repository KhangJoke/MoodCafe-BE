package com.moodcafe.store.mapper;

import com.moodcafe.store.dto.response.StoreScheduleResponse;
import com.moodcafe.store.entity.StoreSchedule;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.DayOfWeek;

@Mapper(componentModel = "spring")
public interface StoreScheduleMapper {

    @Mapping(target = "isOpen", source = "open")
    @Mapping(target = "dayNameVi", expression = "java(getDayNameVi(schedule.getDayOfWeek()))")
    StoreScheduleResponse toResponse(StoreSchedule schedule);

    default String getDayNameVi(DayOfWeek dayOfWeek) {
        if (dayOfWeek == null) return "";
        return switch (dayOfWeek) {
            case MONDAY -> "Thứ Hai";
            case TUESDAY -> "Thứ Ba";
            case WEDNESDAY -> "Thứ Tư";
            case THURSDAY -> "Thứ Năm";
            case FRIDAY -> "Thứ Sáu";
            case SATURDAY -> "Thứ Bảy";
            case SUNDAY -> "Chủ Nhật";
        };
    }
}
