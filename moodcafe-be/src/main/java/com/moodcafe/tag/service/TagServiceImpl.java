package com.moodcafe.tag.service;

import com.moodcafe.shared.error.ErrorCode;
import com.moodcafe.shared.exceptions.AppException;
import com.moodcafe.tag.abstraction.repository.MasterTagRepository;
import com.moodcafe.tag.abstraction.service.TagService;
import com.moodcafe.tag.dto.request.CreateTagRequest;
import com.moodcafe.tag.dto.request.UpdateTagRequest;
import com.moodcafe.tag.dto.response.TagResponse;
import com.moodcafe.tag.entity.MasterTag;
import com.moodcafe.tag.mapper.TagMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TagServiceImpl implements TagService {

    private final MasterTagRepository masterTagRepository;
    private final TagMapper tagMapper;

    @Override
    @Transactional(readOnly = true)
    public List<TagResponse> getAllTags(String category) {
        List<MasterTag> tags;
        if (category != null && !category.isBlank()) {
            tags = masterTagRepository.findAllByCategoryAndActiveTrue(category.trim().toUpperCase());
        } else {
            tags = masterTagRepository.findAllByActiveTrue();
        }

        return tags.stream()
                .map(tagMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TagResponse getTagById(UUID tagId) {
        MasterTag tag = masterTagRepository.findById(tagId)
                .orElseThrow(() -> new AppException(ErrorCode.TAG_NOT_FOUND));

        return tagMapper.toResponse(tag);
    }

    @Override
    @Transactional
    public TagResponse createTag(CreateTagRequest request) {
        String tagName = request.getName().trim();

        if (masterTagRepository.existsByName(tagName)) {
            throw new AppException(ErrorCode.TAG_ALREADY_EXISTS);
        }

        MasterTag tag = tagMapper.toEntity(request);
        tag.setName(tagName);
        tag.setCategory(request.getCategory().trim().toUpperCase());
        tag = masterTagRepository.save(tag);

        return tagMapper.toResponse(tag);
    }

    @Override
    @Transactional
    public TagResponse updateTag(UUID tagId, UpdateTagRequest request) {
        MasterTag tag = masterTagRepository.findById(tagId)
                .orElseThrow(() -> new AppException(ErrorCode.TAG_NOT_FOUND));

        String newName = request.getName().trim();
        if (!tag.getName().equalsIgnoreCase(newName) && masterTagRepository.existsByName(newName)) {
            throw new AppException(ErrorCode.TAG_ALREADY_EXISTS);
        }

        tagMapper.updateEntity(request, tag);
        tag.setName(newName);
        if (request.getCategory() != null && !request.getCategory().isBlank()) {
            tag.setCategory(request.getCategory().trim().toUpperCase());
        }
        tag = masterTagRepository.save(tag);

        return tagMapper.toResponse(tag);
    }

    @Override
    @Transactional
    public void deleteTag(UUID tagId) {
        MasterTag tag = masterTagRepository.findById(tagId)
                .orElseThrow(() -> new AppException(ErrorCode.TAG_NOT_FOUND));

        tag.setActive(false);
        masterTagRepository.save(tag);
    }
}
