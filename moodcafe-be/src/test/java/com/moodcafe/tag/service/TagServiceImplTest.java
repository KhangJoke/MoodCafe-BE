package com.moodcafe.tag.service;

import com.moodcafe.shared.error.ErrorCode;
import com.moodcafe.shared.exceptions.AppException;
import com.moodcafe.tag.abstraction.repository.StoreTagRepository;
import com.moodcafe.tag.abstraction.repository.TagCategoryRepository;
import com.moodcafe.tag.abstraction.repository.TagRepository;
import com.moodcafe.tag.dto.request.CreateTagRequest;
import com.moodcafe.tag.dto.response.TagResponse;
import com.moodcafe.tag.entity.Tag;
import com.moodcafe.tag.entity.TagCategory;
import com.moodcafe.tag.mapper.TagMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TagServiceImplTest {

    @Mock
    private TagRepository tagRepository;

    @Mock
    private TagCategoryRepository tagCategoryRepository;

    @Mock
    private StoreTagRepository storeTagRepository;

    @Mock
    private TagMapper tagMapper;

    @InjectMocks
    private TagServiceImpl tagService;

    private UUID categoryId;
    private TagCategory category;
    private Tag sampleTag;
    private TagResponse sampleResponse;

    @BeforeEach
    void setUp() {
        categoryId = UUID.randomUUID();
        category = TagCategory.builder()
                .tagCategoryId(categoryId)
                .name("Vibe")
                .code("VIBE")
                .build();

        UUID tagId = UUID.randomUUID();
        sampleTag = Tag.builder()
                .tagId(tagId)
                .name("Yên tĩnh")
                .category(category)
                .active(true)
                .build();

        sampleResponse = TagResponse.builder()
                .tagId(tagId)
                .tagCategoryId(categoryId)
                .categoryName("Vibe")
                .categoryCode("VIBE")
                .name("Yên tĩnh")
                .build();
    }

    @Test
    @DisplayName("getAllTags - returns mapped list of active tags")
    void getAllTags_Success() {
        when(tagRepository.findAllByActiveTrue()).thenReturn(List.of(sampleTag));
        when(tagMapper.toResponse(sampleTag)).thenReturn(sampleResponse);

        List<TagResponse> results = tagService.getAllTags(null, null);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getName()).isEqualTo("Yên tĩnh");
        verify(tagRepository, times(1)).findAllByActiveTrue();
    }

    @Test
    @DisplayName("createTag - throws exception when tag name already exists")
    void createTag_DuplicateName_ThrowsAppException() {
        CreateTagRequest request = CreateTagRequest.builder()
                .name("Yên tĩnh")
                .tagCategoryId(categoryId)
                .build();

        when(tagRepository.existsByName("Yên tĩnh")).thenReturn(true);

        assertThatThrownBy(() -> tagService.createTag(request))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TAG_ALREADY_EXISTS);

        verify(tagRepository, never()).save(any());
    }

    @Test
    @DisplayName("createTag - success when tag name does not exist and vibe has imageUrl")
    void createTag_Success() {
        CreateTagRequest request = CreateTagRequest.builder()
                .name("Sân vườn")
                .tagCategoryId(categoryId)
                .imageUrl("/images/vibes/garden.jpg")
                .build();

        Tag newTag = Tag.builder()
                .name("Sân vườn")
                .category(category)
                .imageUrl("/images/vibes/garden.jpg")
                .active(true)
                .build();

        when(tagRepository.existsByName("Sân vườn")).thenReturn(false);
        when(tagCategoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(tagMapper.toEntity(request)).thenReturn(newTag);
        when(tagRepository.save(any(Tag.class))).thenReturn(newTag);
        when(tagMapper.toResponse(newTag)).thenReturn(sampleResponse);

        TagResponse response = tagService.createTag(request);

        assertThat(response).isNotNull();
        verify(tagRepository, times(1)).save(newTag);
    }

    @Test
    @DisplayName("createTag - throws exception when VIBE tag is missing imageUrl")
    void createTag_VibeMissingImage_ThrowsAppException() {
        CreateTagRequest request = CreateTagRequest.builder()
                .name("Sân vườn")
                .tagCategoryId(categoryId)
                .imageUrl(null)
                .build();

        when(tagRepository.existsByName("Sân vườn")).thenReturn(false);
        when(tagCategoryRepository.findById(categoryId)).thenReturn(Optional.of(category));

        assertThatThrownBy(() -> tagService.createTag(request))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.VIBE_IMAGE_REQUIRED);

        verify(tagRepository, never()).save(any());
    }

    @Test
    @DisplayName("deleteTag - soft deletes active tag by setting active to false")
    void deleteTag_Success() {
        UUID tagId = sampleTag.getTagId();
        when(tagRepository.findById(tagId)).thenReturn(Optional.of(sampleTag));
        when(tagRepository.save(sampleTag)).thenReturn(sampleTag);

        tagService.deleteTag(tagId);

        assertThat(sampleTag.isActive()).isFalse();
        verify(tagRepository, times(1)).save(sampleTag);
    }

    @Test
    @DisplayName("deleteTag - throws exception when tag not found")
    void deleteTag_NotFound_ThrowsAppException() {
        UUID nonExistentId = UUID.randomUUID();
        when(tagRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tagService.deleteTag(nonExistentId))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TAG_NOT_FOUND);

        verify(tagRepository, never()).save(any());
    }
}
