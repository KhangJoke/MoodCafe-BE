package com.moodcafe.store.mapper;

import com.moodcafe.store.dto.response.StoreImageResponse;
import com.moodcafe.store.entity.StoreImage;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface StoreImageMapper {

    @Mapping(source = "store.storeId", target = "storeId")
    @Mapping(source = "primary", target = "isPrimary")
    StoreImageResponse toResponse(StoreImage storeImage);
}
