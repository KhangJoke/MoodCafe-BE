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
import com.moodcafe.tag.abstraction.service.TagService;
import com.moodcafe.tag.dto.request.CreateTagRequest;
import com.moodcafe.tag.dto.request.UpdateTagRequest;
import com.moodcafe.tag.dto.response.CityTrendingResponse;
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

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TagServiceImpl implements TagService {

    private final TagRepository tagRepository;
    private final TagCategoryRepository tagCategoryRepository;
    private final StoreTagRepository storeTagRepository;
    private final UserPreferenceRepository userPreferenceRepository;
    private final FavoriteStoreRepository favoriteStoreRepository;
    private final StoreRepository storeRepository;
    private final StoreReviewRepository storeReviewRepository;
    private final StoreImageRepository storeImageRepository;
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
                    .imageUrl(resolveTagImageUrl(purpose))
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

    @Override
    @Transactional(readOnly = true)
    public List<CityTrendingResponse> getCityTrendingData(int tagLimit, int storeLimit) {
        int safeTagLimit = Math.max(1, Math.min(tagLimit, 50));
        int safeStoreLimit = Math.max(1, Math.min(storeLimit, 50));

        // 1. Lấy tất cả các thẻ đang hoạt động (tất cả các loại: VIBE, PURPOSE, AMENITY...)
        List<Tag> allActiveTags = tagRepository.findAllByActiveTrue();
        if (allActiveTags.isEmpty()) {
            return List.of();
        }

        // 2. Thống kê số quán được duyệt cho từng tag
        Map<UUID, Long> storeCounts = new HashMap<>();
        for (Object[] row : storeTagRepository.countDistinctStoresGroupedByTag(StoreTagStatus.APPROVED)) {
            if (row != null && row.length >= 2 && row[0] != null && row[1] != null) {
                storeCounts.put((UUID) row[0], ((Number) row[1]).longValue());
            }
        }

        // 3. Thống kê lượt quan tâm từ sở thích người dùng (UserPreference)
        Map<UUID, Long> preferenceCounts = new HashMap<>();
        for (Object[] row : userPreferenceRepository.countPreferencesGroupedByTag()) {
            if (row != null && row.length >= 2 && row[0] != null && row[1] != null) {
                preferenceCounts.put((UUID) row[0], ((Number) row[1]).longValue());
            }
        }

        List<CityTrendingResponse> trendingList = new ArrayList<>();

        // 4. Nhóm 1: Top thẻ Tag (tất cả các loại) theo safeTagLimit
        // Xếp theo:
        //  1) lượt UserPreference giảm dần
        //  2) nếu bằng nhau: tag nhiều quán hơn (storeCounts giảm dần)
        //  3) nếu vẫn bằng nhau: theo thứ tự category.displayOrder -> createdAt -> name
        List<Tag> topTags = allActiveTags.stream()
                .sorted(Comparator
                        .comparingLong((Tag t) -> preferenceCounts.getOrDefault(t.getTagId(), 0L)).reversed()
                        .thenComparing(Comparator.comparingLong((Tag t) -> storeCounts.getOrDefault(t.getTagId(), 0L)).reversed())
                        .thenComparing(t -> t.getCategory() != null && t.getCategory().getDisplayOrder() != null ? t.getCategory().getDisplayOrder() : 99)
                        .thenComparing(t -> t.getCreatedAt() != null ? t.getCreatedAt() : Instant.EPOCH)
                        .thenComparing(t -> t.getName() != null ? t.getName() : ""))
                .limit(safeTagLimit)
                .toList();

        for (Tag tag : topTags) {
            long stores = storeCounts.getOrDefault(tag.getTagId(), 0L);
            long rawInterest = preferenceCounts.getOrDefault(tag.getTagId(), 0L);

            trendingList.add(CityTrendingResponse.builder()
                    .itemType("TAG")
                    .tagId(tag.getTagId())
                    .tagName(tag.getName())
                    .categoryCode(tag.getCategory() != null ? tag.getCategory().getCode() : "")
                    .categoryName(tag.getCategory() != null ? tag.getCategory().getName() : "Xu hướng")
                    .badgeText("Đang quan tâm")
                    .subtitle(tag.getDescription() != null && !tag.getDescription().isBlank()
                            ? tag.getDescription()
                            : "Không gian được nhiều người lựa chọn nhất")
                    .imageUrl(resolveTagImageUrl(tag))
                    .interestedCount(rawInterest)
                    .storeCount(stores)
                    .build());
        }

        // 5. Nhóm 2: Top Quán cà phê cụ thể theo safeStoreLimit
        // Xếp theo:
        //  1) lượt FavoriteStore giảm dần
        //  2) nếu bằng nhau: quán có rating tổng cao hơn
        //  3) nếu vẫn bằng nhau: theo thứ tự createdAt -> name
        List<Store> approvedStores = storeRepository.findAllByStatus(StoreStatus.ACTIVE);

        Map<UUID, Long> storeFavoriteCounts = new HashMap<>();
        for (Object[] row : favoriteStoreRepository.countFavoritesGroupedByStore()) {
            if (row != null && row.length >= 2 && row[0] != null && row[1] != null) {
                storeFavoriteCounts.put((UUID) row[0], ((Number) row[1]).longValue());
            }
        }

        Map<UUID, Double> storeRatings = new HashMap<>();
        Map<UUID, Long> storeReviewCounts = new HashMap<>();
        for (Object[] row : storeReviewRepository.findOverallRatingAndCountGroupedByStore()) {
            if (row != null && row.length >= 3 && row[0] != null) {
                UUID sId = (UUID) row[0];
                double avg = row[1] != null ? ((Number) row[1]).doubleValue() : 0.0;
                long count = row[2] != null ? ((Number) row[2]).longValue() : 0L;
                storeRatings.put(sId, Math.round(avg * 10.0) / 10.0);
                storeReviewCounts.put(sId, count);
            }
        }

        List<Store> topStores = approvedStores.stream()
                .sorted(Comparator
                        .comparingLong((Store s) -> storeFavoriteCounts.getOrDefault(s.getStoreId(), 0L)).reversed()
                        .thenComparing(Comparator.comparingDouble((Store s) -> storeRatings.getOrDefault(s.getStoreId(), 0.0)).reversed())
                        .thenComparing(s -> s.getCreatedAt() != null ? s.getCreatedAt() : Instant.EPOCH)
                        .thenComparing(s -> s.getName() != null ? s.getName() : ""))
                .limit(safeStoreLimit)
                .toList();

        for (Store store : topStores) {
            long rawFav = storeFavoriteCounts.getOrDefault(store.getStoreId(), 0L);
            Double rating = storeRatings.get(store.getStoreId());
            long reviewCount = storeReviewCounts.getOrDefault(store.getStoreId(), 0L);

            String storeImageUrl = storeImageRepository.findByStoreStoreIdAndPrimaryTrue(store.getStoreId())
                    .map(StoreImage::getImageUrl)
                    .orElseGet(() -> storeImageRepository.findAllByStoreStoreId(store.getStoreId()).stream()
                            .findFirst()
                            .map(StoreImage::getImageUrl)
                            .orElse("https://images.unsplash.com/photo-1501339847302-ac426a4a7cbb?q=80&w=800&auto=format&fit=crop"));

            trendingList.add(CityTrendingResponse.builder()
                    .itemType("STORE")
                    .storeId(store.getStoreId())
                    .storeName(store.getName())
                    .tagName(store.getName())
                    .categoryCode("STORE")
                    .categoryName("Quán nổi bật")
                    .badgeText("Được lưu nhiều")
                    .subtitle(store.getAddress() != null && !store.getAddress().isBlank()
                            ? store.getAddress()
                            : "Quán cà phê được lưu yêu thích nhiều nhất")
                    .imageUrl(storeImageUrl)
                    .interestedCount(rawFav)
                    .storeCount(1L)
                    .rating(rating)
                    .reviewCount(reviewCount)
                    .build());
        }

        return trendingList;
    }

    private String resolveTagImageUrl(Tag tag) {
        if (tag.getImageUrl() != null && !tag.getImageUrl().isBlank()) {
            return tag.getImageUrl();
        }
        return "https://res.cloudinary.com/dy45rrkhf/image/upload/v1789584331/moodcafe/vibes/modern.jpg";
    }

    private String resolveShortName(String fullName) {
        if (fullName == null) return "";
        if (fullName.contains("(")) {
            return fullName.substring(0, fullName.indexOf('(')).trim();
        }
        return fullName.trim();
    }
}
