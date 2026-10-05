package com.moodcafe.store.service;

import com.moodcafe.auth.abstraction.service.CurrentUserService;
import com.moodcafe.auth.entity.User;
import com.moodcafe.configuration.abstraction.service.SystemConfigurationService;
import com.moodcafe.configuration.dto.response.MatchScoreWeights;
import com.moodcafe.notification.abstraction.service.NotificationDispatcherService;
import com.moodcafe.notification.entity.enums.NotificationType;
import com.moodcafe.shared.error.ErrorCode;
import com.moodcafe.shared.exceptions.AppException;
import com.moodcafe.shared.response.PageResponse;
import com.moodcafe.store.abstraction.repository.FavoriteStoreRepository;
import com.moodcafe.store.abstraction.repository.StoreImageRepository;
import com.moodcafe.store.abstraction.repository.StoreRepository;
import com.moodcafe.store.abstraction.repository.StoreReviewRepository;
import com.moodcafe.store.abstraction.repository.StoreRoleRepository;
import com.moodcafe.store.abstraction.repository.StoreStaffRepository;
import com.moodcafe.store.abstraction.service.StoreService;
import com.moodcafe.store.abstraction.service.StoreStaffService;
import com.moodcafe.store.abstraction.repository.TagRatingRepository;
import com.moodcafe.store.dto.request.CreateStoreRequest;
import com.moodcafe.store.dto.request.StoreSearchRequest;
import com.moodcafe.store.dto.request.UpdateStoreRequest;
import com.moodcafe.store.dto.request.UpdateStoreStatusRequest;
import com.moodcafe.store.dto.response.FeaturedMoodStoreResponse;
import com.moodcafe.store.dto.response.StoreImageResponse;
import com.moodcafe.store.dto.response.StoreResponse;
import com.moodcafe.store.dto.response.StoreReviewResponse;
import com.moodcafe.store.dto.response.StoreReviewSummaryResponse;
import com.moodcafe.store.dto.response.StoreSearchItemResponse;
import com.moodcafe.store.dto.response.StoreSearchItemResponse.StoreSearchTagItem;
import com.moodcafe.store.entity.ReviewImage;
import com.moodcafe.store.entity.Store;
import com.moodcafe.store.entity.StoreImage;
import com.moodcafe.store.entity.StoreReview;
import com.moodcafe.store.entity.StoreRole;
import com.moodcafe.store.entity.StoreStaff;
import com.moodcafe.store.entity.enums.StoreStaffStatus;
import com.moodcafe.store.entity.enums.StoreStatus;
import com.moodcafe.store.mapper.StoreImageMapper;
import com.moodcafe.store.mapper.StoreMapper;
import com.moodcafe.store.mapper.StoreReviewMapper;
import com.moodcafe.tag.abstraction.repository.StoreTagRepository;
import com.moodcafe.tag.abstraction.repository.TagCategoryRepository;
import com.moodcafe.tag.abstraction.repository.TagRepository;
import com.moodcafe.tag.abstraction.repository.UserPreferenceRepository;
import com.moodcafe.store.abstraction.repository.VisitVerificationRepository;
import com.moodcafe.store.dto.request.RegisterStoreTagItem;
import com.moodcafe.store.dto.request.StoreRegisterRequest;
import com.moodcafe.store.dto.request.StoreResubmitRequest;
import com.moodcafe.store.dto.response.MerchantDashboardResponse;
import com.moodcafe.store.dto.response.MerchantDashboardResponse.MerchantMetrics;
import com.moodcafe.store.dto.response.MerchantDashboardResponse.MerchantRecentReview;
import com.moodcafe.store.dto.response.MerchantDashboardResponse.MerchantRecentSnap;
import com.moodcafe.store.dto.response.MerchantDashboardResponse.MerchantStoreSummary;
import com.moodcafe.store.dto.response.StoreRegistrationStatusResponse;
import com.moodcafe.store.entity.VisitVerification;
import com.moodcafe.tag.dto.response.StoreTagResponse;
import com.moodcafe.tag.entity.StoreTag;
import com.moodcafe.tag.entity.Tag;
import com.moodcafe.tag.entity.TagCategory;
import com.moodcafe.tag.entity.UserPreference;
import com.moodcafe.subscription.abstraction.service.SubscriptionService;
import com.moodcafe.tag.entity.enums.ApprovalMode;
import com.moodcafe.tag.entity.enums.ControlType;
import com.moodcafe.tag.entity.enums.StoreTagStatus;
import com.moodcafe.tag.mapper.StoreTagMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StoreServiceImpl implements StoreService {

    private final StoreRepository storeRepository;
    private final StoreRoleRepository storeRoleRepository;
    private final StoreStaffRepository storeStaffRepository;
    private final StoreImageRepository storeImageRepository;
    private final StoreTagRepository storeTagRepository;
    private final TagRepository tagRepository;
    private final TagCategoryRepository tagCategoryRepository;
    private final StoreMapper storeMapper;
    private final StoreImageMapper storeImageMapper;
    private final StoreTagMapper storeTagMapper;
    private final StoreStaffService storeStaffService;
    private final CurrentUserService currentUserService;
    private final FavoriteStoreRepository favoriteStoreRepository;
    private final StoreReviewRepository storeReviewRepository;
    private final UserPreferenceRepository userPreferenceRepository;
    private final SystemConfigurationService configurationService;
    private final StoreReviewMapper storeReviewMapper;
    private final TagRatingRepository tagRatingRepository;
    private final VisitVerificationRepository visitVerificationRepository;
    private final SubscriptionService subscriptionService;
    private final NotificationDispatcherService notificationDispatcherService;

    @Override
    @Transactional
    public StoreResponse createStore(CreateStoreRequest request) {
        User currentUser = currentUserService.getCurrentUser();
        subscriptionService.validateBranchLimit(currentUser.getUserId());

        Store store = storeMapper.toEntity(request);
        Long priceFrom = request.getPriceFrom();
        Long priceTo = request.getPriceTo();
        if (priceFrom == null && priceTo == null) {
            priceFrom = 30000L;
            priceTo = 65000L;
        } else if (priceFrom != null && priceTo == null) {
            priceTo = priceFrom;
        } else if (priceTo != null && priceFrom == null) {
            priceFrom = priceTo;
        }
        store.setPriceFrom(priceFrom);
        store.setPriceTo(priceTo);
        store.setStatus(StoreStatus.PENDING);
        store = storeRepository.save(store);

        // Register the creator as the OWNER in store_staffs
        StoreRole ownerRole = storeRoleRepository.findByName("OWNER")
                .orElseThrow(() -> new AppException(ErrorCode.STORE_ROLE_NOT_FOUND, "OWNER role not found"));

        StoreStaff staff = StoreStaff.builder()
                .store(store)
                .user(currentUser)
                .storeRole(ownerRole)
                .status(StoreStaffStatus.ACTIVE)
                .joinedAt(Instant.now())
                .build();

        storeStaffRepository.save(staff);

        notificationDispatcherService.dispatchToAdmins(
                "Cơ sở mới đăng ký",
                "Quán \"" + store.getName() + "\" vừa được tạo và đang chờ duyệt.",
                NotificationType.STORE_REGISTRATION_SUBMITTED,
                store.getStoreId().toString(),
                "/admin/stores?status=PENDING"
        );

        return toStoreResponse(store);
    }

    @Override
    @Transactional
    public StoreRegistrationStatusResponse registerStore(StoreRegisterRequest request) {
        User currentUser = currentUserService.getCurrentUser();
        subscriptionService.validateBranchLimit(currentUser.getUserId());

        if (request.getTags() == null || request.getTags().isEmpty()) {
            throw new AppException(ErrorCode.STORE_TAG_REQUIRED);
        }

        // Validate each tag
        for (RegisterStoreTagItem item : request.getTags()) {
            Tag tag = tagRepository.findById(item.getTagId())
                    .orElseThrow(() -> new AppException(ErrorCode.TAG_NOT_FOUND));

            boolean isSliderCategory = tag.getCategory() != null &&
                    ControlType.SLIDER.equals(tag.getCategory().getControlType());

            if (!isSliderCategory && (item.getProofImageUrl() == null || item.getProofImageUrl().isBlank())) {
                throw new AppException(ErrorCode.STORE_TAG_PROOF_REQUIRED,
                        "Thẻ " + tag.getName() + " yêu cầu đính kèm ảnh minh chứng thực tế");
            }
        }

        Long priceFrom = request.getPriceFrom();
        Long priceTo = request.getPriceTo();
        if (priceFrom == null && priceTo == null) {
            priceFrom = 30000L;
            priceTo = 65000L;
        } else if (priceFrom != null && priceTo == null) {
            priceTo = priceFrom;
        } else if (priceTo != null && priceFrom == null) {
            priceFrom = priceTo;
        }
        Store store = Store.builder()
                .name(request.getName())
                .description(request.getDescription())
                .address(request.getAddress())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .openingTime(request.getOpeningTime())
                .closingTime(request.getClosingTime())
                .priceFrom(priceFrom)
                .priceTo(priceTo)
                .phone(request.getPhone())
                .email(request.getEmail())
                .status(StoreStatus.PENDING)
                .allowResubmit(true)
                .build();
        store = storeRepository.save(store);

        // Save store images
        if (request.getImageUrls() != null && !request.getImageUrls().isEmpty()) {
            boolean first = true;
            for (String url : request.getImageUrls()) {
                if (url != null && !url.isBlank()) {
                    StoreImage img = StoreImage.builder()
                            .store(store)
                            .imageUrl(url)
                            .primary(first)
                            .build();
                    storeImageRepository.save(img);
                    first = false;
                }
            }
        }

        // Save store tags
        for (RegisterStoreTagItem item : request.getTags()) {
            Tag tag = tagRepository.findById(item.getTagId()).orElseThrow();
            StoreTag st = StoreTag.builder()
                    .storeId(store.getStoreId())
                    .tag(tag)
                    .status(StoreTagStatus.PENDING)
                    .proofImageUrl(item.getProofImageUrl())
                    .allowResubmit(true)
                    .build();
            storeTagRepository.save(st);
        }

        // Assign user as OWNER in store_staffs
        StoreRole ownerRole = storeRoleRepository.findByName("OWNER")
                .orElseThrow(() -> new AppException(ErrorCode.STORE_ROLE_NOT_FOUND, "OWNER role not found"));

        StoreStaff staff = StoreStaff.builder()
                .store(store)
                .user(currentUser)
                .storeRole(ownerRole)
                .status(StoreStaffStatus.ACTIVE)
                .joinedAt(Instant.now())
                .build();
        storeStaffRepository.save(staff);

        notificationDispatcherService.dispatchToAdmins(
                "Cơ sở mới đăng ký",
                "Quán \"" + store.getName() + "\" vừa nộp hồ sơ đăng ký và đang chờ duyệt.",
                NotificationType.STORE_REGISTRATION_SUBMITTED,
                store.getStoreId().toString(),
                "/admin/stores?status=PENDING"
        );

        return toStoreRegistrationStatusResponse(store);
    }

    @Override
    @Transactional
    public StoreRegistrationStatusResponse resubmitStore(UUID storeId, StoreResubmitRequest request) {
        storeStaffService.requireStoreAccess(storeId, "OWNER");

        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new AppException(ErrorCode.STORE_NOT_FOUND));

        if (store.getStatus() != StoreStatus.REJECTED) {
            throw new AppException(ErrorCode.STORE_NOT_REJECTED);
        }

        if (!store.isAllowResubmit()) {
            throw new AppException(ErrorCode.STORE_RESUBMIT_NOT_ALLOWED);
        }

        if (request.getTags() == null || request.getTags().isEmpty()) {
            throw new AppException(ErrorCode.STORE_TAG_REQUIRED);
        }

        for (RegisterStoreTagItem item : request.getTags()) {
            Tag tag = tagRepository.findById(item.getTagId())
                    .orElseThrow(() -> new AppException(ErrorCode.TAG_NOT_FOUND));

            boolean isSliderCategory = tag.getCategory() != null &&
                    ControlType.SLIDER.equals(tag.getCategory().getControlType());

            if (!isSliderCategory && (item.getProofImageUrl() == null || item.getProofImageUrl().isBlank())) {
                throw new AppException(ErrorCode.STORE_TAG_PROOF_REQUIRED,
                        "Thẻ " + tag.getName() + " yêu cầu đính kèm ảnh minh chứng thực tế");
            }
        }

        store.setName(request.getName());
        store.setDescription(request.getDescription());
        store.setAddress(request.getAddress());
        store.setLatitude(request.getLatitude());
        store.setLongitude(request.getLongitude());
        store.setOpeningTime(request.getOpeningTime());
        store.setClosingTime(request.getClosingTime());
        Long priceFrom = request.getPriceFrom();
        Long priceTo = request.getPriceTo();
        if (priceFrom == null && priceTo == null) {
            priceFrom = store.getPriceFrom() != null ? store.getPriceFrom() : 30000L;
            priceTo = store.getPriceTo() != null ? store.getPriceTo() : 65000L;
        } else if (priceFrom != null && priceTo == null) {
            priceTo = priceFrom;
        } else if (priceTo != null && priceFrom == null) {
            priceFrom = priceTo;
        }
        store.setPriceFrom(priceFrom);
        store.setPriceTo(priceTo);
        store.setPhone(request.getPhone());
        store.setEmail(request.getEmail());
        store.setStatus(StoreStatus.PENDING);
        store.setRejectReason(null);
        store.setAllowResubmit(true);
        store = storeRepository.save(store);

        // Update images if provided
        if (request.getImageUrls() != null && !request.getImageUrls().isEmpty()) {
            List<StoreImage> oldImages = storeImageRepository.findAllByStoreStoreId(storeId);
            storeImageRepository.deleteAll(oldImages);
            storeImageRepository.flush();

            boolean first = true;
            for (String url : request.getImageUrls()) {
                if (url != null && !url.isBlank()) {
                    StoreImage img = StoreImage.builder()
                            .store(store)
                            .imageUrl(url)
                            .primary(first)
                            .build();
                    storeImageRepository.save(img);
                    first = false;
                }
            }
            storeImageRepository.flush();
        }

        // Deduplicate requested tags by tagId
        Map<UUID, RegisterStoreTagItem> requestedTagMap = new LinkedHashMap<>();
        for (RegisterStoreTagItem item : request.getTags()) {
            if (item != null && item.getTagId() != null) {
                requestedTagMap.put(item.getTagId(), item);
            }
        }

        // Synchronize store tags in-place to avoid uq_store_vibe_tag unique constraint violation
        List<StoreTag> currentStoreTags = storeTagRepository.findAllByStoreId(storeId);
        Map<UUID, StoreTag> existingTagMap = currentStoreTags.stream()
                .filter(st -> st.getTag() != null && st.getTag().getTagId() != null)
                .collect(Collectors.toMap(st -> st.getTag().getTagId(), st -> st, (a, b) -> a));

        List<StoreTag> tagsToDelete = new ArrayList<>();
        for (StoreTag st : currentStoreTags) {
            if (st.getTag() != null && !requestedTagMap.containsKey(st.getTag().getTagId())) {
                tagsToDelete.add(st);
            }
        }

        if (!tagsToDelete.isEmpty()) {
            storeTagRepository.deleteAll(tagsToDelete);
            storeTagRepository.flush();
        }

        for (RegisterStoreTagItem item : requestedTagMap.values()) {
            StoreTag existing = existingTagMap.get(item.getTagId());
            if (existing != null) {
                existing.setStatus(StoreTagStatus.PENDING);
                existing.setProofImageUrl(item.getProofImageUrl());
                existing.setRejectReason(null);
                existing.setAllowResubmit(true);
                existing.setApprovedAt(null);
                existing.setRevokedAt(null);
                storeTagRepository.save(existing);
            } else {
                Tag tag = tagRepository.findById(item.getTagId())
                        .orElseThrow(() -> new AppException(ErrorCode.TAG_NOT_FOUND));
                StoreTag st = StoreTag.builder()
                        .storeId(store.getStoreId())
                        .tag(tag)
                        .status(StoreTagStatus.PENDING)
                        .proofImageUrl(item.getProofImageUrl())
                        .allowResubmit(true)
                        .build();
                storeTagRepository.save(st);
            }
        }
        storeTagRepository.flush();

        notificationDispatcherService.dispatchToAdmins(
                "Cơ sở mới đăng ký",
                "Quán \"" + store.getName() + "\" vừa cập nhật lại hồ sơ đăng ký và đang chờ duyệt lại.",
                NotificationType.STORE_REGISTRATION_SUBMITTED,
                store.getStoreId().toString(),
                "/admin/stores?status=PENDING"
        );

        return toStoreRegistrationStatusResponse(store);
    }

    @Override
    @Transactional(readOnly = true)
    public StoreRegistrationStatusResponse getStoreRegistrationStatus(UUID storeId) {
        storeStaffService.requireStoreAccess(storeId, "OWNER", "MANAGER");
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new AppException(ErrorCode.STORE_NOT_FOUND));
        return toStoreRegistrationStatusResponse(store);
    }

    @Override
    @Transactional(readOnly = true)
    public StoreRegistrationStatusResponse getMyRegistration() {
        User currentUser = currentUserService.getCurrentUser();
        return storeStaffRepository.findFirstByUserUserIdAndStoreRoleNameOrderByJoinedAtDesc(currentUser.getUserId(), "OWNER")
                .map(staff -> toStoreRegistrationStatusResponse(staff.getStore()))
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public MerchantDashboardResponse getMerchantDashboard(UUID storeId) {
        storeStaffService.requireStoreAccess(storeId, "OWNER", "MANAGER");
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new AppException(ErrorCode.STORE_NOT_FOUND));

        String coverImageUrl = resolveStorePrimaryImage(storeId);

        MerchantStoreSummary summary = MerchantStoreSummary.builder()
                .storeId(store.getStoreId())
                .name(store.getName())
                .address(store.getAddress())
                .status(store.getStatus())
                .coverImageUrl(coverImageUrl)
                .latitude(store.getLatitude())
                .longitude(store.getLongitude())
                .build();

        long totalSnaps = visitVerificationRepository.countByStoreStoreId(storeId);
        long totalReviews = storeReviewRepository.countByStoreStoreId(storeId);

        List<Object[]> reviewSummary = storeReviewRepository.getReviewSummaryByStoreId(storeId);
        double avgRating = 0.0;
        if (reviewSummary != null && !reviewSummary.isEmpty() && reviewSummary.get(0) != null && reviewSummary.get(0).length > 0 && reviewSummary.get(0)[0] != null) {
            avgRating = Math.round(((Number) reviewSummary.get(0)[0]).doubleValue() * 10.0) / 10.0;
        }

        List<StoreTag> storeTags = storeTagRepository.findAllByStoreId(storeId);
        long activeTagsCount = (storeTags != null)
                ? storeTags.stream().filter(st -> StoreTagStatus.APPROVED.equals(st.getStatus())).count()
                : 0L;

        MerchantMetrics metrics = MerchantMetrics.builder()
                .totalSnaps(totalSnaps)
                .totalReviews(totalReviews)
                .averageRating(avgRating)
                .activeTagsCount(activeTagsCount)
                .build();

        List<VisitVerification> snaps = visitVerificationRepository.findTop5ByStoreStoreIdOrderByCreatedAtDesc(storeId);
        List<MerchantRecentSnap> recentSnaps = (snaps != null) ? snaps.stream()
                .limit(5)
                .map(v -> MerchantRecentSnap.builder()
                        .visitVerificationId(v.getVisitVerificationId())
                        .imageUrl(v.getImageUrl())
                        .userId(v.getUser() != null ? v.getUser().getUserId() : null)
                        .userFullName(v.getUser() != null ? v.getUser().getFullName() : null)
                        .userAvatarUrl(v.getUser() != null ? v.getUser().getAvatarUrl() : null)
                        .capturedAt(v.getCapturedAt())
                        .distanceFromStoreMeters(v.getDistanceFromStoreMeters())
                        .build())
                .toList() : List.of();

        List<StoreReview> reviews = storeReviewRepository.findTop5ByStoreStoreIdOrderByCreatedAtDesc(storeId);
        List<MerchantRecentReview> recentReviews = (reviews != null) ? reviews.stream()
                .limit(5)
                .map(r -> MerchantRecentReview.builder()
                        .reviewId(r.getReviewId())
                        .userId(r.getUser() != null ? r.getUser().getUserId() : null)
                        .userFullName(r.getUser() != null ? r.getUser().getFullName() : null)
                        .userAvatarUrl(r.getUser() != null ? r.getUser().getAvatarUrl() : null)
                        .overallRating(r.getOverallRating())
                        .content(r.getContent())
                        .imageUrls(r.getImages() != null ? r.getImages().stream().map(ReviewImage::getImageUrl).toList() : List.of())
                        .merchantReply(r.getMerchantReply())
                        .replyAt(r.getReplyAt())
                        .createdAt(r.getCreatedAt())
                        .build())
                .toList() : List.of();

        List<StoreTagResponse> tagResponses = (storeTags != null) ? storeTags.stream()
                .map(storeTagMapper::toResponse)
                .toList() : List.of();

        return MerchantDashboardResponse.builder()
                .store(summary)
                .metrics(metrics)
                .recentSnaps(recentSnaps)
                .recentReviews(recentReviews)
                .tags(tagResponses)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public StoreResponse getStoreById(UUID storeId) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new AppException(ErrorCode.STORE_NOT_FOUND));
        return toStoreDetailResponse(store);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StoreResponse> getAllStores(String status) {
        List<Store> stores;
        if (status != null && !status.isBlank()) {
            try {
                StoreStatus storeStatus = StoreStatus.valueOf(status.trim().toUpperCase());
                stores = storeRepository.findAllByStatus(storeStatus);
            } catch (IllegalArgumentException e) {
                stores = List.of();
            }
        } else {
            stores = storeRepository.findAll();
        }
        return stores.stream()
                .map(this::toStoreResponse)
                .toList();
    }

    @Override
    @Transactional
    public StoreResponse updateStore(UUID storeId, UpdateStoreRequest request) {
        storeStaffService.requireStoreAccess(storeId, "OWNER", "MANAGER");

        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new AppException(ErrorCode.STORE_NOT_FOUND));

        storeMapper.updateEntity(request, store);

        store = storeRepository.save(store);

        // Update images if provided
        if (request.getImageUrls() != null && !request.getImageUrls().isEmpty()) {
            List<StoreImage> oldImages = storeImageRepository.findAllByStoreStoreId(storeId);
            storeImageRepository.deleteAll(oldImages);
            storeImageRepository.flush();

            boolean first = true;
            for (String url : request.getImageUrls()) {
                if (url != null && !url.isBlank()) {
                    StoreImage img = StoreImage.builder()
                            .store(store)
                            .imageUrl(url.trim())
                            .primary(first)
                            .build();
                    storeImageRepository.save(img);
                    first = false;
                }
            }
            storeImageRepository.flush();
        }

        return toStoreResponse(store);
    }

    @Override
    @Transactional
    public StoreResponse changeStoreStatus(UUID storeId, UpdateStoreStatusRequest request) {
        currentUserService.requireSystemAdmin();

        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new AppException(ErrorCode.STORE_NOT_FOUND));

        store.setStatus(request.getStatus());
        if (request.getStatus() == StoreStatus.REJECTED || request.getStatus() == StoreStatus.INACTIVE) {
            if (request.getRejectReason() != null && !request.getRejectReason().isBlank()) {
                store.setRejectReason(request.getRejectReason());
            }
            if (request.getAllowResubmit() != null) {
                store.setAllowResubmit(request.getAllowResubmit());
            } else if (request.getStatus() == StoreStatus.REJECTED) {
                store.setAllowResubmit(true);
            }
        } else if (request.getStatus() == StoreStatus.ACTIVE) {
            store.setRejectReason(null);
            store.setAllowResubmit(true);

            // Auto-approve OWNER_CUSTOM tags when store is approved
            List<StoreTag> storeTags = storeTagRepository.findAllByStoreId(storeId);
            for (StoreTag st : storeTags) {
                if (st.getTag() != null && st.getTag().getCategory() != null) {
                    TagCategory cat = st.getTag().getCategory();
                    if (ApprovalMode.OWNER_CUSTOM.equals(cat.getApprovalMode()) && StoreTagStatus.PENDING.equals(st.getStatus())) {
                        st.setStatus(StoreTagStatus.APPROVED);
                        st.setApprovedAt(Instant.now());
                        storeTagRepository.save(st);
                    }
                }
            }

            // Enforce requirement: All tags of the store must be reviewed before activating the store
            boolean hasPendingTags = storeTags.stream()
                    .anyMatch(st -> StoreTagStatus.PENDING.equals(st.getStatus()));
            if (hasPendingTags) {
                throw new AppException(ErrorCode.BAD_REQUEST,
                        "Không thể kích hoạt quán: Vui lòng duyệt hoặc từ chối tất cả các thẻ (vibe/tiện ích) của quán trước.");
            }
        }
        store = storeRepository.save(store);

        List<StoreStaff> ownerStaffs = storeStaffRepository.findAllByStoreStoreIdAndStoreRoleName(storeId, "OWNER");
        List<UUID> ownerUserIds = ownerStaffs.stream()
                .map(staff -> staff.getUser().getUserId())
                .toList();

        if (!ownerUserIds.isEmpty()) {
            String title = "Kết quả thẩm định quán";
            String message;
            if (request.getStatus() == StoreStatus.ACTIVE) {
                message = "Chúc mừng! Quán \"" + store.getName() + "\" của bạn đã được phê duyệt và đưa vào hoạt động.";
            } else if (request.getStatus() == StoreStatus.REJECTED) {
                message = "Hồ sơ quán \"" + store.getName() + "\" đã bị từ chối."
                        + (store.getRejectReason() != null ? " Lý do: " + store.getRejectReason() : "");
            } else {
                message = "Trạng thái quán \"" + store.getName() + "\" đã được cập nhật thành: " + request.getStatus();
            }

            notificationDispatcherService.dispatchToUsers(
                    ownerUserIds,
                    title,
                    message,
                    NotificationType.STORE_STATUS_UPDATED,
                    store.getStoreId().toString(),
                    "/merchant/stores"
            );
        }

        return toStoreResponse(store);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<StoreSearchItemResponse> searchStores(StoreSearchRequest request) {
        int page = (request.getPage() != null && request.getPage() >= 0) ? request.getPage() : 0;
        int size = (request.getSize() != null && request.getSize() > 0) ? Math.min(request.getSize(), 50) : 15;
        String keyword = (request.getKeyword() != null && !request.getKeyword().isBlank())
                ? request.getKeyword().trim().toLowerCase()
                : null;
        Long priceFrom = request.getPriceFrom();
        Long priceTo = request.getPriceTo();
        boolean openNow = Boolean.TRUE.equals(request.getOpenNow());
        boolean highRatingOnly = Boolean.TRUE.equals(request.getHighRatingOnly());
        boolean matchPersonalGuOnly = Boolean.TRUE.equals(request.getMatchPersonalGuOnly());
        String sortBy = request.getSortBy() != null ? request.getSortBy().trim().toUpperCase() : "RECOMMENDED";

        List<Store> activeStores = storeRepository.findAllByStatus(StoreStatus.ACTIVE);
        if (activeStores.isEmpty()) {
            return PageResponse.<StoreSearchItemResponse>builder()
                    .items(List.of())
                    .page(page)
                    .size(size)
                    .totalElements(0)
                    .totalPages(0)
                    .last(true)
                    .build();
        }

        // Preload metadata in bulk
        List<StoreTag> allApprovedStoreTags = storeTagRepository.findAllByStatusOrderByCreatedAtDesc(StoreTagStatus.APPROVED);
        Map<UUID, List<StoreTag>> storeTagsMap = allApprovedStoreTags.stream()
                .collect(Collectors.groupingBy(StoreTag::getStoreId));

        // Group requested tags by category (Faceted filtering: AND across categories, OR within same category)
        Map<String, List<UUID>> requestedTagsByCategory = Collections.emptyMap();
        if (request.getTagIds() != null && !request.getTagIds().isEmpty()) {
            List<Tag> requestedTags = tagRepository.findAllById(request.getTagIds());
            requestedTagsByCategory = requestedTags.stream()
                    .collect(Collectors.groupingBy(
                            t -> (t.getCategory() != null && t.getCategory().getCode() != null)
                                    ? t.getCategory().getCode().toUpperCase()
                                    : "OTHER",
                            Collectors.mapping(Tag::getTagId, Collectors.toList())
                    ));
        }

        Map<UUID, Long> favoriteCounts = new HashMap<>();
        for (Object[] row : favoriteStoreRepository.countFavoritesGroupedByStore()) {
            if (row != null && row.length >= 2 && row[0] != null && row[1] != null) {
                favoriteCounts.put((UUID) row[0], ((Number) row[1]).longValue());
            }
        }

        Map<UUID, Double> ratingMap = new HashMap<>();
        Map<UUID, Long> reviewCountMap = new HashMap<>();
        for (Object[] row : storeReviewRepository.findOverallRatingAndCountGroupedByStore()) {
            if (row != null && row.length >= 3 && row[0] != null) {
                UUID sId = (UUID) row[0];
                double avg = row[1] != null ? ((Number) row[1]).doubleValue() : 0.0;
                long count = row[2] != null ? ((Number) row[2]).longValue() : 0L;
                ratingMap.put(sId, Math.round(avg * 10.0) / 10.0);
                reviewCountMap.put(sId, count);
            }
        }

        List<UserPreference> userPrefs = Collections.emptyList();
        try {
            User currentUser = currentUserService.getCurrentUser();
            if (currentUser != null) {
                userPrefs = userPreferenceRepository.findAllByUserId(currentUser.getUserId());
            }
        } catch (Exception ignored) {
            // Unauthenticated guest user
        }

        MatchScoreWeights weights = configurationService.getMatchScoreWeights();
        List<TagCategory> activeCategories = tagCategoryRepository.findAllByActiveTrueOrderByDisplayOrderAsc();

        List<StoreSearchItemResponse> filteredList = new ArrayList<>();
        for (Store store : activeStores) {
            UUID storeId = store.getStoreId();
            List<StoreTag> tags = storeTagsMap.getOrDefault(storeId, Collections.emptyList());

            // 1. Filter price
            if (!matchesPrice(priceFrom, priceTo, store)) {
                continue;
            }

            // 3. Filter openNow
            boolean isOpen = checkIsOpenNow(store.getOpeningTime(), store.getClosingTime());
            if (openNow && !isOpen) {
                continue;
            }

            // 4. Filter keyword (match store name, address, or tag names)
            if (keyword != null) {
                String name = store.getName() != null ? store.getName().toLowerCase() : "";
                String address = store.getAddress() != null ? store.getAddress().toLowerCase() : "";
                boolean tagMatches = tags.stream()
                        .anyMatch(st -> st.getTag() != null && st.getTag().getName() != null
                                && st.getTag().getName().toLowerCase().contains(keyword));
                if (!name.contains(keyword) && !address.contains(keyword) && !tagMatches) {
                    continue;
                }
            }

            // 5. Filter tagIds (Faceted: AND across categories, OR within same category)
            if (!requestedTagsByCategory.isEmpty()) {
                Set<UUID> storeTagIdSet = tags.stream()
                        .filter(st -> st.getTag() != null)
                        .map(st -> st.getTag().getTagId())
                        .collect(Collectors.toSet());
                boolean matchesAllCategories = requestedTagsByCategory.values().stream()
                        .allMatch(categoryTagIds -> categoryTagIds.stream().anyMatch(storeTagIdSet::contains));
                if (!matchesAllCategories) {
                    continue;
                }
            }

            Double rating = ratingMap.get(storeId);
            Long reviewCount = reviewCountMap.getOrDefault(storeId, 0L);
            Long favCount = favoriteCounts.getOrDefault(storeId, 0L);

            // 6. Filter highRatingOnly (rating >= 4.5)
            if (highRatingOnly && (rating == null || rating < 4.5)) {
                continue;
            }

            // Calculate match score
            Integer matchScore = calculateMatchScore(tags, userPrefs, rating, weights, activeCategories);

            // 7. Filter matchPersonalGuOnly (matchScore >= 85)
            if (matchPersonalGuOnly && (matchScore == null || matchScore < 85)) {
                continue;
            }

            // Resolve primary image
            String primaryImg = resolveStorePrimaryImage(storeId);

            // Extract highlight tags (up to 4 tags, prioritizing owner-configured highlight tags)
            List<StoreSearchTagItem> highlightTags = tags.stream()
                    .filter(st -> st.getTag() != null && st.isHighlighted())
                    .map(st -> StoreSearchTagItem.builder()
                            .tagId(st.getTag().getTagId())
                            .name(st.getTag().getName())
                            .categoryCode(st.getTag().getCategory() != null ? st.getTag().getCategory().getCode() : "")
                            .categoryName(st.getTag().getCategory() != null ? st.getTag().getCategory().getName() : "")
                            .scaleValue(st.getTag().getScaleValue())
                            .build())
                    .limit(4)
                    .toList();

            // Fallback: If owner has not explicitly chosen highlight tags yet, pick up to 4 tags with diverse categories
            if (highlightTags.isEmpty()) {
                Map<String, StoreTag> distinctCatTags = new LinkedHashMap<>();
                for (StoreTag st : tags) {
                    if (st.getTag() == null) continue;
                    String catCode = (st.getTag().getCategory() != null && st.getTag().getCategory().getCode() != null)
                            ? st.getTag().getCategory().getCode().toUpperCase()
                            : "OTHER";
                    distinctCatTags.putIfAbsent(catCode, st);
                    if (distinctCatTags.size() >= 4) break;
                }
                highlightTags = distinctCatTags.values().stream()
                        .map(st -> StoreSearchTagItem.builder()
                                .tagId(st.getTag().getTagId())
                                .name(st.getTag().getName())
                                .categoryCode(st.getTag().getCategory() != null ? st.getTag().getCategory().getCode() : "")
                                .categoryName(st.getTag().getCategory() != null ? st.getTag().getCategory().getName() : "")
                                .scaleValue(st.getTag().getScaleValue())
                                .build())
                        .limit(4)
                        .toList();

                if (highlightTags.size() < 4 && tags.size() > highlightTags.size()) {
                    Set<UUID> existingIds = highlightTags.stream().map(StoreSearchTagItem::getTagId).collect(Collectors.toSet());
                    List<StoreSearchTagItem> additional = tags.stream()
                            .filter(st -> st.getTag() != null && !existingIds.contains(st.getTag().getTagId()))
                            .map(st -> StoreSearchTagItem.builder()
                                    .tagId(st.getTag().getTagId())
                                    .name(st.getTag().getName())
                                    .categoryCode(st.getTag().getCategory() != null ? st.getTag().getCategory().getCode() : "")
                                    .categoryName(st.getTag().getCategory() != null ? st.getTag().getCategory().getName() : "")
                                    .scaleValue(st.getTag().getScaleValue())
                                    .build())
                            .limit(4 - highlightTags.size())
                            .toList();
                    List<StoreSearchTagItem> merged = new ArrayList<>(highlightTags);
                    merged.addAll(additional);
                    highlightTags = merged;
                }
            }

            filteredList.add(StoreSearchItemResponse.builder()
                    .storeId(storeId)
                    .name(store.getName())
                    .description(store.getDescription())
                    .address(store.getAddress())
                    .latitude(store.getLatitude())
                    .longitude(store.getLongitude())
                    .priceFrom(store.getPriceFrom())
                    .priceTo(store.getPriceTo())
                    .openingTime(store.getOpeningTime())
                    .closingTime(store.getClosingTime())
                    .isOpenNow(isOpen)
                    .overallRating(rating)
                    .reviewCount(reviewCount)
                    .favoriteCount(favCount)
                    .matchScore(matchScore)
                    .primaryImageUrl(primaryImg)
                    .highlightTags(highlightTags)
                    .createdAt(store.getCreatedAt())
                    .build());
        }

        // Sorting
        Comparator<StoreSearchItemResponse> comparator;
        switch (sortBy.toUpperCase()) {
            case "RATING_DESC":
            case "RATING":
                comparator = Comparator.comparingDouble((StoreSearchItemResponse s) -> s.getOverallRating() != null ? s.getOverallRating() : 0.0).reversed()
                        .thenComparing(Comparator.comparingLong((StoreSearchItemResponse s) -> s.getReviewCount() != null ? s.getReviewCount() : 0L).reversed());
                break;
            case "FAVORITE_DESC":
            case "POPULAR_DESC":
            case "POPULAR":
                comparator = Comparator.comparingLong((StoreSearchItemResponse s) -> s.getFavoriteCount() != null ? s.getFavoriteCount() : 0L).reversed();
                break;
            case "PRICE_ASC":
                comparator = Comparator.comparingLong((StoreSearchItemResponse s) -> s.getPriceFrom() != null ? s.getPriceFrom() : Long.MAX_VALUE)
                        .thenComparing(Comparator.comparingLong((StoreSearchItemResponse s) -> s.getPriceTo() != null ? s.getPriceTo() : Long.MAX_VALUE));
                break;
            case "PRICE_DESC":
                comparator = Comparator.comparingLong((StoreSearchItemResponse s) -> s.getPriceTo() != null ? s.getPriceTo() : 0L).reversed()
                        .thenComparing(Comparator.comparingLong((StoreSearchItemResponse s) -> s.getPriceFrom() != null ? s.getPriceFrom() : 0L).reversed());
                break;
            case "NEWEST":
                comparator = Comparator.comparing((StoreSearchItemResponse s) -> s.getCreatedAt() != null ? s.getCreatedAt() : Instant.MIN).reversed()
                        .thenComparing(StoreSearchItemResponse::getName);
                break;
            case "RECOMMENDED":
            default:
                comparator = Comparator.comparing((StoreSearchItemResponse s) -> s.getMatchScore() != null ? s.getMatchScore() : -1).reversed()
                        .thenComparing(Comparator.comparingDouble((StoreSearchItemResponse s) -> s.getOverallRating() != null ? s.getOverallRating() : 0.0).reversed())
                        .thenComparing(Comparator.comparingLong((StoreSearchItemResponse s) -> s.getFavoriteCount() != null ? s.getFavoriteCount() : 0L).reversed());
                break;
        }
        filteredList.sort(comparator);

        // Pagination
        int totalElements = filteredList.size();
        int totalPages = (int) Math.ceil((double) totalElements / size);
        int start = Math.min(page * size, totalElements);
        int end = Math.min(start + size, totalElements);
        List<StoreSearchItemResponse> pageItems = filteredList.subList(start, end);
        boolean isLast = (page + 1) >= totalPages || totalPages == 0;

        return PageResponse.<StoreSearchItemResponse>builder()
                .items(pageItems)
                .page(page)
                .size(size)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .last(isLast)
                .build();
    }

    private Integer calculateMatchScore(
            List<StoreTag> storeTags,
            List<UserPreference> userPrefs,
            Double overallRating,
            MatchScoreWeights weights,
            List<TagCategory> activeCategories
    ) {
        if (userPrefs == null || userPrefs.isEmpty()) {
            return null;
        }

        // Group active user preferences by TagCategory ID
        Map<UUID, List<UserPreference>> prefsByCategory = new HashMap<>();
        for (UserPreference up : userPrefs) {
            if (up.isSkipped()) continue;
            UUID catId = null;
            if (up.getTag() != null && up.getTag().getCategory() != null) {
                catId = up.getTag().getCategory().getTagCategoryId();
            } else if (up.getQuestion() != null && up.getQuestion().getTagCategory() != null) {
                catId = up.getQuestion().getTagCategory().getTagCategoryId();
            }
            if (catId != null) {
                prefsByCategory.computeIfAbsent(catId, k -> new ArrayList<>()).add(up);
            }
        }

        // Group store tags by TagCategory ID
        Map<UUID, List<StoreTag>> storeTagsByCategory = new HashMap<>();
        for (StoreTag st : storeTags) {
            if (st.getTag() != null && st.getTag().getCategory() != null) {
                UUID catId = st.getTag().getCategory().getTagCategoryId();
                storeTagsByCategory.computeIfAbsent(catId, k -> new ArrayList<>()).add(st);
            }
        }

        double totalWeightedScore = 0.0;
        double totalCategoryWeight = 0.0;

        List<TagCategory> categoriesToEvaluate = activeCategories != null ? activeCategories : Collections.emptyList();
        for (TagCategory cat : categoriesToEvaluate) {
            UUID catId = cat.getTagCategoryId();
            double weight = cat.getWeight() != null ? cat.getWeight() : 1.0;
            if (weight <= 0) continue;

            List<UserPreference> catPrefs = prefsByCategory.getOrDefault(catId, Collections.emptyList());
            List<StoreTag> catStoreTags = storeTagsByCategory.getOrDefault(catId, Collections.emptyList());

            if (ControlType.SLIDER.equals(cat.getControlType())) {
                Integer userVal = null;
                for (UserPreference up : catPrefs) {
                    if (up.getNumericValue() != null) {
                        userVal = up.getNumericValue();
                        break;
                    } else if (up.getTag() != null && up.getTag().getScaleValue() != null) {
                        userVal = up.getTag().getScaleValue();
                        break;
                    }
                }

                Integer storeVal = null;
                for (StoreTag st : catStoreTags) {
                    if (st.getTag() != null && st.getTag().getScaleValue() != null) {
                        storeVal = st.getTag().getScaleValue();
                        break;
                    }
                }

                double score = 1.0;
                if (userVal != null && storeVal != null) {
                    int diff = Math.abs(userVal - storeVal);
                    score = Math.max(0.0, 1.0 - (diff * 0.25));
                }

                totalWeightedScore += score * weight;
                totalCategoryWeight += weight;
            } else {
                Set<UUID> userTagIds = catPrefs.stream()
                        .filter(up -> up.getTag() != null)
                        .map(up -> up.getTag().getTagId())
                        .collect(Collectors.toSet());

                Set<UUID> storeTagIds = catStoreTags.stream()
                        .filter(st -> st.getTag() != null)
                        .map(st -> st.getTag().getTagId())
                        .collect(Collectors.toSet());

                double score = 1.0;
                if (!userTagIds.isEmpty()) {
                    long matchedCount = storeTagIds.stream().filter(userTagIds::contains).count();
                    score = (double) matchedCount / userTagIds.size();
                }

                totalWeightedScore += score * weight;
                totalCategoryWeight += weight;
            }
        }

        double ratingScore = (overallRating != null && overallRating > 0) ? Math.min(1.0, overallRating / 5.0) : 0.7;
        double ratingRatio = (weights != null && weights.getRatingWeight() > 0) ? weights.getRatingWeight() : 0.10;
        double ratingWeightEffective = (totalCategoryWeight > 0 ? totalCategoryWeight : 1.0) * ratingRatio;

        if (totalCategoryWeight + ratingWeightEffective <= 0) {
            return (int) Math.round(ratingScore * 100.0);
        }

        double finalScoreRatio = (totalWeightedScore + (ratingScore * ratingWeightEffective)) / (totalCategoryWeight + ratingWeightEffective);
        int result = (int) Math.round(finalScoreRatio * 100.0);
        return Math.min(100, Math.max(0, result));
    }

    private static final ZoneId STORE_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private boolean checkIsOpenNow(LocalTime open, LocalTime close) {
        if (open == null || close == null) return true;
        LocalTime now = LocalTime.now(STORE_ZONE);
        if (close.isAfter(open)) {
            return !now.isBefore(open) && !now.isAfter(close);
        } else {
            return !now.isBefore(open) || !now.isAfter(close);
        }
    }

    private String resolveStorePrimaryImage(UUID storeId) {
        return storeImageRepository.findByStoreStoreIdAndPrimaryTrue(storeId)
                .map(StoreImage::getImageUrl)
                .orElseGet(() -> storeImageRepository.findAllByStoreStoreId(storeId).stream()
                        .findFirst()
                        .map(StoreImage::getImageUrl)
                        .orElse("https://images.unsplash.com/photo-1501339847302-ac426a4a7cbb?q=80&w=800&auto=format&fit=crop"));
    }


    private StoreResponse toStoreResponse(Store store) {
        StoreResponse response = storeMapper.toResponse(store);

        List<StoreImageResponse> images = storeImageRepository.findAllByStoreStoreId(store.getStoreId())
                .stream()
                .map(storeImageMapper::toResponse)
                .toList();
        response.setImages(images);

        List<StoreTag> storeTags;
        if (StoreStatus.PENDING.equals(store.getStatus())) {
            storeTags = storeTagRepository.findAllByStoreId(store.getStoreId());
        } else {
            storeTags = storeTagRepository.findAllByStoreIdAndStatus(store.getStoreId(), StoreTagStatus.APPROVED);
        }
        List<StoreTagResponse> tags = storeTags.stream()
                .map(storeTagMapper::toResponse)
                .toList();
        response.setTags(tags);

        return response;
    }

    private StoreRegistrationStatusResponse toStoreRegistrationStatusResponse(Store store) {
        List<StoreImageResponse> images = storeImageRepository.findAllByStoreStoreId(store.getStoreId())
                .stream()
                .map(storeImageMapper::toResponse)
                .toList();

        List<StoreTagResponse> tags = storeTagRepository.findAllByStoreId(store.getStoreId())
                .stream()
                .map(storeTagMapper::toResponse)
                .toList();

        return StoreRegistrationStatusResponse.builder()
                .storeId(store.getStoreId())
                .name(store.getName())
                .description(store.getDescription())
                .address(store.getAddress())
                .latitude(store.getLatitude())
                .longitude(store.getLongitude())
                .openingTime(store.getOpeningTime())
                .closingTime(store.getClosingTime())
                .priceFrom(store.getPriceFrom())
                .priceTo(store.getPriceTo())
                .phone(store.getPhone())
                .email(store.getEmail())
                .status(store.getStatus())
                .rejectReason(store.getRejectReason())
                .allowResubmit(store.isAllowResubmit())
                .images(images)
                .tags(tags)
                .createdAt(store.getCreatedAt())
                .updatedAt(store.getUpdatedAt())
                .build();
    }

    private StoreResponse toStoreDetailResponse(Store store) {
        StoreResponse response = toStoreResponse(store);
        UUID storeId = store.getStoreId();

        // 1. Sync live tag scores if available
        List<Object[]> tagStats = tagRatingRepository.getAllTagRatingSummariesForStore(storeId);
        if (tagStats != null && !tagStats.isEmpty()) {
            Map<UUID, Object[]> tagScoreMap = new HashMap<>();
            for (Object[] row : tagStats) {
                if (row != null && row.length >= 3 && row[0] != null) {
                    tagScoreMap.put((UUID) row[0], row);
                }
            }

            if (response.getTags() != null) {
                for (StoreTagResponse tagResp : response.getTags()) {
                    if (tagResp != null && tagScoreMap.containsKey(tagResp.getTagId())) {
                        Object[] s = tagScoreMap.get(tagResp.getTagId());
                        tagResp.setAverageScore(Math.round(((Number) s[1]).doubleValue() * 10.0) / 10.0);
                        tagResp.setReviewCount(((Number) s[2]).intValue());
                    }
                }
            }
        }

        // 2. Load reviews for this store
        List<StoreReview> reviews = storeReviewRepository.findAllByStoreStoreIdOrderByCreatedAtDesc(storeId);
        List<StoreReviewResponse> reviewResponses = (reviews != null)
                ? reviews.stream().map(storeReviewMapper::toResponse).toList()
                : Collections.emptyList();
        response.setReviews(reviewResponses);

        // 3. Load review summary and root ratings
        List<Object[]> summary = storeReviewRepository.getReviewSummaryByStoreId(storeId);
        if (summary != null && !summary.isEmpty() && summary.get(0)[5] != null && ((Number) summary.get(0)[5]).longValue() > 0) {
            Object[] row = summary.get(0);
            double avg = row[0] != null ? Math.round(((Number) row[0]).doubleValue() * 10.0) / 10.0 : 0.0;
            long count = ((Number) row[5]).longValue();
            StoreReviewSummaryResponse summaryResponse = StoreReviewSummaryResponse.builder()
                    .storeId(storeId)
                    .averageRating(avg)
                    .averageQuietness(row[1] != null ? Math.round(((Number) row[1]).doubleValue() * 10.0) / 10.0 : null)
                    .averageLighting(row[2] != null ? Math.round(((Number) row[2]).doubleValue() * 10.0) / 10.0 : null)
                    .averageSeating(row[3] != null ? Math.round(((Number) row[3]).doubleValue() * 10.0) / 10.0 : null)
                    .averageOutlet(row[4] != null ? Math.round(((Number) row[4]).doubleValue() * 10.0) / 10.0 : null)
                    .totalReviews(count)
                    .build();
            response.setReviewSummary(summaryResponse);
            response.setOverallRating(avg);
            response.setReviewCount(count);
        } else {
            response.setReviewSummary(StoreReviewSummaryResponse.builder()
                    .storeId(storeId)
                    .averageRating(0.0)
                    .totalReviews(0L)
                    .averageQuietness(0.0)
                    .averageLighting(0.0)
                    .averageSeating(0.0)
                    .averageOutlet(0.0)
                    .build());
            response.setOverallRating(0.0);
            response.setReviewCount(0L);
        }

        // 4. Resolve current user review status (Duplicate review prevention / My review status)
        boolean hasReviewed = false;
        StoreReviewResponse userReview = null;

        if (currentUserService.isAuthenticated()) {
            try {
                UUID currentUserId = currentUserService.getCurrentUserId();
                if (currentUserId != null) {
                    Optional<StoreReview> userReviewOpt = storeReviewRepository.findFirstByStoreStoreIdAndUserUserIdOrderByCreatedAtDesc(storeId, currentUserId);
                    if (userReviewOpt.isPresent()) {
                        hasReviewed = true;
                        userReview = storeReviewMapper.toResponse(userReviewOpt.get());
                    }
                }
            } catch (Exception ignored) {
                // Anonymous or unauthenticated request
            }
        }

        response.setHasReviewed(hasReviewed);
        response.setUserReview(userReview);

        return response;
    }

    private boolean matchesPrice(Long filterFrom, Long filterTo, Store store) {
        // If neither filter is provided, accept all stores
        if (filterFrom == null && filterTo == null) {
            return true;
        }

        Long storeFrom = store.getPriceFrom();
        Long storeTo = store.getPriceTo();

        // If store has no price info, cannot match filter
        if (storeFrom == null && storeTo == null) {
            return false;
        }

        long sMin = storeFrom != null ? storeFrom : (storeTo != null ? storeTo : 0L);
        long sMax = storeTo != null ? storeTo : (storeFrom != null ? storeFrom : Long.MAX_VALUE);

        if (filterFrom != null && sMax < filterFrom) {
            return false;
        }
        if (filterTo != null && sMin > filterTo) {
            return false;
        }

        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public List<FeaturedMoodStoreResponse> getFeaturedMoodStores() {
        List<TagCategory> activeCategories = tagCategoryRepository.findAllByActiveTrueOrderByDisplayOrderAsc();
        if (activeCategories.isEmpty()) {
            return Collections.emptyList();
        }

        // 1. Thống kê lượt quan tâm từ sở thích Onboarding người dùng (user_preferences)
        Map<UUID, Long> preferenceCounts = new HashMap<>();
        for (Object[] row : userPreferenceRepository.countPreferencesGroupedByTag()) {
            if (row != null && row.length >= 2 && row[0] != null && row[1] != null) {
                preferenceCounts.put((UUID) row[0], ((Number) row[1]).longValue());
            }
        }

        // 2. Thống kê số lượng quán sở hữu từng tag (cho tiêu chí tie-break 2A)
        Map<UUID, Long> storeCounts = new HashMap<>();
        for (Object[] row : storeTagRepository.countDistinctStoresGroupedByTag(StoreTagStatus.APPROVED)) {
            if (row != null && row.length >= 2 && row[0] != null && row[1] != null) {
                storeCounts.put((UUID) row[0], ((Number) row[1]).longValue());
            }
        }

        // 3. Toàn bộ approved tags của từng quán để tra cứu chính xác
        List<StoreTag> allApprovedStoreTags = storeTagRepository.findAllByStatusOrderByCreatedAtDesc(StoreTagStatus.APPROVED);
        Map<UUID, List<StoreTag>> storeTagsMap = allApprovedStoreTags.stream()
                .collect(Collectors.groupingBy(StoreTag::getStoreId));

        // 4. Lấy danh sách quán active đã được tính toán đầy đủ rating, reviews, favs, images
        StoreSearchRequest searchReq = StoreSearchRequest.builder()
                .page(0)
                .size(100)
                .build();
        PageResponse<StoreSearchItemResponse> pageResp = searchStores(searchReq);
        List<StoreSearchItemResponse> allStores = (pageResp != null && pageResp.getItems() != null)
                ? new ArrayList<>(pageResp.getItems())
                : Collections.emptyList();

        if (allStores.isEmpty()) {
            return Collections.emptyList();
        }

        // Comparator sắp xếp quán theo thứ bậc chặt chẽ:
        // 1. overallRating DESC -> 2. reviewCount DESC -> 3. favoriteCount DESC -> 4. Tên quán A-Z -> 5. storeId
        Comparator<StoreSearchItemResponse> storeComparator = (a, b) -> {
            double ratingA = a.getOverallRating() != null ? a.getOverallRating() : 0.0;
            double ratingB = b.getOverallRating() != null ? b.getOverallRating() : 0.0;
            if (Double.compare(ratingB, ratingA) != 0) {
                return Double.compare(ratingB, ratingA);
            }

            long reviewsA = a.getReviewCount() != null ? a.getReviewCount() : 0L;
            long reviewsB = b.getReviewCount() != null ? b.getReviewCount() : 0L;
            if (Long.compare(reviewsB, reviewsA) != 0) {
                return Long.compare(reviewsB, reviewsA);
            }

            long favsA = a.getFavoriteCount() != null ? a.getFavoriteCount() : 0L;
            long favsB = b.getFavoriteCount() != null ? b.getFavoriteCount() : 0L;
            if (Long.compare(favsB, favsA) != 0) {
                return Long.compare(favsB, favsA);
            }

            String nameA = a.getName() != null ? a.getName().toLowerCase() : "";
            String nameB = b.getName() != null ? b.getName().toLowerCase() : "";
            int nameCompare = nameA.compareTo(nameB);
            if (nameCompare != 0) {
                return nameCompare;
            }

            return a.getStoreId().compareTo(b.getStoreId());
        };

        Set<UUID> usedStoreIds = new HashSet<>();
        List<FeaturedMoodStoreResponse> result = new ArrayList<>();

        for (TagCategory category : activeCategories) {
            // 5a. Lấy toàn bộ tags active thuộc category này
            List<Tag> categoryTags = new ArrayList<>(
                    tagRepository.findAllByCategoryTagCategoryIdAndActiveTrue(category.getTagCategoryId())
            );

            // 5b. Sắp xếp tags tìm tag Onboarding phổ biến nhất:
            // 1. Số lượt chọn trong user_preferences DESC
            // 2. Tiêu chí 2A: Số quán sở hữu tag DESC
            // 3. Tên tag A-Z
            // 4. Ngày tạo ASC
            categoryTags.sort((t1, t2) -> {
                long pref1 = preferenceCounts.getOrDefault(t1.getTagId(), 0L);
                long pref2 = preferenceCounts.getOrDefault(t2.getTagId(), 0L);
                if (Long.compare(pref2, pref1) != 0) {
                    return Long.compare(pref2, pref1);
                }

                long stores1 = storeCounts.getOrDefault(t1.getTagId(), 0L);
                long stores2 = storeCounts.getOrDefault(t2.getTagId(), 0L);
                if (Long.compare(stores2, stores1) != 0) {
                    return Long.compare(stores2, stores1);
                }

                String name1 = t1.getName() != null ? t1.getName().toLowerCase() : "";
                String name2 = t2.getName() != null ? t2.getName().toLowerCase() : "";
                int nameCompare = name1.compareTo(name2);
                if (nameCompare != 0) {
                    return nameCompare;
                }

                Instant c1 = t1.getCreatedAt() != null ? t1.getCreatedAt() : Instant.EPOCH;
                Instant c2 = t2.getCreatedAt() != null ? t2.getCreatedAt() : Instant.EPOCH;
                return c1.compareTo(c2);
            });

            Tag topTag = categoryTags.isEmpty() ? null : categoryTags.get(0);
            long topTagPrefCount = topTag != null ? preferenceCounts.getOrDefault(topTag.getTagId(), 0L) : 0L;

            StoreSearchItemResponse selectedStore = null;
            String moodBadgeText = topTag != null ? cleanMoodBadgeText(topTag.getName()) : category.getName();

            // Ưu tiên 1: Tìm quán chưa chọn chứa đúng topTag phổ biến nhất
            if (topTag != null) {
                UUID topTagId = topTag.getTagId();
                List<StoreSearchItemResponse> exactMatches = allStores.stream()
                        .filter(s -> !usedStoreIds.contains(s.getStoreId()))
                        .filter(s -> {
                            List<StoreTag> sTags = storeTagsMap.getOrDefault(s.getStoreId(), Collections.emptyList());
                            return sTags.stream().anyMatch(st -> st.getTag() != null && topTagId.equals(st.getTag().getTagId()));
                        })
                        .sorted(storeComparator)
                        .toList();

                if (!exactMatches.isEmpty()) {
                    selectedStore = exactMatches.get(0);
                    moodBadgeText = cleanMoodBadgeText(topTag.getName());
                }
            }

            // Ưu tiên 2: Tìm quán chưa chọn chứa BẤT KỲ tag nào thuộc category này
            if (selectedStore == null) {
                UUID catId = category.getTagCategoryId();
                List<StoreSearchItemResponse> categoryMatches = allStores.stream()
                        .filter(s -> !usedStoreIds.contains(s.getStoreId()))
                        .filter(s -> {
                            List<StoreTag> sTags = storeTagsMap.getOrDefault(s.getStoreId(), Collections.emptyList());
                            return sTags.stream().anyMatch(st -> st.getTag() != null && st.getTag().getCategory() != null
                                    && catId.equals(st.getTag().getCategory().getTagCategoryId()));
                        })
                        .sorted(storeComparator)
                        .toList();

                if (!categoryMatches.isEmpty()) {
                    selectedStore = categoryMatches.get(0);
                    List<StoreTag> sTags = storeTagsMap.getOrDefault(selectedStore.getStoreId(), Collections.emptyList());
                    moodBadgeText = sTags.stream()
                            .filter(st -> st.getTag() != null && st.getTag().getCategory() != null
                                    && catId.equals(st.getTag().getCategory().getTagCategoryId()))
                            .map(st -> cleanMoodBadgeText(st.getTag().getName()))
                            .findFirst()
                            .orElse(moodBadgeText);
                }
            }

            // TH cuối (Fallback): Lấy quán chưa chọn có rating cao nhất toàn hệ thống
            if (selectedStore == null) {
                List<StoreSearchItemResponse> remaining = allStores.stream()
                        .filter(s -> !usedStoreIds.contains(s.getStoreId()))
                        .sorted(storeComparator)
                        .toList();

                if (!remaining.isEmpty()) {
                    selectedStore = remaining.get(0);
                } else {
                    // Trường hợp cực đoan: Số lượng quán ít hơn số category
                    int fallbackIndex = result.size() % allStores.size();
                    selectedStore = allStores.get(fallbackIndex);
                }
            }

            usedStoreIds.add(selectedStore.getStoreId());
            result.add(FeaturedMoodStoreResponse.builder()
                    .categoryCode(category.getCode())
                    .categoryName(category.getName())
                    .moodBadgeText(cleanMoodBadgeText(moodBadgeText))
                    .preferenceCount(topTagPrefCount)
                    .store(selectedStore)
                    .build());
        }

        return result;
    }

    private String cleanMoodBadgeText(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.replaceAll("^#+", "")
                .replaceAll("\\s*\\([^)]*\\)", "")
                .trim();
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getActiveDistricts() {
        return Collections.emptyList();
    }
}
