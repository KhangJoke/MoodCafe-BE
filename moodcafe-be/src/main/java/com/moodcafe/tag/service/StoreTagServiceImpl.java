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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
        } else if (StoreTagStatus.REJECTED.equals(targetStatus) || StoreTagStatus.REVOKED.equals(targetStatus)) {
            storeTag.setRejectReason(request.getRejectReason());
            if (StoreTagStatus.REVOKED.equals(targetStatus)) {
                storeTag.setRevokedAt(Instant.now());
            }
        }

        storeTag = storeTagRepository.save(storeTag);
        return storeTagMapper.toResponse(storeTag);
    }
}
