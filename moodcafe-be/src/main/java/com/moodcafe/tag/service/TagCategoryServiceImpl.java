package com.moodcafe.tag.service;

import com.moodcafe.shared.error.ErrorCode;
import com.moodcafe.shared.exceptions.AppException;
import com.moodcafe.tag.abstraction.repository.TagCategoryRepository;
import com.moodcafe.tag.abstraction.service.TagCategoryService;
import com.moodcafe.tag.dto.request.CreateTagCategoryRequest;
import com.moodcafe.tag.dto.request.UpdateTagCategoryRequest;
import com.moodcafe.tag.dto.response.TagCategoryResponse;
import com.moodcafe.tag.entity.TagCategory;
import com.moodcafe.tag.mapper.TagCategoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TagCategoryServiceImpl implements TagCategoryService {

    private final TagCategoryRepository tagCategoryRepository;
    private final TagCategoryMapper tagCategoryMapper;

    @Override
    @Transactional(readOnly = true)
    public List<TagCategoryResponse> getAllTagCategories() {
        return tagCategoryRepository.findAllByOrderByDisplayOrderAsc().stream()
                .map(tagCategoryMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TagCategoryResponse> getActiveTagCategories() {
        return tagCategoryRepository.findAllByActiveTrueOrderByDisplayOrderAsc().stream()
                .map(tagCategoryMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TagCategoryResponse getTagCategoryById(UUID tagCategoryId) {
        TagCategory tagCategory = tagCategoryRepository.findById(tagCategoryId)
                .orElseThrow(() -> new AppException(ErrorCode.TAG_CATEGORY_NOT_FOUND));
        return tagCategoryMapper.toResponse(tagCategory);
    }

    @Override
    @Transactional
    public TagCategoryResponse createTagCategory(CreateTagCategoryRequest request) {
        if (tagCategoryRepository.findByCode(request.getCode().trim().toUpperCase()).isPresent()) {
            throw new AppException(ErrorCode.TAG_CATEGORY_ALREADY_EXISTS, "Tag category with code " + request.getCode() + " already exists");
        }

        TagCategory tagCategory = tagCategoryMapper.toEntity(request);
        tagCategory.setCode(request.getCode().trim().toUpperCase());
        tagCategory = tagCategoryRepository.save(tagCategory);
        return tagCategoryMapper.toResponse(tagCategory);
    }

    @Override
    @Transactional
    public TagCategoryResponse updateTagCategory(UUID tagCategoryId, UpdateTagCategoryRequest request) {
        TagCategory tagCategory = tagCategoryRepository.findById(tagCategoryId)
                .orElseThrow(() -> new AppException(ErrorCode.TAG_CATEGORY_NOT_FOUND));

        if (request.getCode() != null && !request.getCode().trim().equalsIgnoreCase(tagCategory.getCode())) {
            if (tagCategoryRepository.findByCode(request.getCode().trim().toUpperCase()).isPresent()) {
                throw new AppException(ErrorCode.TAG_CATEGORY_ALREADY_EXISTS, "Tag category with code " + request.getCode() + " already exists");
            }
            tagCategory.setCode(request.getCode().trim().toUpperCase());
        }

        tagCategoryMapper.updateEntity(request, tagCategory);
        tagCategory = tagCategoryRepository.save(tagCategory);
        return tagCategoryMapper.toResponse(tagCategory);
    }

    @Override
    @Transactional
    public void deleteTagCategory(UUID tagCategoryId) {
        TagCategory tagCategory = tagCategoryRepository.findById(tagCategoryId)
                .orElseThrow(() -> new AppException(ErrorCode.TAG_CATEGORY_NOT_FOUND));
        tagCategory.setActive(false);
        tagCategoryRepository.save(tagCategory);
    }
}
