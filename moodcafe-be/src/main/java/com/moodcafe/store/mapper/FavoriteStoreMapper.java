package com.moodcafe.store.mapper;

import com.moodcafe.store.dto.response.FavoriteStoreResponse;
import com.moodcafe.store.entity.FavoriteStore;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface FavoriteStoreMapper {

    @Mapping(source = "user.userId", target = "userId")
    @Mapping(source = "store.storeId", target = "storeId")
    @Mapping(source = "store.name", target = "storeName")
    @Mapping(source = "store.address", target = "storeAddress")
    @Mapping(source = "store.district", target = "district")
    @Mapping(source = "store.priceFrom", target = "priceFrom")
    @Mapping(source = "store.priceTo", target = "priceTo")
    @Mapping(source = "store.openingTime", target = "openingTime")
    @Mapping(source = "store.closingTime", target = "closingTime")
    @Mapping(target = "primaryImageUrl", ignore = true)
    @Mapping(target = "isOpenNow", ignore = true)
    @Mapping(target = "overallRating", ignore = true)
    @Mapping(target = "reviewCount", ignore = true)
    @Mapping(target = "highlightTags", ignore = true)
    FavoriteStoreResponse toResponse(FavoriteStore favoriteStore);
}

