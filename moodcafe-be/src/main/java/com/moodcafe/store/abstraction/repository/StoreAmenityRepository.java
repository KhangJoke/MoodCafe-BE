package com.moodcafe.store.abstraction.repository;

import com.moodcafe.store.entity.StoreAmenity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StoreAmenityRepository extends JpaRepository<StoreAmenity, UUID> {

    List<StoreAmenity> findAllByStoreStoreId(UUID storeId);

    boolean existsByStoreStoreIdAndAmenityAmenityId(UUID storeId, UUID amenityId);

    Optional<StoreAmenity> findByStoreStoreIdAndAmenityAmenityId(UUID storeId, UUID amenityId);

    void deleteByStoreStoreIdAndAmenityAmenityId(UUID storeId, UUID amenityId);
}
