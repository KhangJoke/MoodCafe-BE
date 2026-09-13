package com.moodcafe.store.controller;

import com.moodcafe.shared.response.ApiResponse;
import com.moodcafe.store.abstraction.service.FavoriteStoreService;
import com.moodcafe.store.dto.response.FavoriteStoreResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class FavoriteStoreController {

    private final FavoriteStoreService favoriteStoreService;

    @PostMapping("/api/stores/{storeId}/favorite")
    public ResponseEntity<ApiResponse<Void>> addFavoriteStore(@PathVariable UUID storeId) {
        favoriteStoreService.addFavoriteStore(storeId);
        return ResponseEntity.ok(ApiResponse.success(null, "Store added to favorites"));
    }

    @DeleteMapping("/api/stores/{storeId}/favorite")
    public ResponseEntity<ApiResponse<Void>> removeFavoriteStore(@PathVariable UUID storeId) {
        favoriteStoreService.removeFavoriteStore(storeId);
        return ResponseEntity.ok(ApiResponse.success(null, "Store removed from favorites"));
    }

    @GetMapping("/api/stores/{storeId}/favorite")
    public ResponseEntity<ApiResponse<Boolean>> isFavoriteStore(@PathVariable UUID storeId) {
        boolean isFavorite = favoriteStoreService.isFavoriteStore(storeId);
        return ResponseEntity.ok(ApiResponse.success(isFavorite));
    }

    @GetMapping("/api/users/me/favorites")
    public ResponseEntity<ApiResponse<List<FavoriteStoreResponse>>> getMyFavoriteStores() {
        List<FavoriteStoreResponse> favorites = favoriteStoreService.getMyFavoriteStores();
        return ResponseEntity.ok(ApiResponse.success(favorites));
    }
}
