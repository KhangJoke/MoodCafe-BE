package com.moodcafe.tag.service;

import com.moodcafe.auth.abstraction.service.CurrentUserService;
import com.moodcafe.shared.error.ErrorCode;
import com.moodcafe.shared.exceptions.AppException;
import com.moodcafe.store.abstraction.service.StoreStaffService;
import com.moodcafe.tag.abstraction.repository.StoreTagRepository;
import com.moodcafe.tag.abstraction.repository.TagCategoryRepository;
import com.moodcafe.tag.abstraction.repository.TagRepository;
import com.moodcafe.tag.abstraction.service.StoreTagService;
import com.moodcafe.tag.dto.request.RevokeStoreTagAdminRequest;
import com.moodcafe.tag.dto.request.ReviewStoreTagRequest;
import com.moodcafe.tag.dto.request.SubmitStoreTagRequest;
import com.moodcafe.tag.dto.request.UpdateStoreHighlightTagsRequest;
import com.moodcafe.tag.dto.response.StoreAttributesResponse;
import com.moodcafe.tag.dto.response.StoreTagResponse;
import com.moodcafe.store.abstraction.repository.StoreRepository;
import com.moodcafe.store.entity.Store;
import com.moodcafe.tag.entity.StoreTag;
import com.moodcafe.tag.entity.Tag;
import com.moodcafe.tag.entity.TagCategory;
import com.moodcafe.tag.entity.enums.ApprovalMode;
import com.moodcafe.tag.entity.enums.ControlType;
import com.moodcafe.tag.entity.enums.StoreTagStatus;
import com.moodcafe.tag.mapper.StoreTagMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StoreTagServiceImpl implements StoreTagService {

    private final StoreTagRepository storeTagRepository;
    private final TagRepository tagRepository;
    private final TagCategoryRepository tagCategoryRepository;
    private final StoreTagMapper storeTagMapper;
    private final StoreStaffService storeStaffService;
    private final CurrentUserService currentUserService;
    private final StoreRepository storeRepository;

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

    private String resolveProofImages(SubmitStoreTagRequest request) {
        if (request == null || request.getProofImageUrls() == null) return null;
        List<String> urls = request.getProofImageUrls().stream()
                .filter(u -> u != null && !u.isBlank())
                .map(String::trim)
                .toList();

        if (urls.size() > 3) {
            throw new AppException(ErrorCode.INVALID_INPUT, "Chỉ được tải lên tối đa 3 ảnh minh chứng.");
        }

        return urls.isEmpty() ? null : String.join(",", urls);
    }

    @Override
    @Transactional
    public StoreTagResponse requestStoreTag(UUID storeId, SubmitStoreTagRequest request) {
        storeStaffService.requireStoreAccess(storeId, "OWNER", "MANAGER");

        String resolvedProofImages = resolveProofImages(request);

        Tag tag;
        if (request.getTagId() != null) {
            tag = tagRepository.findById(request.getTagId())
                    .orElseThrow(() -> new AppException(ErrorCode.TAG_NOT_FOUND));
        } else if (request.getCustomTagName() != null && !request.getCustomTagName().trim().isEmpty()) {
            String cleanTagName = request.getCustomTagName().trim();
            if (request.getCategoryCode() == null || request.getCategoryCode().trim().isEmpty()) {
                throw new AppException(ErrorCode.INVALID_INPUT, "Vui lòng chọn danh mục cho thẻ đề xuất.");
            }
            String catCode = request.getCategoryCode().trim().toUpperCase();
            TagCategory category = tagCategoryRepository.findByCode(catCode)
                    .orElseThrow(() -> new AppException(ErrorCode.TAG_CATEGORY_NOT_FOUND));

            if (ApprovalMode.OWNER_CUSTOM.equals(category.getApprovalMode())) {
                throw new AppException(ErrorCode.INVALID_INPUT, "Danh mục này không cho phép chủ quán đề xuất tên thẻ mới.");
            }

            if (resolvedProofImages == null || resolvedProofImages.isBlank()) {
                throw new AppException(ErrorCode.INVALID_INPUT, "Ảnh minh chứng là bắt buộc khi đề xuất thẻ mới.");
            }

            Optional<Tag> existingTagOpt = tagRepository.findByName(cleanTagName);
            if (existingTagOpt.isPresent()) {
                tag = existingTagOpt.get();
            } else {
                tag = Tag.builder()
                        .name(cleanTagName)
                        .category(category)
                        .active(false)
                        .build();
                tag = tagRepository.save(tag);
            }
        } else {
            throw new AppException(ErrorCode.INVALID_INPUT, "Vui lòng chọn thẻ hoặc nhập tên thẻ đề xuất mới");
        }

        boolean isSliderCategory = tag.getCategory() != null && ControlType.SLIDER.equals(tag.getCategory().getControlType());
        boolean isOwnerCustom = tag.getCategory() != null && ApprovalMode.OWNER_CUSTOM.equals(tag.getCategory().getApprovalMode());
        boolean isOwnerRequest = tag.getCategory() != null && ApprovalMode.OWNER_REQUEST.equals(tag.getCategory().getApprovalMode());

        Optional<StoreTag> existingOpt = storeTagRepository.findByStoreIdAndTagTagId(storeId, tag.getTagId());
        if (existingOpt.isPresent()) {
            StoreTag existing = existingOpt.get();
            if (!isSliderCategory && (StoreTagStatus.APPROVED.equals(existing.getStatus()) || StoreTagStatus.PENDING.equals(existing.getStatus()))) {
                throw new AppException(ErrorCode.STORE_TAG_ALREADY_REQUESTED);
            }
        }

        if (isOwnerRequest && (resolvedProofImages == null || resolvedProofImages.isBlank())) {
            throw new AppException(ErrorCode.STORE_TAG_PROOF_REQUIRED, "Ảnh minh chứng là bắt buộc cho thẻ thuộc danh mục cần kiểm duyệt.");
        }

        if (isSliderCategory && tag.getCategory() != null) {
            // For a slider category (e.g. NOISE), a store can only have 1 active tag.
            List<StoreTag> categoryTags = storeTagRepository.findAllByStoreIdAndTagCategoryTagCategoryId(
                    storeId, tag.getCategory().getTagCategoryId());
            for (StoreTag st : categoryTags) {
                if (!st.getTag().getTagId().equals(tag.getTagId())) {
                    st.setStatus(StoreTagStatus.REVOKED);
                    st.setRevokedAt(Instant.now());
                    storeTagRepository.delete(st);
                }
            }
        }

        StoreTag storeTag;
        StoreTagStatus targetStatus = isOwnerCustom ? StoreTagStatus.APPROVED : StoreTagStatus.PENDING;
        Instant approvedAt = isOwnerCustom ? Instant.now() : null;

        if (existingOpt.isPresent()) {
            storeTag = existingOpt.get();
            storeTag.setStatus(targetStatus);
            storeTag.setProofImageUrl(resolvedProofImages);
            storeTag.setRejectReason(null);
            storeTag.setApprovedAt(approvedAt);
            storeTag.setRevokedAt(null);
        } else {
            storeTag = StoreTag.builder()
                    .storeId(storeId)
                    .tag(tag)
                    .status(targetStatus)
                    .proofImageUrl(resolvedProofImages)
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
                .filter(st -> !StoreTagStatus.REVOKED.equals(st.getStatus()))
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

        String resolvedProofImages = resolveProofImages(request);

        Tag tag = storeTag.getTag();
        boolean isOwnerCustom = tag.getCategory() != null && ApprovalMode.OWNER_CUSTOM.equals(tag.getCategory().getApprovalMode());
        if (!isOwnerCustom && (resolvedProofImages == null || resolvedProofImages.isBlank())) {
            throw new AppException(ErrorCode.STORE_TAG_PROOF_REQUIRED);
        }

        storeTag.setProofImageUrl(resolvedProofImages);
        storeTag.setStatus(isOwnerCustom ? StoreTagStatus.APPROVED : StoreTagStatus.PENDING);
        storeTag.setApprovedAt(isOwnerCustom ? Instant.now() : null);
        storeTag.setRejectReason(null);
        storeTag = storeTagRepository.save(storeTag);

        return storeTagMapper.toResponse(storeTag);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StoreTagResponse> getPendingStoreTagRequests() {
        currentUserService.requireSystemAdmin();
        List<StoreTag> pendingTags = storeTagRepository.findAllByStatusOrderByCreatedAtDesc(StoreTagStatus.PENDING);
        if (pendingTags.isEmpty()) {
            return List.of();
        }

        Set<UUID> storeIds = pendingTags.stream()
                .map(StoreTag::getStoreId)
                .collect(Collectors.toSet());
        Map<UUID, String> storeNames = storeRepository.findAllById(storeIds).stream()
                .collect(Collectors.toMap(Store::getStoreId, Store::getName, (a, b) -> a));

        return pendingTags.stream()
                .map(st -> {
                    StoreTagResponse resp = storeTagMapper.toResponse(st);
                    resp.setStoreName(storeNames.get(st.getStoreId()));
                    return resp;
                })
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
            if (storeTag.getTag() != null && !storeTag.getTag().isActive()) {
                Tag tag = storeTag.getTag();
                tag.setActive(true);
                tagRepository.save(tag);
            }
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
    public StoreTagResponse revokeStoreTagByAdmin(UUID storeTagId, RevokeStoreTagAdminRequest request) {
        currentUserService.requireSystemAdmin();

        StoreTag storeTag = storeTagRepository.findById(storeTagId)
                .orElseThrow(() -> new AppException(ErrorCode.STORE_TAG_NOT_FOUND));

        storeTag.setStatus(StoreTagStatus.REVOKED);
        storeTag.setRevokedAt(Instant.now());
        String reason = (request != null && request.getReason() != null && !request.getReason().isBlank())
                ? request.getReason().trim()
                : "Bị quản trị viên thu hồi";
        storeTag.setRejectReason(reason);
        storeTag.setAllowResubmit(false);
        storeTagRepository.delete(storeTag);

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
