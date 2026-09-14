package com.moodcafe.store.abstraction.service;

import com.moodcafe.store.dto.request.AddStoreStaffRequest;
import com.moodcafe.store.dto.request.CreateStaffAccountRequest;
import com.moodcafe.store.dto.request.UpdateStaffPasswordRequest;
import com.moodcafe.store.dto.request.UpdateStoreStaffRequest;
import com.moodcafe.store.dto.response.StoreStaffResponse;
import com.moodcafe.store.dto.response.UserStoreResponse;
import com.moodcafe.store.entity.StoreStaff;

import java.util.List;
import java.util.UUID;

public interface StoreStaffService {

    StoreStaffResponse createStaffAccount(UUID storeId, CreateStaffAccountRequest request);

    StoreStaffResponse addStaff(UUID storeId, AddStoreStaffRequest request);

    StoreStaffResponse updateStaffRole(UUID storeId, UUID userId, UpdateStoreStaffRequest request);

    void updateStaffPassword(UUID storeId, UUID userId, UpdateStaffPasswordRequest request);

    void removeStaff(UUID storeId, UUID userId);

    List<StoreStaffResponse> getStoreStaff(UUID storeId);

    List<UserStoreResponse> getUserStores();

    StoreStaff requireStoreAccess(UUID storeId, String... allowedRoles);
}
