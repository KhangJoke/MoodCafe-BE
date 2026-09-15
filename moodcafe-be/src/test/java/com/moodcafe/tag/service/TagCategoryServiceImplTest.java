package com.moodcafe.tag.service;

import com.moodcafe.shared.error.ErrorCode;
import com.moodcafe.shared.exceptions.AppException;
import com.moodcafe.tag.abstraction.repository.TagCategoryRepository;
import com.moodcafe.tag.dto.request.CreateTagCategoryRequest;
import com.moodcafe.tag.dto.response.TagCategoryResponse;
import com.moodcafe.tag.entity.ApprovalMode;
import com.moodcafe.tag.entity.ControlType;
import com.moodcafe.tag.entity.TagCategory;
import com.moodcafe.tag.mapper.TagCategoryMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TagCategoryServiceImplTest {

    @Mock
    private TagCategoryRepository tagCategoryRepository;

    @Mock
    private TagCategoryMapper tagCategoryMapper;

    @InjectMocks
    private TagCategoryServiceImpl tagCategoryService;

    private UUID tagCategoryId;
    private TagCategory tagCategory;
    private TagCategoryResponse tagCategoryResponse;

    @BeforeEach
    void setUp() {
        tagCategoryId = UUID.randomUUID();

        tagCategory = TagCategory.builder()
                .tagCategoryId(tagCategoryId)
                .name("Vibe & Phong cách")
                .code("VIBE")
                .approvalMode(ApprovalMode.OWNER_REQUEST)
                .controlType(ControlType.TAG_LIST)
                .displayOrder(1)
                .active(true)
                .createdAt(Instant.now())
                .build();

        tagCategoryResponse = TagCategoryResponse.builder()
                .tagCategoryId(tagCategoryId)
                .name("Vibe & Phong cách")
                .code("VIBE")
                .approvalMode(ApprovalMode.OWNER_REQUEST)
                .controlType(ControlType.TAG_LIST)
                .displayOrder(1)
                .active(true)
                .createdAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("getAllTagCategories - returns all tag categories in order")
    void getAllTagCategories_success() {
        when(tagCategoryRepository.findAllByOrderByDisplayOrderAsc()).thenReturn(List.of(tagCategory));
        when(tagCategoryMapper.toResponse(tagCategory)).thenReturn(tagCategoryResponse);

        List<TagCategoryResponse> result = tagCategoryService.getAllTagCategories();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCode()).isEqualTo("VIBE");
    }

    @Test
    @DisplayName("getActiveTagCategories - returns only active tag categories in order")
    void getActiveTagCategories_success() {
        when(tagCategoryRepository.findAllByActiveTrueOrderByDisplayOrderAsc()).thenReturn(List.of(tagCategory));
        when(tagCategoryMapper.toResponse(tagCategory)).thenReturn(tagCategoryResponse);

        List<TagCategoryResponse> result = tagCategoryService.getActiveTagCategories();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCode()).isEqualTo("VIBE");
    }

    @Test
    @DisplayName("getTagCategoryById - found")
    void getTagCategoryById_found() {
        when(tagCategoryRepository.findById(tagCategoryId)).thenReturn(Optional.of(tagCategory));
        when(tagCategoryMapper.toResponse(tagCategory)).thenReturn(tagCategoryResponse);

        TagCategoryResponse result = tagCategoryService.getTagCategoryById(tagCategoryId);

        assertThat(result).isNotNull();
        assertThat(result.getTagCategoryId()).isEqualTo(tagCategoryId);
    }

    @Test
    @DisplayName("getTagCategoryById - not found throws AppException")
    void getTagCategoryById_notFound() {
        when(tagCategoryRepository.findById(tagCategoryId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tagCategoryService.getTagCategoryById(tagCategoryId))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TAG_CATEGORY_NOT_FOUND);
    }

    @Test
    @DisplayName("createTagCategory - success")
    void createTagCategory_success() {
        CreateTagCategoryRequest request = CreateTagCategoryRequest.builder()
                .name("Mục đích")
                .code("PURPOSE")
                .approvalMode(ApprovalMode.OWNER_REQUEST)
                .controlType(ControlType.TAG_LIST)
                .displayOrder(2)
                .build();

        when(tagCategoryRepository.findByCode("PURPOSE")).thenReturn(Optional.empty());
        when(tagCategoryMapper.toEntity(request)).thenReturn(tagCategory);
        when(tagCategoryRepository.save(any(TagCategory.class))).thenReturn(tagCategory);
        when(tagCategoryMapper.toResponse(tagCategory)).thenReturn(tagCategoryResponse);

        TagCategoryResponse result = tagCategoryService.createTagCategory(request);

        assertThat(result).isNotNull();
        verify(tagCategoryRepository).save(any(TagCategory.class));
    }

    @Test
    @DisplayName("createTagCategory - duplicate code throws AppException")
    void createTagCategory_duplicateCode() {
        CreateTagCategoryRequest request = CreateTagCategoryRequest.builder()
                .name("Vibe")
                .code("VIBE")
                .approvalMode(ApprovalMode.OWNER_REQUEST)
                .controlType(ControlType.TAG_LIST)
                .build();

        when(tagCategoryRepository.findByCode("VIBE")).thenReturn(Optional.of(tagCategory));

        assertThatThrownBy(() -> tagCategoryService.createTagCategory(request))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TAG_CATEGORY_ALREADY_EXISTS);
    }

    @Test
    @DisplayName("deleteTagCategory - sets active to false")
    void deleteTagCategory_softDelete() {
        when(tagCategoryRepository.findById(tagCategoryId)).thenReturn(Optional.of(tagCategory));

        tagCategoryService.deleteTagCategory(tagCategoryId);

        assertThat(tagCategory.isActive()).isFalse();
        verify(tagCategoryRepository).save(tagCategory);
    }
}
