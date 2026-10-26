package com.moodcafe.booking.mapper;

import com.moodcafe.booking.dto.response.BookingResponse;
import com.moodcafe.booking.dto.response.StoreBookingConfigResponse;
import com.moodcafe.booking.dto.response.TableCategoryResponse;
import com.moodcafe.booking.entity.Booking;
import com.moodcafe.booking.entity.StoreBookingConfig;
import com.moodcafe.booking.entity.TableCategory;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

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
}

