package com.moodcafe.tag.service;

import com.moodcafe.shared.error.ErrorCode;
import com.moodcafe.shared.exceptions.AppException;
import com.moodcafe.store.abstraction.repository.FavoriteStoreRepository;
import com.moodcafe.store.abstraction.repository.StoreImageRepository;
import com.moodcafe.store.abstraction.repository.StoreRepository;
import com.moodcafe.store.abstraction.repository.StoreReviewRepository;
import com.moodcafe.store.entity.Store;
import com.moodcafe.store.entity.StoreImage;
import com.moodcafe.store.entity.enums.StoreStatus;
import com.moodcafe.tag.abstraction.repository.StoreTagRepository;
import com.moodcafe.tag.abstraction.repository.TagCategoryRepository;
import com.moodcafe.tag.abstraction.repository.TagRepository;
import com.moodcafe.tag.abstraction.repository.UserPreferenceRepository;
import com.moodcafe.tag.dto.request.CreateTagRequest;
import com.moodcafe.tag.dto.response.CityTrendingResponse;
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
    private UserPreferenceRepository userPreferenceRepository;

    @Mock
    private FavoriteStoreRepository favoriteStoreRepository;

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private StoreReviewRepository storeReviewRepository;

    @Mock
    private StoreImageRepository storeImageRepository;

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

    @Test
    @DisplayName("getCityTrendingData - returns top 2 trending tags and top 2 favorite stores")
    void getCityTrendingData_Success() {
        Tag tag1 = Tag.builder().tagId(UUID.randomUUID()).name("Tag 1").category(category).imageUrl("http://img1.jpg").active(true).build();
        Tag tag2 = Tag.builder().tagId(UUID.randomUUID()).name("Tag 2").category(category).imageUrl("http://img2.jpg").active(true).build();

        when(tagRepository.findAllByActiveTrue()).thenReturn(List.of(tag1, tag2));
        when(storeTagRepository.countDistinctStoresGroupedByTag(any())).thenReturn(List.of(
                new Object[]{tag1.getTagId(), 5L},
                new Object[]{tag2.getTagId(), 3L}
        ));
        when(userPreferenceRepository.countPreferencesGroupedByTag()).thenReturn(List.of(
                new Object[]{tag1.getTagId(), 50L},
                new Object[]{tag2.getTagId(), 40L}
        ));

        Store store1 = Store.builder().storeId(UUID.randomUUID()).name("Store 1").address("123 Street").status(StoreStatus.ACTIVE).build();
        Store store2 = Store.builder().storeId(UUID.randomUUID()).name("Store 2").address("456 Avenue").status(StoreStatus.ACTIVE).build();

        when(storeRepository.findAllByStatus(StoreStatus.ACTIVE)).thenReturn(List.of(store1, store2));
        when(favoriteStoreRepository.countFavoritesGroupedByStore()).thenReturn(List.of(
                new Object[]{store1.getStoreId(), 25L},
                new Object[]{store2.getStoreId(), 15L}
        ));
        when(storeReviewRepository.findOverallRatingAndCountGroupedByStore()).thenReturn(List.of(
                new Object[]{store1.getStoreId(), 4.8, 12L},
                new Object[]{store2.getStoreId(), 4.5, 8L}
        ));
        when(storeImageRepository.findByStoreStoreIdAndPrimaryTrue(any())).thenReturn(Optional.empty());
        when(storeImageRepository.findAllByStoreStoreId(any())).thenReturn(List.of());

        List<CityTrendingResponse> result = tagService.getCityTrendingData();

        assertThat(result).hasSize(4);
        assertThat(result.get(0).getItemType()).isEqualTo("TAG");
        assertThat(result.get(0).getBadgeText()).isEqualTo("Đang quan tâm");
        assertThat(result.get(0).getInterestedCount()).isEqualTo(50L);
        assertThat(result.get(0).getStoreCount()).isEqualTo(5L);

        assertThat(result.get(1).getItemType()).isEqualTo("TAG");
        assertThat(result.get(1).getBadgeText()).isEqualTo("Đang quan tâm");
        assertThat(result.get(1).getInterestedCount()).isEqualTo(40L);
        assertThat(result.get(1).getStoreCount()).isEqualTo(3L);

        assertThat(result.get(2).getItemType()).isEqualTo("STORE");
        assertThat(result.get(2).getBadgeText()).isEqualTo("Được lưu nhiều");
        assertThat(result.get(2).getStoreName()).isEqualTo("Store 1");
        assertThat(result.get(2).getInterestedCount()).isEqualTo(25L);
        assertThat(result.get(2).getRating()).isEqualTo(4.8);

        assertThat(result.get(3).getItemType()).isEqualTo("STORE");
        assertThat(result.get(3).getBadgeText()).isEqualTo("Được lưu nhiều");
        assertThat(result.get(3).getStoreName()).isEqualTo("Store 2");
        assertThat(result.get(3).getInterestedCount()).isEqualTo(15L);
        assertThat(result.get(3).getRating()).isEqualTo(4.5);
    }

    @Test
    @DisplayName("getCityTrendingData - verifies tie-breaking rules and strict limit of exactly 4 cards (2 tags + 2 stores)")
    void getCityTrendingData_TieBreaking_AppliesOrderRulesAndStrictLimit4() {
        Tag tagA = Tag.builder().tagId(UUID.randomUUID()).name("Tag A").category(category).active(true).build();
        Tag tagB = Tag.builder().tagId(UUID.randomUUID()).name("Tag B").category(category).active(true).build();
        Tag tagC = Tag.builder().tagId(UUID.randomUUID()).name("Tag C").category(category).active(true).build();

        when(tagRepository.findAllByActiveTrue()).thenReturn(List.of(tagA, tagB, tagC));
        when(storeTagRepository.countDistinctStoresGroupedByTag(any())).thenReturn(List.of(
                new Object[]{tagA.getTagId(), 5L},
                new Object[]{tagB.getTagId(), 10L}, // tie on preferences with tagA, but tagB has more stores
                new Object[]{tagC.getTagId(), 2L}
        ));
        when(userPreferenceRepository.countPreferencesGroupedByTag()).thenReturn(List.of(
                new Object[]{tagA.getTagId(), 10L},
                new Object[]{tagB.getTagId(), 10L},
                new Object[]{tagC.getTagId(), 20L} // highest preferences
        ));

        Store storeA = Store.builder().storeId(UUID.randomUUID()).name("Store A").address("A").status(StoreStatus.ACTIVE).build();
        Store storeB = Store.builder().storeId(UUID.randomUUID()).name("Store B").address("B").status(StoreStatus.ACTIVE).build();
        Store storeC = Store.builder().storeId(UUID.randomUUID()).name("Store C").address("C").status(StoreStatus.ACTIVE).build();

        when(storeRepository.findAllByStatus(StoreStatus.ACTIVE)).thenReturn(List.of(storeA, storeB, storeC));
        when(favoriteStoreRepository.countFavoritesGroupedByStore()).thenReturn(List.of(
                new Object[]{storeA.getStoreId(), 5L},
                new Object[]{storeB.getStoreId(), 5L}, // tie on favorites with storeA, but storeA has higher rating
                new Object[]{storeC.getStoreId(), 12L} // highest favorites
        ));
        when(storeReviewRepository.findOverallRatingAndCountGroupedByStore()).thenReturn(List.of(
                new Object[]{storeA.getStoreId(), 4.9, 20L},
                new Object[]{storeB.getStoreId(), 4.2, 10L},
                new Object[]{storeC.getStoreId(), 4.0, 5L}
        ));
        when(storeImageRepository.findByStoreStoreIdAndPrimaryTrue(any())).thenReturn(Optional.empty());
        when(storeImageRepository.findAllByStoreStoreId(any())).thenReturn(List.of());

        List<CityTrendingResponse> result = tagService.getCityTrendingData();

        assertThat(result).hasSize(4);
        // Top 2 Tags: tagC (20 prefs), tagB (10 prefs, 10 stores)
        assertThat(result.get(0).getTagName()).isEqualTo("Tag C");
        assertThat(result.get(1).getTagName()).isEqualTo("Tag B");

        // Top 2 Stores: storeC (12 favs), storeA (5 favs, 4.9 rating)
        assertThat(result.get(2).getStoreName()).isEqualTo("Store C");
        assertThat(result.get(3).getStoreName()).isEqualTo("Store A");
    }
}
