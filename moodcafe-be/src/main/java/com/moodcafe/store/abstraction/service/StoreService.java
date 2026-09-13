package com.moodcafe.store.abstraction.service;

import com.moodcafe.store.dto.request.CreateStoreRequest;
import com.moodcafe.store.dto.request.UpdateStoreRequest;
import com.moodcafe.store.dto.request.UpdateStoreStatusRequest;
import com.moodcafe.store.dto.response.StoreResponse;

import java.util.List;
import java.util.UUID;

public interface StoreService {

    StoreResponse createStore(CreateStoreRequest request);

    StoreResponse getStoreById(UUID storeId);

    List<StoreResponse> getAllStores(String status);

    StoreResponse updateStore(UUID storeId, UpdateStoreRequest request);

    StoreResponse changeStoreStatus(UUID storeId, UpdateStoreStatusRequest request);
}
