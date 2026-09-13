package com.moodcafe.store.abstraction.service;

import com.moodcafe.store.dto.request.CreateStoreImageRequest;
import com.moodcafe.store.dto.response.StoreImageResponse;

import java.util.List;
import java.util.UUID;

public interface StoreImageService {

    StoreImageResponse addImage(UUID storeId, CreateStoreImageRequest request);

    void removeImage(UUID storeId, UUID imageId);

    List<StoreImageResponse> getStoreImages(UUID storeId);

    StoreImageResponse setPrimaryImage(UUID storeId, UUID imageId);
}
