package com.moodcafe.store.service;

import com.moodcafe.auth.abstraction.service.CurrentUserService;
import com.moodcafe.auth.entity.User;
import com.moodcafe.shared.error.ErrorCode;
import com.moodcafe.shared.exceptions.AppException;
import com.moodcafe.store.abstraction.repository.FavoriteStoreRepository;
import com.moodcafe.store.abstraction.repository.StoreImageRepository;
import com.moodcafe.store.abstraction.repository.StoreRepository;
import com.moodcafe.store.abstraction.repository.StoreReviewRepository;
import com.moodcafe.store.abstraction.service.FavoriteStoreService;
import com.moodcafe.store.dto.response.FavoriteStoreResponse;
import com.moodcafe.store.dto.response.StoreSearchItemResponse.StoreSearchTagItem;
import com.moodcafe.store.entity.FavoriteStore;
import com.moodcafe.store.entity.Store;
import com.moodcafe.store.entity.StoreImage;
import com.moodcafe.store.mapper.FavoriteStoreMapper;
import com.moodcafe.tag.abstraction.repository.StoreTagRepository;
import com.moodcafe.tag.entity.StoreTag;
import com.moodcafe.tag.entity.enums.StoreTagStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FavoriteStoreServiceImpl implements FavoriteStoreService {

    private final FavoriteStoreRepository favoriteStoreRepository;
    private final StoreRepository storeRepository;
    private final StoreImageRepository storeImageRepository;
    private final StoreReviewRepository storeReviewRepository;
    private final StoreTagRepository storeTagRepository;
    private final FavoriteStoreMapper favoriteStoreMapper;
    private final CurrentUserService currentUserService;

    @Override
    @Transactional
    public void addFavoriteStore(UUID storeId) {
        User currentUser = currentUserService.getCurrentUser();

        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new AppException(ErrorCode.STORE_NOT_FOUND));

        if (favoriteStoreRepository.existsByUserUserIdAndStoreStoreId(currentUser.getUserId(), storeId)) {
            throw new AppException(ErrorCode.FAVORITE_STORE_ALREADY_EXISTS);
        }

        int restored = favoriteStoreRepository.restoreByUserUserIdAndStoreStoreId(currentUser.getUserId(), storeId);
        if (restored > 0) {
            return;
        }

        FavoriteStore favoriteStore = FavoriteStore.builder()
                .user(currentUser)
                .store(store)
                .build();

        favoriteStoreRepository.save(favoriteStore);
    }

    @Override
    @Transactional
    public void removeFavoriteStore(UUID storeId) {
        User currentUser = currentUserService.getCurrentUser();

        if (!favoriteStoreRepository.existsByUserUserIdAndStoreStoreId(currentUser.getUserId(), storeId)) {
            throw new AppException(ErrorCode.FAVORITE_STORE_NOT_FOUND);
        }

        favoriteStoreRepository.deleteByUserUserIdAndStoreStoreId(currentUser.getUserId(), storeId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FavoriteStoreResponse> getMyFavoriteStores() {
        User currentUser = currentUserService.getCurrentUser();

        List<FavoriteStore> favorites = favoriteStoreRepository.findAllByUserUserId(currentUser.getUserId());
        if (favorites.isEmpty()) {
            return Collections.emptyList();
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

        LocalTime now = LocalTime.now();

        return favorites.stream()
                .map(fav -> {
                    FavoriteStoreResponse response = favoriteStoreMapper.toResponse(fav);
                    Store store = fav.getStore();
                    UUID storeId = store.getStoreId();

                    storeImageRepository.findByStoreStoreIdAndPrimaryTrue(storeId)
                            .map(StoreImage::getImageUrl)
                            .ifPresent(response::setPrimaryImageUrl);

                    // Rating & reviews
                    response.setOverallRating(ratingMap.getOrDefault(storeId, 0.0));
                    response.setReviewCount(reviewCountMap.getOrDefault(storeId, 0L));

                    // Is open now
                    boolean isOpen = false;
                    if (store.getOpeningTime() != null && store.getClosingTime() != null) {
                        if (store.getClosingTime().isAfter(store.getOpeningTime())) {
                            isOpen = !now.isBefore(store.getOpeningTime()) && !now.isAfter(store.getClosingTime());
                        } else {
                            isOpen = !now.isBefore(store.getOpeningTime()) || !now.isAfter(store.getClosingTime());
                        }
                    }
                    response.setOpenNow(isOpen);

                    // Highlight tags (up to 4 tags)
                    List<StoreTag> storeTags = storeTagRepository.findAllByStoreIdAndStatus(storeId, StoreTagStatus.APPROVED);
                    List<StoreSearchTagItem> highlightTags = storeTags.stream()
                            .filter(st -> st.getTag() != null)
                            .sorted((a, b) -> Boolean.compare(b.isHighlighted(), a.isHighlighted()))
                            .limit(4)
                            .map(st -> StoreSearchTagItem.builder()
                                    .tagId(st.getTag().getTagId())
                                    .name(st.getTag().getName())
                                    .categoryCode(st.getTag().getCategory() != null ? st.getTag().getCategory().getCode() : "")
                                    .categoryName(st.getTag().getCategory() != null ? st.getTag().getCategory().getName() : "")
                                    .scaleValue(st.getTag().getScaleValue())
                                    .build())
                            .toList();
                    response.setHighlightTags(highlightTags);

                    return response;
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isFavoriteStore(UUID storeId) {
        User currentUser = currentUserService.getCurrentUser();
        return favoriteStoreRepository.existsByUserUserIdAndStoreStoreId(currentUser.getUserId(), storeId);
    }

}

