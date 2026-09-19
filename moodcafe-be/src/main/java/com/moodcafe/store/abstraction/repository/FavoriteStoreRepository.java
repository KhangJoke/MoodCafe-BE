package com.moodcafe.store.abstraction.repository;

import com.moodcafe.store.entity.FavoriteStore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FavoriteStoreRepository extends JpaRepository<FavoriteStore, UUID> {

    List<FavoriteStore> findAllByUserUserId(UUID userId);

    boolean existsByUserUserIdAndStoreStoreId(UUID userId, UUID storeId);

    Optional<FavoriteStore> findByUserUserIdAndStoreStoreId(UUID userId, UUID storeId);

    void deleteByUserUserIdAndStoreStoreId(UUID userId, UUID storeId);

    @Modifying
    @Query(value = "UPDATE favorite_stores SET is_deleted = false, created_at = CURRENT_TIMESTAMP WHERE user_id = :userId AND store_id = :storeId", nativeQuery = true)
    int restoreByUserUserIdAndStoreStoreId(@Param("userId") UUID userId, @Param("storeId") UUID storeId);

    @Query("SELECT fs.store.storeId, COUNT(DISTINCT fs.favoriteStoreId) FROM FavoriteStore fs GROUP BY fs.store.storeId")
    List<Object[]> countFavoritesGroupedByStore();
}
