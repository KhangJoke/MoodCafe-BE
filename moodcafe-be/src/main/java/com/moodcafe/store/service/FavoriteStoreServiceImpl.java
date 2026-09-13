package com.moodcafe.store.service;

import com.moodcafe.auth.abstraction.service.CurrentUserService;
import com.moodcafe.auth.entity.User;

import com.moodcafe.shared.error.ErrorCode;
import com.moodcafe.shared.exceptions.AppException;
import com.moodcafe.store.abstraction.repository.FavoriteStoreRepository;
import com.moodcafe.store.abstraction.repository.StoreImageRepository;
import com.moodcafe.store.abstraction.repository.StoreRepository;
import com.moodcafe.store.abstraction.service.FavoriteStoreService;
import com.moodcafe.store.dto.response.FavoriteStoreResponse;
import com.moodcafe.store.entity.FavoriteStore;
import com.moodcafe.store.entity.Store;
import com.moodcafe.store.entity.StoreImage;
import com.moodcafe.store.mapper.FavoriteStoreMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FavoriteStoreServiceImpl implements FavoriteStoreService {

    private final FavoriteStoreRepository favoriteStoreRepository;
    private final StoreRepository storeRepository;
    private final StoreImageRepository storeImageRepository;
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

        return favorites.stream()
                .map(fav -> {
                    FavoriteStoreResponse response = favoriteStoreMapper.toResponse(fav);
                    storeImageRepository.findByStoreStoreIdAndPrimaryTrue(fav.getStore().getStoreId())
                            .map(StoreImage::getImageUrl)
                            .ifPresent(response::setPrimaryImageUrl);
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
