package com.moodcafe.tag.service;

import com.moodcafe.shared.error.ErrorCode;
import com.moodcafe.shared.exceptions.AppException;
import com.moodcafe.tag.abstraction.repository.TagCategoryRepository;
import com.moodcafe.tag.abstraction.repository.TagRepository;
import com.moodcafe.tag.abstraction.service.TagService;
import com.moodcafe.tag.dto.request.CreateTagRequest;
import com.moodcafe.tag.dto.request.UpdateTagRequest;
import com.moodcafe.tag.dto.response.TagResponse;
import com.moodcafe.tag.entity.Tag;
import com.moodcafe.tag.entity.TagCategory;
import com.moodcafe.tag.mapper.TagMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TagServiceImpl implements TagService {

    private final TagRepository tagRepository;
    private final TagCategoryRepository tagCategoryRepository;
    private final TagMapper tagMapper;

    @Override
    @Transactional(readOnly = true)
    public List<TagResponse> getAllTags(UUID categoryId, String categoryCode) {
        List<Tag> tags;
        if (categoryId != null) {
            tags = tagRepository.findAllByCategoryTagCategoryIdAndActiveTrue(categoryId);
        } else if (categoryCode != null && !categoryCode.isBlank()) {
            tags = tagRepository.findAllByCategoryCodeAndActiveTrue(categoryCode.trim().toUpperCase());
        } else {
            tags = tagRepository.findAllByActiveTrue();
        }

        return tags.stream()
                .map(tagMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TagResponse getTagById(UUID tagId) {
        Tag tag = tagRepository.findById(tagId)
                .orElseThrow(() -> new AppException(ErrorCode.TAG_NOT_FOUND));

        return tagMapper.toResponse(tag);
    }

    @Override
    @Transactional
    public TagResponse createTag(CreateTagRequest request) {
        String tagName = request.getName().trim();

        if (tagRepository.existsByName(tagName)) {
            throw new AppException(ErrorCode.TAG_ALREADY_EXISTS);
        }

        TagCategory category = tagCategoryRepository.findById(request.getTagCategoryId())
                .orElseThrow(() -> new AppException(ErrorCode.TAG_CATEGORY_NOT_FOUND));

        Tag tag = tagMapper.toEntity(request);
        tag.setName(tagName);
        tag.setCategory(category);
        tag = tagRepository.save(tag);

        return tagMapper.toResponse(tag);
    }

    @Override
    @Transactional
    public TagResponse updateTag(UUID tagId, UpdateTagRequest request) {
        Tag tag = tagRepository.findById(tagId)
                .orElseThrow(() -> new AppException(ErrorCode.TAG_NOT_FOUND));

        if (request.getName() != null && !request.getName().isBlank()) {
            String newName = request.getName().trim();
            if (!tag.getName().equalsIgnoreCase(newName) && tagRepository.existsByName(newName)) {
                throw new AppException(ErrorCode.TAG_ALREADY_EXISTS);
            }
            tag.setName(newName);
        }

        if (request.getTagCategoryId() != null) {
            TagCategory category = tagCategoryRepository.findById(request.getTagCategoryId())
                    .orElseThrow(() -> new AppException(ErrorCode.TAG_CATEGORY_NOT_FOUND));
            tag.setCategory(category);
        }

        tagMapper.updateEntity(request, tag);
        tag = tagRepository.save(tag);

        return tagMapper.toResponse(tag);
    }

    @Override
    @Transactional
    public void deleteTag(UUID tagId) {
        Tag tag = tagRepository.findById(tagId)
                .orElseThrow(() -> new AppException(ErrorCode.TAG_NOT_FOUND));

        tag.setActive(false);
        tagRepository.save(tag);
    }
}
