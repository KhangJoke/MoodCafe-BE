package com.moodcafe.store.abstraction.service;

import com.moodcafe.store.dto.request.AddStoreStaffRequest;
import com.moodcafe.store.dto.request.UpdateStoreStaffRequest;
import com.moodcafe.store.dto.response.StoreStaffResponse;
import com.moodcafe.store.dto.response.UserStoreResponse;

import java.util.List;
import java.util.UUID;

public interface IStoreStaffService {

    StoreStaffResponse addStaff(UUID storeId, AddStoreStaffRequest request);

    StoreStaffResponse updateStaffRole(UUID storeId, UUID userId, UpdateStoreStaffRequest request);

    void removeStaff(UUID storeId, UUID userId);

    List<StoreStaffResponse> getStoreStaff(UUID storeId);

    List<UserStoreResponse> getUserStores();
}
