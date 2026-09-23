package com.moodcafe.store.mapper;

import com.moodcafe.store.dto.response.StoreStaffResponse;
import com.moodcafe.store.dto.response.UserStoreResponse;
import com.moodcafe.store.entity.StoreStaff;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface StoreStaffMapper {

    @Mapping(source = "store.storeId", target = "storeId")
    @Mapping(source = "user.userId", target = "userId")
    @Mapping(source = "user.fullName", target = "fullName")
    @Mapping(source = "user.email", target = "email")
    @Mapping(source = "storeRole.name", target = "storeRole")
    StoreStaffResponse toResponse(StoreStaff storeStaff);

    @Mapping(source = "store.storeId", target = "storeId")
    @Mapping(source = "store.name", target = "storeName")
    @Mapping(source = "store.address", target = "storeAddress")
    @Mapping(source = "storeRole.name", target = "storeRole")
    @Mapping(source = "store.status", target = "storeStatus")
    @Mapping(source = "store.rejectReason", target = "rejectReason")
    @Mapping(source = "store.allowResubmit", target = "allowResubmit")
    UserStoreResponse toUserStoreResponse(StoreStaff storeStaff);
}
