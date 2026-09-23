package com.moodcafe.store.abstraction.service;

import com.moodcafe.shared.response.PageResponse;
import com.moodcafe.store.dto.request.CreateStoreRequest;
import com.moodcafe.store.dto.request.StoreRegisterRequest;
import com.moodcafe.store.dto.request.StoreResubmitRequest;
import com.moodcafe.store.dto.request.StoreSearchRequest;
import com.moodcafe.store.dto.request.UpdateStoreRequest;
import com.moodcafe.store.dto.request.UpdateStoreStatusRequest;
import com.moodcafe.store.dto.response.FeaturedMoodStoreResponse;
import com.moodcafe.store.dto.response.MerchantDashboardResponse;
import com.moodcafe.store.dto.response.StoreRegistrationStatusResponse;
import com.moodcafe.store.dto.response.StoreResponse;
import com.moodcafe.store.dto.response.StoreSearchItemResponse;

import java.util.List;
import java.util.UUID;

public interface StoreService {

    StoreResponse createStore(CreateStoreRequest request);

    StoreRegistrationStatusResponse registerStore(StoreRegisterRequest request);

    StoreRegistrationStatusResponse resubmitStore(UUID storeId, StoreResubmitRequest request);

    StoreRegistrationStatusResponse getStoreRegistrationStatus(UUID storeId);

    StoreRegistrationStatusResponse getMyRegistration();

    MerchantDashboardResponse getMerchantDashboard(UUID storeId);

    StoreResponse getStoreById(UUID storeId);

    List<StoreResponse> getAllStores(String status);

    StoreResponse updateStore(UUID storeId, UpdateStoreRequest request);

    StoreResponse changeStoreStatus(UUID storeId, UpdateStoreStatusRequest request);

    PageResponse<StoreSearchItemResponse> searchStores(StoreSearchRequest request);

    List<FeaturedMoodStoreResponse> getFeaturedMoodStores();

    List<String> getActiveDistricts();
}
