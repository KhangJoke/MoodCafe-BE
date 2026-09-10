package com.moodcafe.store.abstraction.service;

import com.moodcafe.store.dto.response.FavoriteStoreResponse;

import java.util.List;
import java.util.UUID;

public interface IFavoriteStoreService {

    void addFavoriteStore(UUID storeId);

    void removeFavoriteStore(UUID storeId);

    List<FavoriteStoreResponse> getMyFavoriteStores();

    boolean isFavoriteStore(UUID storeId);
}
