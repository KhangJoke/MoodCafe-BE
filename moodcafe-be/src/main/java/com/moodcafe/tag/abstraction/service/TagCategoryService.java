package com.moodcafe.tag.abstraction.service;

import com.moodcafe.tag.dto.request.CreateTagCategoryRequest;
import com.moodcafe.tag.dto.request.UpdateTagCategoryRequest;
import com.moodcafe.tag.dto.response.TagCategoryResponse;

import java.util.List;
import java.util.UUID;

public interface TagCategoryService {

    List<TagCategoryResponse> getAllTagCategories();

    List<TagCategoryResponse> getActiveTagCategories();

    TagCategoryResponse getTagCategoryById(UUID tagCategoryId);

    TagCategoryResponse createTagCategory(CreateTagCategoryRequest request);

    TagCategoryResponse updateTagCategory(UUID tagCategoryId, UpdateTagCategoryRequest request);

    void deleteTagCategory(UUID tagCategoryId);
}
