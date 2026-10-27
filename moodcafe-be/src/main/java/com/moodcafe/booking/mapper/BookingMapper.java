package com.moodcafe.booking.mapper;

import com.moodcafe.booking.dto.response.BookingDeskSummaryResponse;
import com.moodcafe.booking.dto.response.BookingResponse;
import com.moodcafe.booking.dto.response.CategoryScheduleDTO;
import com.moodcafe.booking.dto.response.SlotAvailabilityResponse;
import com.moodcafe.booking.dto.response.StoreBookingConfigResponse;
import com.moodcafe.booking.dto.response.StoreDailyScheduleResponse;
import com.moodcafe.booking.dto.response.TableCategoryResponse;
import com.moodcafe.booking.dto.response.TimeSlotDTO;
import com.moodcafe.booking.entity.Booking;
import com.moodcafe.booking.entity.StoreBookingConfig;
import com.moodcafe.booking.entity.TableCategory;
import com.moodcafe.store.entity.Store;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Mapper(componentModel = "spring")
public interface BookingMapper {

    @Mapping(source = "store.storeId", target = "storeId")
    StoreBookingConfigResponse toConfigResponse(StoreBookingConfig config);

    @Mapping(source = "store.storeId", target = "storeId")
    TableCategoryResponse toCategoryResponse(TableCategory category);

    @Mapping(source = "store.storeId", target = "storeId")
    @Mapping(source = "store.name", target = "storeName")
    @Mapping(source = "store.address", target = "storeAddress")
    @Mapping(source = "store.phone", target = "storePhone")
    @Mapping(source = "tableCategory.categoryId", target = "categoryId")
    @Mapping(source = "tableCategory.name", target = "categoryName")
    @Mapping(source = "tableCategory.imageUrl", target = "categoryImageUrl")
    @Mapping(source = "tableCategory.maxCapacity", target = "maxCapacity")
    @Mapping(source = "user.userId", target = "userId")
    BookingResponse toBookingResponse(Booking booking);

    @Mapping(source = "booking.store.storeId", target = "storeId")
    @Mapping(source = "booking.store.name", target = "storeName")
    @Mapping(source = "booking.store.address", target = "storeAddress")
    @Mapping(source = "booking.store.phone", target = "storePhone")
    @Mapping(source = "booking.tableCategory.categoryId", target = "categoryId")
    @Mapping(source = "booking.tableCategory.name", target = "categoryName")
    @Mapping(source = "booking.tableCategory.imageUrl", target = "categoryImageUrl")
    @Mapping(source = "booking.tableCategory.maxCapacity", target = "maxCapacity")
    @Mapping(source = "booking.user.userId", target = "userId")
    @Mapping(source = "config.gracePeriodMins", target = "gracePeriodMins")
    @Mapping(source = "config.freeCancelHours", target = "freeCancelHours")
    @Mapping(source = "booking.createdAt", target = "createdAt")
    @Mapping(target = "paymentSessionStatus", expression = "java(booking.getStatus() != null ? booking.getStatus().name() : null)")
    BookingResponse toBookingResponse(Booking booking, StoreBookingConfig config);

    @Mapping(source = "cat.categoryId", target = "categoryId")
    @Mapping(source = "cat.name", target = "categoryName")
    @Mapping(source = "cat.maxCapacity", target = "maxCapacity")
    @Mapping(source = "cat.minSpendAmount", target = "minSpendAmount")
    @Mapping(source = "cat.appQuota", target = "totalQuota")
    @Mapping(source = "cat.imageUrl", target = "imageUrl")
    @Mapping(source = "cat.description", target = "description")
    @Mapping(source = "timeSlots", target = "timeSlots")
    CategoryScheduleDTO toCategoryScheduleDTO(TableCategory cat, List<TimeSlotDTO> timeSlots);

    @Mapping(source = "store.storeId", target = "storeId")
    @Mapping(source = "store.name", target = "storeName")
    @Mapping(source = "date", target = "date")
    @Mapping(source = "isBookingEnabled", target = "isBookingEnabled")
    @Mapping(source = "config.isTimeLimitEnabled", target = "isTimeLimitEnabled")
    @Mapping(source = "config.sittingDurationMins", target = "sittingDurationMins")
    @Mapping(source = "config.cleanupBufferMins", target = "cleanupBufferMins")
    @Mapping(source = "config.minAdvanceMins", target = "minAdvanceMins")
    @Mapping(source = "config.gracePeriodMins", target = "gracePeriodMins")
    @Mapping(source = "config.freeCancelHours", target = "freeCancelHours")
    @Mapping(source = "openingTime", target = "openingTime")
    @Mapping(source = "closingTime", target = "closingTime")
    @Mapping(source = "categories", target = "categories")
    StoreDailyScheduleResponse toStoreDailyScheduleResponse(Store store, LocalDate date, StoreBookingConfig config,
                                                            boolean isBookingEnabled, String openingTime,
                                                            String closingTime, List<CategoryScheduleDTO> categories);

    @Mapping(source = "category.categoryId", target = "categoryId")
    @Mapping(source = "category.name", target = "categoryName")
    @Mapping(source = "category.minSpendAmount", target = "minSpendAmount")
    @Mapping(source = "startTime", target = "startTime")
    @Mapping(source = "endTime", target = "endTime")
    @Mapping(source = "totalQuota", target = "totalQuota")
    @Mapping(source = "occupied", target = "occupiedCount")
    @Mapping(source = "available", target = "availableCount")
    @Mapping(expression = "java(available > 0)", target = "isAvailable")
    SlotAvailabilityResponse toSlotAvailabilityResponse(TableCategory category, Instant startTime, Instant endTime,
                                                        int totalQuota, int occupied, int available);

    BookingDeskSummaryResponse toDeskSummaryResponse(long totalTodayBookings, long pendingArrivalCount,
                                                     long checkedInCount, long completedCount,
                                                     long tableExpiredCount, long claimedLateCount,
                                                     long totalAppQuota);
}
