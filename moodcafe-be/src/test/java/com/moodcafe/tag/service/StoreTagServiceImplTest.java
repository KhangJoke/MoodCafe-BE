package com.moodcafe.tag.service;

import com.moodcafe.auth.abstraction.service.CurrentUserService;
import com.moodcafe.shared.error.ErrorCode;
import com.moodcafe.shared.exceptions.AppException;
import com.moodcafe.store.abstraction.repository.StoreRepository;
import com.moodcafe.store.abstraction.service.StoreStaffService;
import com.moodcafe.store.entity.Store;
import com.moodcafe.tag.abstraction.repository.StoreTagRepository;
import com.moodcafe.tag.abstraction.repository.TagRepository;
import com.moodcafe.tag.dto.request.ReviewStoreTagRequest;
import com.moodcafe.tag.dto.request.SubmitStoreTagRequest;
import com.moodcafe.tag.dto.request.UpdateStoreHighlightTagsRequest;
import com.moodcafe.tag.dto.response.StoreAttributesResponse;
import com.moodcafe.tag.dto.response.StoreTagResponse;
import com.moodcafe.tag.entity.StoreTag;
import com.moodcafe.tag.entity.Tag;
import com.moodcafe.tag.entity.TagCategory;
import com.moodcafe.tag.entity.enums.ApprovalMode;
import com.moodcafe.tag.entity.enums.ControlType;
import com.moodcafe.tag.entity.enums.StoreTagStatus;
import com.moodcafe.tag.mapper.StoreTagMapper;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StoreTagServiceImplTest {

    @Mock
    private StoreTagRepository storeTagRepository;

    @Mock
    private TagRepository tagRepository;

    @Mock
    private StoreTagMapper storeTagMapper;

    @Mock
    private StoreStaffService storeStaffService;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private StoreRepository storeRepository;

    @InjectMocks
    private StoreTagServiceImpl storeTagService;

    private UUID storeId;
    private UUID tagId;
    private UUID storeTagId;
    private Tag tag;
    private StoreTag storeTag;
    private StoreTagResponse storeTagResponse;

    @BeforeEach
    void setUp() {
        storeId = UUID.randomUUID();
        tagId = UUID.randomUUID();
        storeTagId = UUID.randomUUID();

        tag = Tag.builder()
                .tagId(tagId)
                .name("Vintage")
                .build();

        storeTag = StoreTag.builder()
                .storeTagId(storeTagId)
                .storeId(storeId)
                .tag(tag)
                .status(StoreTagStatus.PENDING)
                .proofImageUrl("https://res.cloudinary.com/proof.jpg")
                .build();

        storeTagResponse = StoreTagResponse.builder()
                .storeTagId(storeTagId)
                .tagId(tagId)
                .tagName("Vintage")
                .status(StoreTagStatus.PENDING)
                .proofImageUrl("https://res.cloudinary.com/proof.jpg")
                .build();
    }

    @Test
    @DisplayName("getStoreAttributes - returns store tags")
    void getStoreAttributes_success() {
        when(storeTagRepository.findAllByStoreId(storeId)).thenReturn(List.of(storeTag));
        when(storeTagMapper.toResponse(storeTag)).thenReturn(storeTagResponse);

        StoreAttributesResponse response = storeTagService.getStoreAttributes(storeId);

        assertThat(response.getStoreId()).isEqualTo(storeId);
        assertThat(response.getTags()).hasSize(1);
    }

    @Test
    @DisplayName("requestStoreTag - owner creates new tag request with proof")
    void requestStoreTag_newRequest_success() {
        SubmitStoreTagRequest request = SubmitStoreTagRequest.builder()
                .tagId(tagId)
                .proofImageUrl("https://res.cloudinary.com/proof.jpg")
                .build();

        when(tagRepository.findById(tagId)).thenReturn(Optional.of(tag));
        when(storeTagRepository.findByStoreIdAndTagTagId(storeId, tagId)).thenReturn(Optional.empty());
        when(storeTagRepository.save(any(StoreTag.class))).thenReturn(storeTag);
        when(storeTagMapper.toResponse(storeTag)).thenReturn(storeTagResponse);

        StoreTagResponse response = storeTagService.requestStoreTag(storeId, request);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(StoreTagStatus.PENDING);
        verify(storeStaffService).requireStoreAccess(storeId, "OWNER", "MANAGER");
        verify(storeTagRepository).save(any(StoreTag.class));
    }

    @Test
    @DisplayName("requestStoreTag - throws if already requested or approved")
    void requestStoreTag_alreadyExists() {
        SubmitStoreTagRequest request = SubmitStoreTagRequest.builder()
                .tagId(tagId)
                .build();

        StoreTag existingApproved = StoreTag.builder()
                .storeId(storeId)
                .tag(tag)
                .status(StoreTagStatus.APPROVED)
                .build();

        when(tagRepository.findById(tagId)).thenReturn(Optional.of(tag));
        when(storeTagRepository.findByStoreIdAndTagTagId(storeId, tagId)).thenReturn(Optional.of(existingApproved));

        assertThatThrownBy(() -> storeTagService.requestStoreTag(storeId, request))
                .isInstanceOf(AppException.class)
                .satisfies(e -> assertThat(((AppException) e).getErrorCode())
                        .isEqualTo(ErrorCode.STORE_TAG_ALREADY_REQUESTED));
    }

    @Test
    @DisplayName("reviewStoreTagRequest - admin approves tag request")
    void reviewStoreTagRequest_approve_success() {
        ReviewStoreTagRequest request = ReviewStoreTagRequest.builder()
                .status(StoreTagStatus.APPROVED)
                .build();

        when(storeTagRepository.findById(storeTagId)).thenReturn(Optional.of(storeTag));
        when(storeTagRepository.save(storeTag)).thenReturn(storeTag);
        when(storeTagMapper.toResponse(storeTag)).thenReturn(storeTagResponse);

        StoreTagResponse response = storeTagService.reviewStoreTagRequest(storeTagId, request);

        assertThat(response).isNotNull();
        assertThat(storeTag.getStatus()).isEqualTo(StoreTagStatus.APPROVED);
        assertThat(storeTag.getApprovedAt()).isNotNull();
        verify(currentUserService).requireSystemAdmin();
    }

    @Test
    @DisplayName("reviewStoreTagRequest - admin rejects tag request with reason")
    void reviewStoreTagRequest_reject_success() {
        ReviewStoreTagRequest request = ReviewStoreTagRequest.builder()
                .status(StoreTagStatus.REJECTED)
                .rejectReason("Ảnh mờ không rõ ràng")
                .build();

        when(storeTagRepository.findById(storeTagId)).thenReturn(Optional.of(storeTag));
        when(storeTagRepository.save(storeTag)).thenReturn(storeTag);
        when(storeTagMapper.toResponse(storeTag)).thenReturn(storeTagResponse);

        StoreTagResponse response = storeTagService.reviewStoreTagRequest(storeTagId, request);

        assertThat(response).isNotNull();
        assertThat(storeTag.getStatus()).isEqualTo(StoreTagStatus.REJECTED);
        assertThat(storeTag.getRejectReason()).isEqualTo("Ảnh mờ không rõ ràng");
    }

    @Test
    @DisplayName("requestStoreTag - slider/owner_custom tag auto-approves and revokes prior tag in same category")
    void requestStoreTag_slider_autoApprovesAndRevokesPrior() {
        UUID categoryId = UUID.randomUUID();
        TagCategory noiseCategory = TagCategory.builder()
                .tagCategoryId(categoryId)
                .code("NOISE")
                .approvalMode(ApprovalMode.OWNER_CUSTOM)
                .controlType(ControlType.SLIDER)
                .build();

        UUID noiseTagId1 = UUID.randomUUID();
        Tag oldNoiseTag = Tag.builder()
                .tagId(noiseTagId1)
                .name("Yên tĩnh")
                .category(noiseCategory)
                .scaleValue(1)
                .build();

        UUID noiseTagId2 = UUID.randomUUID();
        Tag newNoiseTag = Tag.builder()
                .tagId(noiseTagId2)
                .name("Bình thường")
                .category(noiseCategory)
                .scaleValue(3)
                .build();

        StoreTag oldStoreTag = StoreTag.builder()
                .storeTagId(UUID.randomUUID())
                .storeId(storeId)
                .tag(oldNoiseTag)
                .status(StoreTagStatus.APPROVED)
                .build();

        SubmitStoreTagRequest request = SubmitStoreTagRequest.builder()
                .tagId(noiseTagId2)
                .build();

        when(tagRepository.findById(noiseTagId2)).thenReturn(Optional.of(newNoiseTag));
        when(storeTagRepository.findAllByStoreIdAndTagCategoryTagCategoryId(storeId, categoryId))
                .thenReturn(List.of(oldStoreTag));
        when(storeTagRepository.findByStoreIdAndTagTagId(storeId, noiseTagId2)).thenReturn(Optional.empty());

        StoreTag newStoreTag = StoreTag.builder()
                .storeId(storeId)
                .tag(newNoiseTag)
                .status(StoreTagStatus.APPROVED)
                .build();
        when(storeTagRepository.save(any(StoreTag.class))).thenReturn(newStoreTag);
        when(storeTagMapper.toResponse(any(StoreTag.class))).thenReturn(StoreTagResponse.builder()
                .tagId(noiseTagId2)
                .status(StoreTagStatus.APPROVED)
                .build());

        StoreTagResponse response = storeTagService.requestStoreTag(storeId, request);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(StoreTagStatus.APPROVED);
        assertThat(oldStoreTag.getStatus()).isEqualTo(StoreTagStatus.REVOKED);
    }

    @Test
    @DisplayName("updateStoreHighlightTags - owner successfully sets highlight tags")
    void updateStoreHighlightTags_success() {
        StoreTag tag1 = StoreTag.builder()
                .storeId(storeId)
                .tag(tag)
                .status(StoreTagStatus.APPROVED)
                .highlighted(false)
                .build();

        UUID tagId2 = UUID.randomUUID();
        Tag secondTag = Tag.builder().tagId(tagId2).name("Yên tĩnh").build();
        StoreTag tag2 = StoreTag.builder()
                .storeId(storeId)
                .tag(secondTag)
                .status(StoreTagStatus.APPROVED)
                .highlighted(false)
                .build();

        UpdateStoreHighlightTagsRequest request = UpdateStoreHighlightTagsRequest.builder()
                .tagIds(List.of(tagId))
                .build();

        when(storeTagRepository.findAllByStoreIdAndStatus(storeId, StoreTagStatus.APPROVED))
                .thenReturn(List.of(tag1, tag2));
        when(storeTagMapper.toResponse(any(StoreTag.class))).thenAnswer(inv -> {
            StoreTag st = inv.getArgument(0);
            return StoreTagResponse.builder()
                    .tagId(st.getTag().getTagId())
                    .highlighted(st.isHighlighted())
                    .build();
        });

        List<StoreTagResponse> result = storeTagService.updateStoreHighlightTags(storeId, request);

        assertThat(result).hasSize(2);
        assertThat(tag1.isHighlighted()).isTrue();
        assertThat(tag2.isHighlighted()).isFalse();
        verify(storeStaffService).requireStoreAccess(storeId, "OWNER", "MANAGER");
        verify(storeTagRepository).save(tag1);
    }

    @Test
    @DisplayName("updateStoreHighlightTags - throws exception if more than 4 tags")
    void updateStoreHighlightTags_moreThan4_throwsException() {
        UpdateStoreHighlightTagsRequest request = UpdateStoreHighlightTagsRequest.builder()
                .tagIds(List.of(
                        UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()
                ))
                .build();

        assertThatThrownBy(() -> storeTagService.updateStoreHighlightTags(storeId, request))
                .isInstanceOf(AppException.class)
                .satisfies(e -> assertThat(((AppException) e).getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
    }

    @Test
    @DisplayName("updateStoreHighlightTags - throws exception if tag is not approved for store")
    void updateStoreHighlightTags_unapprovedTag_throwsException() {
        UUID unapprovedTagId = UUID.randomUUID();
        UpdateStoreHighlightTagsRequest request = UpdateStoreHighlightTagsRequest.builder()
                .tagIds(List.of(unapprovedTagId))
                .build();

        when(storeTagRepository.findAllByStoreIdAndStatus(storeId, StoreTagStatus.APPROVED))
                .thenReturn(List.of(storeTag)); // storeTag has tagId, not unapprovedTagId

        assertThatThrownBy(() -> storeTagService.updateStoreHighlightTags(storeId, request))
                .isInstanceOf(AppException.class)
                .satisfies(e -> assertThat(((AppException) e).getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
    }

    @Test
    @DisplayName("getPendingStoreTagRequests - returns pending store tags with store name")
    void getPendingStoreTagRequests_returnsPendingTagsWithStoreName() {
        Store mockStore = Store.builder().storeId(storeId).name("Mood Cafe Test").build();
        when(storeTagRepository.findAllByStatusOrderByCreatedAtDesc(StoreTagStatus.PENDING))
                .thenReturn(List.of(storeTag));
        when(storeRepository.findAllById(any())).thenReturn(List.of(mockStore));
        when(storeTagMapper.toResponse(storeTag)).thenReturn(storeTagResponse);

        List<StoreTagResponse> result = storeTagService.getPendingStoreTagRequests();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStoreName()).isEqualTo("Mood Cafe Test");
        verify(currentUserService).requireSystemAdmin();
    }
}
