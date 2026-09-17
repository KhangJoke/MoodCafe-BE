package com.moodcafe.tag.abstraction.service;

import com.moodcafe.tag.dto.request.CreateTagRequest;
import com.moodcafe.tag.dto.request.UpdateTagRequest;
import com.moodcafe.tag.dto.response.CityTrendingResponse;
import com.moodcafe.tag.dto.response.ExperienceMatcherResponse;
import com.moodcafe.tag.dto.response.TagResponse;

import java.util.List;
import java.util.UUID;

public interface TagService {

    List<TagResponse> getAllTags(UUID categoryId, String categoryCode);

    TagResponse getTagById(UUID tagId);

    TagResponse createTag(CreateTagRequest request);

    TagResponse updateTag(UUID tagId, UpdateTagRequest request);

    void deleteTag(UUID tagId);

    List<ExperienceMatcherResponse> getExperienceMatcherData();

    List<CityTrendingResponse> getCityTrendingData();
}
