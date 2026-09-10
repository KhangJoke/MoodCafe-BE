package com.moodcafe.store.mapper;

import com.moodcafe.store.dto.request.CreateStoreRequest;
import com.moodcafe.store.dto.request.UpdateStoreRequest;
import com.moodcafe.store.dto.response.StoreResponse;
import com.moodcafe.store.entity.Store;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface StoreMapper {

    @Mapping(target = "storeId", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Store toEntity(CreateStoreRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "storeId", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(UpdateStoreRequest request, @MappingTarget Store store);

    @Mapping(target = "images", ignore = true)
    @Mapping(target = "amenities", ignore = true)
    StoreResponse toResponse(Store store);
}
