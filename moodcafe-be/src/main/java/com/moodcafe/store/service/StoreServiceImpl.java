package com.moodcafe.store.service;

import com.moodcafe.auth.abstraction.service.CurrentUserService;
import com.moodcafe.auth.entity.User;
import com.moodcafe.configuration.abstraction.service.SystemConfigurationService;
import com.moodcafe.configuration.dto.response.MatchScoreWeights;
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
import com.moodcafe.tag.dto.response.StoreTagResponse;
import com.moodcafe.tag.entity.StoreTag;
import com.moodcafe.tag.entity.Tag;
import com.moodcafe.tag.entity.TagCategory;
import com.moodcafe.tag.entity.UserPreference;
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

    @Override
    @Transactional
    public StoreResponse createStore(CreateStoreRequest request) {
        User currentUser = currentUserService.getCurrentUser();

        Store store = storeMapper.toEntity(request);
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

        return toStoreResponse(store);
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

        return toStoreResponse(store);
    }

    @Override
    @Transactional
    public StoreResponse changeStoreStatus(UUID storeId, UpdateStoreStatusRequest request) {
        currentUserService.requireSystemAdmin();

        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new AppException(ErrorCode.STORE_NOT_FOUND));

        store.setStatus(request.getStatus());
        store = storeRepository.save(store);

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
        List<String> targetDistricts = new ArrayList<>();
        if (request.getDistricts() != null && !request.getDistricts().isEmpty()) {
            for (String d : request.getDistricts()) {
                if (d != null && !d.isBlank() && !d.equalsIgnoreCase("all") && !d.equalsIgnoreCase("tất cả khu vực")) {
                    targetDistricts.add(d.trim().toLowerCase());
                }
            }
        }
        if (request.getDistrict() != null && !request.getDistrict().isBlank() && !request.getDistrict().equalsIgnoreCase("all") && !request.getDistrict().equalsIgnoreCase("tất cả khu vực")) {
            String singleD = request.getDistrict().trim().toLowerCase();
            if (!targetDistricts.contains(singleD)) {
                targetDistricts.add(singleD);
            }
        }
        String priceRange = (request.getPriceRange() != null && !request.getPriceRange().isBlank())
                ? request.getPriceRange().trim()
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

        List<StoreSearchItemResponse> filteredList = new ArrayList<>();
        for (Store store : activeStores) {
            UUID storeId = store.getStoreId();
            List<StoreTag> tags = storeTagsMap.getOrDefault(storeId, Collections.emptyList());

            // 1. Filter district (if not specified or "all", match all districts)
            if (!targetDistricts.isEmpty()) {
                String storeDist = store.getDistrict() != null ? store.getDistrict().toLowerCase() : "";
                String address = store.getAddress() != null ? store.getAddress().toLowerCase() : "";
                String extracted = extractDistrictFromAddress(store.getAddress()).toLowerCase();
                boolean matchesAny = targetDistricts.stream().anyMatch(
                        d -> storeDist.contains(d) || address.contains(d) || extracted.contains(d)
                );
                if (!matchesAny) {
                    continue;
                }
            }

            // 2. Filter price (supports exact numeric range [priceFrom, priceTo] and presets)
            if (!matchesPrice(priceFrom, priceTo, priceRange, store)) {
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

            // 5b. Filter noiseLevel (1 to 5)
            if (request.getNoiseLevel() != null) {
                int targetNoise = request.getNoiseLevel();
                boolean matchesNoise = tags.stream().anyMatch(st -> {
                    Tag t = st.getTag();
                    return t != null
                            && t.getCategory() != null
                            && "NOISE".equalsIgnoreCase(t.getCategory().getCode())
                            && t.getScaleValue() != null
                            && t.getScaleValue() == targetNoise;
                });
                if (!matchesNoise) {
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
            Integer matchScore = calculateMatchScore(tags, userPrefs, rating, weights);

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
                    .district(store.getDistrict() != null && !store.getDistrict().isBlank() ? store.getDistrict() : extractDistrictFromAddress(store.getAddress()))
                    .latitude(store.getLatitude())
                    .longitude(store.getLongitude())
                    .priceRange(store.getPriceRange())
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
            MatchScoreWeights weights
    ) {
        if (userPrefs == null || userPrefs.isEmpty()) {
            return null;
        }

        Set<UUID> userVibeTagIds = new HashSet<>();
        Set<UUID> userPurposeTagIds = new HashSet<>();
        Set<UUID> userAmenityTagIds = new HashSet<>();
        Integer userPreferredNoise = null;

        for (UserPreference up : userPrefs) {
            if (up.isSkipped()) continue;
            if (up.getNumericValue() != null) {
                userPreferredNoise = up.getNumericValue();
            } else if (up.getTag() != null && up.getTag().getScaleValue() != null) {
                userPreferredNoise = up.getTag().getScaleValue();
            }
            if (up.getTag() != null && up.getTag().getCategory() != null) {
                String catCode = up.getTag().getCategory().getCode();
                if ("VIBE".equalsIgnoreCase(catCode)) {
                    userVibeTagIds.add(up.getTag().getTagId());
                } else if ("PURPOSE".equalsIgnoreCase(catCode)) {
                    userPurposeTagIds.add(up.getTag().getTagId());
                } else if ("AMENITY".equalsIgnoreCase(catCode)) {
                    userAmenityTagIds.add(up.getTag().getTagId());
                }
            }
        }

        Set<UUID> storeVibeTagIds = new HashSet<>();
        Set<UUID> storePurposeTagIds = new HashSet<>();
        Set<UUID> storeAmenityTagIds = new HashSet<>();
        Integer storeNoiseLevel = null;

        for (StoreTag st : storeTags) {
            Tag t = st.getTag();
            if (t == null || t.getCategory() == null) continue;
            String catCode = t.getCategory().getCode();
            if ("VIBE".equalsIgnoreCase(catCode)) {
                storeVibeTagIds.add(t.getTagId());
            } else if ("PURPOSE".equalsIgnoreCase(catCode)) {
                storePurposeTagIds.add(t.getTagId());
            } else if ("AMENITY".equalsIgnoreCase(catCode)) {
                storeAmenityTagIds.add(t.getTagId());
            } else if ("NOISE".equalsIgnoreCase(catCode)) {
                if (t.getScaleValue() != null) {
                    storeNoiseLevel = t.getScaleValue();
                } else {
                    String name = t.getName() != null ? t.getName().toLowerCase() : "";
                    if (name.contains("yên tĩnh") && !name.contains("khá")) storeNoiseLevel = 1;
                    else if (name.contains("khá yên tĩnh")) storeNoiseLevel = 2;
                    else if (name.contains("bình thường") || name.contains("vừa phải")) storeNoiseLevel = 3;
                    else if (name.contains("khá sôi động") || name.contains("sôi động")) storeNoiseLevel = 4;
                    else if (name.contains("náo nhiệt")) storeNoiseLevel = 5;
                }
            }
        }

        double vibeScore = 1.0;
        if (!userVibeTagIds.isEmpty()) {
            long matchedVibes = storeVibeTagIds.stream().filter(userVibeTagIds::contains).count();
            vibeScore = (double) matchedVibes / userVibeTagIds.size();
        }

        double purposeScore = 1.0;
        if (!userPurposeTagIds.isEmpty()) {
            long matchedPurposes = storePurposeTagIds.stream().filter(userPurposeTagIds::contains).count();
            purposeScore = (double) matchedPurposes / userPurposeTagIds.size();
        }

        double amenityScore = 1.0;
        if (!userAmenityTagIds.isEmpty()) {
            long matchedAmenities = storeAmenityTagIds.stream().filter(userAmenityTagIds::contains).count();
            amenityScore = (double) matchedAmenities / userAmenityTagIds.size();
        }

        double noiseScore = 1.0;
        if (userPreferredNoise != null && storeNoiseLevel != null) {
            int diff = Math.abs(userPreferredNoise - storeNoiseLevel);
            noiseScore = Math.max(0.0, 1.0 - (diff * 0.25));
        }

        double ratingScore = (overallRating != null && overallRating > 0) ? Math.min(1.0, overallRating / 5.0) : 0.7;

        double totalScore = (weights.getVibeWeight() * vibeScore)
                + (weights.getPurposeWeight() * purposeScore)
                + (weights.getNoiseWeight() * noiseScore)
                + (weights.getAmenityWeight() * amenityScore)
                + (weights.getRatingWeight() * ratingScore);

        int result = (int) Math.round(totalScore * 100.0);
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

    private String extractDistrictFromAddress(String address) {
        if (address == null || address.isBlank()) return "TP.HCM";
        String lower = address.toLowerCase();
        if (lower.contains("quận 1") || lower.contains("district 1")) return "Quận 1";
        if (lower.contains("quận 2") || lower.contains("district 2") || lower.contains("thảo điền") || lower.contains("thao dien")) return "Thảo Điền, TP. Thủ Đức";
        if (lower.contains("quận 3") || lower.contains("district 3")) return "Quận 3";
        if (lower.contains("bình thạnh") || lower.contains("binh thanh")) return "Bình Thạnh";
        if (lower.contains("phú nhuận") || lower.contains("phu nhuan")) return "Phú Nhuận";
        if (lower.contains("thủ đức") || lower.contains("thu duc")) return "TP. Thủ Đức";
        if (lower.contains("tân bình") || lower.contains("tan binh")) return "Tân Bình";
        if (lower.contains("quận 7") || lower.contains("district 7")) return "Quận 7";
        return address.split(",")[0].trim();
    }

    private StoreResponse toStoreResponse(Store store) {
        StoreResponse response = storeMapper.toResponse(store);

        List<StoreImageResponse> images = storeImageRepository.findAllByStoreStoreId(store.getStoreId())
                .stream()
                .map(storeImageMapper::toResponse)
                .toList();
        response.setImages(images);

        List<StoreTagResponse> tags = storeTagRepository.findAllByStoreIdAndStatus(store.getStoreId(), StoreTagStatus.APPROVED)
                .stream()
                .map(storeTagMapper::toResponse)
                .toList();
        response.setTags(tags);

        return response;
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

        return response;
    }

    private boolean matchesPrice(Long filterFrom, Long filterTo, String legacyRange, Store store) {
        // If neither filter is provided, accept all stores
        if (filterFrom == null && filterTo == null && (legacyRange == null || legacyRange.isBlank())) {
            return true;
        }

        Long storeFrom = store.getPriceFrom();
        Long storeTo = store.getPriceTo();

        // Fallback: extract numeric bounds from store.getPriceRange() if numeric fields are not set
        if (storeFrom == null || storeTo == null) {
            long[] parsed = parsePriceRangeString(store.getPriceRange());
            if (parsed != null) {
                if (storeFrom == null) storeFrom = parsed[0];
                if (storeTo == null) storeTo = parsed[1];
            }
        }

        // 1. If numeric filter bounds (filterFrom, filterTo) are provided
        if (filterFrom != null || filterTo != null) {
            if (storeFrom == null && storeTo == null) {
                return false;
            }
            long sMin = storeFrom != null ? storeFrom : storeTo;
            long sMax = storeTo != null ? storeTo : storeFrom;

            // If store's maximum price is strictly below user's minimum budget
            if (filterFrom != null && sMax < filterFrom) {
                return false;
            }
            // If store's minimum price is strictly above user's maximum budget
            if (filterTo != null && sMin > filterTo) {
                return false;
            }
            return true;
        }

        // 2. Legacy string preset matching
        return matchesPriceRange(legacyRange, store.getPriceRange(), storeFrom, storeTo);
    }

    private boolean matchesPriceRange(String filterRange, String storePriceRange, Long storeFrom, Long storeTo) {
        if (filterRange == null || filterRange.isBlank()) return true;
        if (storePriceRange != null && filterRange.equalsIgnoreCase(storePriceRange)) return true;

        long sMin = storeFrom != null ? storeFrom : 0L;
        long sMax = storeTo != null ? storeTo : Long.MAX_VALUE;

        if (storeFrom == null || storeTo == null) {
            long[] parsed = parsePriceRangeString(storePriceRange);
            if (parsed != null) {
                sMin = parsed[0];
                sMax = parsed[1];
            } else {
                return storePriceRange != null && storePriceRange.toLowerCase().contains(filterRange.toLowerCase());
            }
        }

        switch (filterRange.toUpperCase()) {
            case "UNDER_30K":
                return sMin <= 30000;
            case "30K_50K":
                return sMin <= 50000 && sMax >= 30000;
            case "50K_80K":
                return sMin <= 80000 && sMax >= 50000;
            case "ABOVE_80K":
                return sMax >= 80000;
            default:
                return storePriceRange != null && storePriceRange.toLowerCase().contains(filterRange.toLowerCase());
        }
    }

    private long[] parsePriceRangeString(String storePriceRange) {
        if (storePriceRange == null || storePriceRange.isBlank()) return null;
        try {
            String[] parts = storePriceRange.split("-");
            long storeMin = 0;
            long storeMax = Long.MAX_VALUE;
            if (parts.length >= 1) {
                String clean0 = parts[0].replaceAll("[^0-9]", "");
                if (!clean0.isBlank()) storeMin = Long.parseLong(clean0);
            }
            if (parts.length >= 2) {
                String clean1 = parts[1].replaceAll("[^0-9]", "");
                if (!clean1.isBlank()) storeMax = Long.parseLong(clean1);
            } else {
                storeMax = storeMin;
            }
            return new long[]{storeMin, storeMax};
        } catch (Exception ignored) {
            return null;
        }
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
        List<Store> activeStores = storeRepository.findAllByStatus(StoreStatus.ACTIVE);
        Set<String> districts = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (Store s : activeStores) {
            if (s.getDistrict() != null && !s.getDistrict().isBlank()) {
                districts.add(s.getDistrict().trim());
            } else if (s.getAddress() != null && !s.getAddress().isBlank()) {
                districts.add(extractDistrictFromAddress(s.getAddress()));
            }
        }
        return new ArrayList<>(districts);
    }
}
