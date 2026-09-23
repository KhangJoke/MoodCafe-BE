package com.moodcafe.tag.service;

import com.moodcafe.auth.abstraction.service.CurrentUserService;
import com.moodcafe.shared.error.ErrorCode;
import com.moodcafe.shared.exceptions.AppException;
import com.moodcafe.store.abstraction.service.StoreStaffService;
import com.moodcafe.tag.abstraction.repository.StoreTagRepository;
import com.moodcafe.tag.abstraction.repository.TagRepository;
import com.moodcafe.tag.abstraction.service.StoreTagService;
import com.moodcafe.tag.dto.request.ReviewStoreTagRequest;
import com.moodcafe.tag.dto.request.SubmitStoreTagRequest;
import com.moodcafe.tag.dto.request.UpdateStoreHighlightTagsRequest;
import com.moodcafe.tag.dto.response.StoreAttributesResponse;
import com.moodcafe.tag.dto.response.StoreTagResponse;
import com.moodcafe.tag.entity.StoreTag;
import com.moodcafe.tag.entity.Tag;
import com.moodcafe.tag.entity.enums.ApprovalMode;
import com.moodcafe.tag.entity.enums.ControlType;
import com.moodcafe.tag.entity.enums.StoreTagStatus;
import com.moodcafe.tag.mapper.StoreTagMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StoreTagServiceImpl implements StoreTagService {

    private final StoreTagRepository storeTagRepository;
    private final TagRepository tagRepository;
    private final StoreTagMapper storeTagMapper;
    private final StoreStaffService storeStaffService;
    private final CurrentUserService currentUserService;

    @Override
    @Transactional(readOnly = true)
    public StoreAttributesResponse getStoreAttributes(UUID storeId) {
        List<StoreTag> tags = storeTagRepository.findAllByStoreId(storeId);

        List<StoreTagResponse> tagResponses = tags.stream()
                .map(storeTagMapper::toResponse)
                .toList();

        return StoreAttributesResponse.builder()
                .storeId(storeId)
                .tags(tagResponses)
                .build();
    }

    @Override
    @Transactional
    public StoreTagResponse requestStoreTag(UUID storeId, SubmitStoreTagRequest request) {
        storeStaffService.requireStoreAccess(storeId, "OWNER", "MANAGER");

        Tag tag = tagRepository.findById(request.getTagId())
                .orElseThrow(() -> new AppException(ErrorCode.TAG_NOT_FOUND));

        boolean isSliderCategory = tag.getCategory() != null && ControlType.SLIDER.equals(tag.getCategory().getControlType());
        boolean isOwnerCustom = tag.getCategory() != null && ApprovalMode.OWNER_CUSTOM.equals(tag.getCategory().getApprovalMode());

        if (isSliderCategory && tag.getCategory() != null) {
            // For a slider category (e.g. NOISE), a store can only have 1 active tag.
            List<StoreTag> categoryTags = storeTagRepository.findAllByStoreIdAndTagCategoryTagCategoryId(
                    storeId, tag.getCategory().getTagCategoryId());
            for (StoreTag st : categoryTags) {
                if (!st.getTag().getTagId().equals(tag.getTagId())) {
                    st.setStatus(StoreTagStatus.REVOKED);
                    st.setRevokedAt(Instant.now());
                    storeTagRepository.save(st);
                }
            }
        }

        Optional<StoreTag> existingOpt = storeTagRepository.findByStoreIdAndTagTagId(storeId, request.getTagId());

        StoreTag storeTag;
        StoreTagStatus targetStatus = isOwnerCustom ? StoreTagStatus.APPROVED : StoreTagStatus.PENDING;
        Instant approvedAt = isOwnerCustom ? Instant.now() : null;

        if (existingOpt.isPresent()) {
            storeTag = existingOpt.get();
            if (!isSliderCategory && (StoreTagStatus.APPROVED.equals(storeTag.getStatus()) || StoreTagStatus.PENDING.equals(storeTag.getStatus()))) {
                throw new AppException(ErrorCode.STORE_TAG_ALREADY_REQUESTED);
            }
            storeTag.setStatus(targetStatus);
            storeTag.setProofImageUrl(request.getProofImageUrl());
            storeTag.setRejectReason(null);
            storeTag.setApprovedAt(approvedAt);
            storeTag.setRevokedAt(null);
        } else {
            storeTag = StoreTag.builder()
                    .storeId(storeId)
                    .tag(tag)
                    .status(targetStatus)
                    .proofImageUrl(request.getProofImageUrl())
                    .approvedAt(approvedAt)
                    .build();
        }

        storeTag = storeTagRepository.save(storeTag);
        return storeTagMapper.toResponse(storeTag);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StoreTagResponse> getStoreTagsManagement(UUID storeId) {
        storeStaffService.requireStoreAccess(storeId, "OWNER", "MANAGER");
        return storeTagRepository.findAllByStoreId(storeId).stream()
                .map(storeTagMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public StoreTagResponse resubmitStoreTag(UUID storeId, UUID storeTagId, SubmitStoreTagRequest request) {
        storeStaffService.requireStoreAccess(storeId, "OWNER", "MANAGER");

        StoreTag storeTag = storeTagRepository.findById(storeTagId)
                .orElseThrow(() -> new AppException(ErrorCode.STORE_TAG_NOT_FOUND));

        if (!storeTag.getStoreId().equals(storeId)) {
            throw new AppException(ErrorCode.FORBIDDEN_STORE_ACCESS);
        }

        if (!StoreTagStatus.REJECTED.equals(storeTag.getStatus())) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Chỉ có thẻ vibe bị từ chối mới có thể nộp lại");
        }

        if (!storeTag.isAllowResubmit()) {
            throw new AppException(ErrorCode.STORE_TAG_RESUBMIT_NOT_ALLOWED);
        }

        Tag tag = storeTag.getTag();
        boolean isSliderCategory = tag.getCategory() != null && ControlType.SLIDER.equals(tag.getCategory().getControlType());
        if (!isSliderCategory && (request.getProofImageUrl() == null || request.getProofImageUrl().isBlank())) {
            throw new AppException(ErrorCode.STORE_TAG_PROOF_REQUIRED);
        }

        storeTag.setProofImageUrl(request.getProofImageUrl());
        storeTag.setStatus(StoreTagStatus.PENDING);
        storeTag.setRejectReason(null);
        storeTag = storeTagRepository.save(storeTag);

        return storeTagMapper.toResponse(storeTag);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StoreTagResponse> getPendingStoreTagRequests() {
        currentUserService.requireSystemAdmin();
        return storeTagRepository.findAllByStatusOrderByCreatedAtDesc(StoreTagStatus.PENDING).stream()
                .map(storeTagMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public StoreTagResponse reviewStoreTagRequest(UUID storeTagId, ReviewStoreTagRequest request) {
        currentUserService.requireSystemAdmin();

        StoreTag storeTag = storeTagRepository.findById(storeTagId)
                .orElseThrow(() -> new AppException(ErrorCode.STORE_TAG_NOT_FOUND));

        StoreTagStatus targetStatus = request.getStatus();
        storeTag.setStatus(targetStatus);

        if (StoreTagStatus.APPROVED.equals(targetStatus)) {
            storeTag.setApprovedAt(Instant.now());
            storeTag.setRejectReason(null);
            storeTag.setAllowResubmit(true);
        } else if (StoreTagStatus.REJECTED.equals(targetStatus) || StoreTagStatus.REVOKED.equals(targetStatus)) {
            storeTag.setRejectReason(request.getRejectReason());
            storeTag.setAllowResubmit(request.getAllowResubmit() != null ? request.getAllowResubmit() : true);
            if (StoreTagStatus.REVOKED.equals(targetStatus)) {
                storeTag.setRevokedAt(Instant.now());
            }
        }

        storeTag = storeTagRepository.save(storeTag);
        return storeTagMapper.toResponse(storeTag);
    }

    @Override
    @Transactional
    public List<StoreTagResponse> updateStoreHighlightTags(UUID storeId, UpdateStoreHighlightTagsRequest request) {
        storeStaffService.requireStoreAccess(storeId, "OWNER", "MANAGER");

        List<UUID> targetTagIds = (request != null && request.getTagIds() != null) ? request.getTagIds() : List.of();
        if (targetTagIds.size() > 4) {
            throw new AppException(ErrorCode.INVALID_INPUT, "Tối đa chỉ được chọn 4 thẻ hiển thị trên thẻ quán");
        }

        List<StoreTag> storeTags = storeTagRepository.findAllByStoreIdAndStatus(storeId, StoreTagStatus.APPROVED);
        Set<UUID> approvedTagIds = storeTags.stream()
                .filter(st -> st.getTag() != null)
                .map(st -> st.getTag().getTagId())
                .collect(Collectors.toSet());

        for (UUID tagId : targetTagIds) {
            if (!approvedTagIds.contains(tagId)) {
                throw new AppException(ErrorCode.INVALID_INPUT, "Thẻ được chọn phải thuộc danh sách thẻ đã được duyệt của quán");
            }
        }

        Set<UUID> targetSet = new HashSet<>(targetTagIds);
        for (StoreTag st : storeTags) {
            if (st.getTag() == null) continue;
            boolean shouldHighlight = targetSet.contains(st.getTag().getTagId());
            if (st.isHighlighted() != shouldHighlight) {
                st.setHighlighted(shouldHighlight);
                storeTagRepository.save(st);
            }
        }

        return storeTagRepository.findAllByStoreIdAndStatus(storeId, StoreTagStatus.APPROVED).stream()
                .map(storeTagMapper::toResponse)
                .toList();
    }
}
