package com.moodcafe.store.abstraction.service;

import com.moodcafe.shared.response.PageResponse;
import com.moodcafe.store.dto.request.CreateStoreRequest;
import com.moodcafe.store.dto.request.StoreSearchRequest;
import com.moodcafe.store.dto.request.UpdateStoreRequest;
import com.moodcafe.store.dto.request.UpdateStoreStatusRequest;
import com.moodcafe.store.dto.response.FeaturedMoodStoreResponse;
import com.moodcafe.store.dto.response.StoreResponse;
import com.moodcafe.store.dto.response.StoreSearchItemResponse;

import java.util.List;
import java.util.UUID;

public interface StoreService {

    StoreResponse createStore(CreateStoreRequest request);

    StoreResponse getStoreById(UUID storeId);

    List<StoreResponse> getAllStores(String status);

    StoreResponse updateStore(UUID storeId, UpdateStoreRequest request);

    StoreResponse changeStoreStatus(UUID storeId, UpdateStoreStatusRequest request);

    PageResponse<StoreSearchItemResponse> searchStores(StoreSearchRequest request);

    List<FeaturedMoodStoreResponse> getFeaturedMoodStores();

    List<String> getActiveDistricts();
}
