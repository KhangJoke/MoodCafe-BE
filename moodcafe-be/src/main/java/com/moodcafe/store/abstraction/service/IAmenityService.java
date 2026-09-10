package com.moodcafe.store.abstraction.service;

import com.moodcafe.store.dto.request.CreateAmenityRequest;
import com.moodcafe.store.dto.request.UpdateAmenityRequest;
import com.moodcafe.store.dto.response.AmenityResponse;

import java.util.List;
import java.util.UUID;

public interface IAmenityService {

    AmenityResponse createAmenity(CreateAmenityRequest request);

    AmenityResponse updateAmenity(UUID amenityId, UpdateAmenityRequest request);

    List<AmenityResponse> getAllAmenities();

    void addAmenityToStore(UUID storeId, UUID amenityId);

    void removeAmenityFromStore(UUID storeId, UUID amenityId);

    List<AmenityResponse> getStoreAmenities(UUID storeId);
}
