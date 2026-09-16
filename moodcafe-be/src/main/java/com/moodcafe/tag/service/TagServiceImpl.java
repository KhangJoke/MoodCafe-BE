package com.moodcafe.tag.service;

import com.moodcafe.shared.error.ErrorCode;
import com.moodcafe.shared.exceptions.AppException;
import com.moodcafe.tag.abstraction.repository.StoreTagRepository;
import com.moodcafe.tag.abstraction.repository.TagCategoryRepository;
import com.moodcafe.tag.abstraction.repository.TagRepository;
import com.moodcafe.tag.abstraction.service.TagService;
import com.moodcafe.tag.dto.request.CreateTagRequest;
import com.moodcafe.tag.dto.request.UpdateTagRequest;
import com.moodcafe.tag.dto.response.ExperienceMatcherResponse;
import com.moodcafe.tag.dto.response.ExperienceVibeResponse;
import com.moodcafe.tag.dto.response.TagResponse;
import com.moodcafe.tag.entity.Tag;
import com.moodcafe.tag.entity.TagCategory;
import com.moodcafe.tag.entity.enums.StoreTagStatus;
import com.moodcafe.tag.mapper.TagMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TagServiceImpl implements TagService {

    private final TagRepository tagRepository;
    private final TagCategoryRepository tagCategoryRepository;
    private final StoreTagRepository storeTagRepository;
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

        if ("VIBE".equalsIgnoreCase(category.getCode())) {
            if (request.getImageUrl() == null || request.getImageUrl().trim().isEmpty()) {
                throw new AppException(ErrorCode.VIBE_IMAGE_REQUIRED);
            }
        }

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

        if (tag.getCategory() != null && "VIBE".equalsIgnoreCase(tag.getCategory().getCode())) {
            if (request.getImageUrl() != null && request.getImageUrl().trim().isEmpty()) {
                throw new AppException(ErrorCode.VIBE_IMAGE_REQUIRED);
            }
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

    @Override
    @Transactional(readOnly = true)
    public List<ExperienceMatcherResponse> getExperienceMatcherData() {
        List<Tag> purposeTags = tagRepository.findAllByCategoryCodeAndActiveTrue("PURPOSE");
        List<Tag> vibeTags = tagRepository.findAllByCategoryCodeAndActiveTrue("VIBE").stream()
                .filter(v -> v.getImageUrl() != null && !v.getImageUrl().isBlank())
                .toList();

        List<ExperienceMatcherResponse> result = new ArrayList<>();

        for (Tag purpose : purposeTags) {
            long totalStoreCount = storeTagRepository.countDistinctStoresByTagIdAndStatus(
                    purpose.getTagId(), StoreTagStatus.APPROVED
            );

            List<ExperienceVibeResponse> vibeResponses = new ArrayList<>();
            for (Tag vibe : vibeTags) {
                long count = storeTagRepository.countStoresWithBothTags(
                        purpose.getTagId(), vibe.getTagId(), StoreTagStatus.APPROVED
                );

                vibeResponses.add(ExperienceVibeResponse.builder()
                        .vibeId(vibe.getTagId())
                        .vibeName(vibe.getName())
                        .shortName(resolveShortName(vibe.getName()))
                        .description(vibe.getDescription())
                        .imageUrl(vibe.getImageUrl())
                        .storeCount(count)
                        .build());
            }

            // Sắp xếp các Vibe con: Vibe có nhiều quán nhất lên trước; nếu hòa (ví dụ = 0), xếp theo tên
            vibeResponses.sort(Comparator
                    .comparingLong(ExperienceVibeResponse::getStoreCount).reversed()
                    .thenComparing(ExperienceVibeResponse::getVibeName));

            result.add(ExperienceMatcherResponse.builder()
                    .purposeId(purpose.getTagId())
                    .purposeName(purpose.getName())
                    .shortName(resolveShortName(purpose.getName()))
                    .subtitle(purpose.getDescription())
                    .totalStoreCount(totalStoreCount)
                    .vibes(vibeResponses)
                    .build());
        }

        // Sắp xếp các Purpose cha: Mục đích có nhiều quán nhất lên trước; nếu hòa (ví dụ = 0), xếp theo tên
        result.sort(Comparator
                .comparingLong(ExperienceMatcherResponse::getTotalStoreCount).reversed()
                .thenComparing(ExperienceMatcherResponse::getPurposeName));

        // Giới hạn đúng 6 mục đích nổi bật nhất để hiển thị giao diện chuẩn 3 cột x 2 hàng
        return result.stream().limit(6).toList();
    }

    private String resolveShortName(String fullName) {
        if (fullName == null) return "";
        if (fullName.contains("(")) {
            return fullName.substring(0, fullName.indexOf('(')).trim();
        }
        return fullName.trim();
    }
}
