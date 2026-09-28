package com.moodcafe.store.abstraction.repository;

import com.moodcafe.store.entity.StoreImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StoreImageRepository extends JpaRepository<StoreImage, UUID> {

    List<StoreImage> findAllByStoreStoreId(UUID storeId);

    Optional<StoreImage> findFirstByStoreStoreIdAndPrimaryTrueOrderByCreatedAtDesc(UUID storeId);

    default Optional<StoreImage> findByStoreStoreIdAndPrimaryTrue(UUID storeId) {
        return findFirstByStoreStoreIdAndPrimaryTrueOrderByCreatedAtDesc(storeId);
    }

    Optional<StoreImage> findByStoreImageIdAndStoreStoreId(UUID storeImageId, UUID storeId);
}
