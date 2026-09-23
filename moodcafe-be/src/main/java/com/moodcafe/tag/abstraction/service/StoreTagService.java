package com.moodcafe.tag.abstraction.service;

import com.moodcafe.tag.dto.request.ReviewStoreTagRequest;
import com.moodcafe.tag.dto.request.SubmitStoreTagRequest;
import com.moodcafe.tag.dto.request.UpdateStoreHighlightTagsRequest;
import com.moodcafe.tag.dto.response.StoreAttributesResponse;
import com.moodcafe.tag.dto.response.StoreTagResponse;

import java.util.List;
import java.util.UUID;

public interface StoreTagService {

    StoreAttributesResponse getStoreAttributes(UUID storeId);

    List<StoreTagResponse> getStoreTagsManagement(UUID storeId);

    StoreTagResponse requestStoreTag(UUID storeId, SubmitStoreTagRequest request);

    StoreTagResponse resubmitStoreTag(UUID storeId, UUID storeTagId, SubmitStoreTagRequest request);

    List<StoreTagResponse> getPendingStoreTagRequests();

    StoreTagResponse reviewStoreTagRequest(UUID storeTagId, ReviewStoreTagRequest request);

    List<StoreTagResponse> updateStoreHighlightTags(UUID storeId, UpdateStoreHighlightTagsRequest request);
}
