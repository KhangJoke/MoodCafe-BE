package com.moodcafe.store.abstraction.repository;

import com.moodcafe.store.entity.StoreStaff;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StoreStaffRepository extends JpaRepository<StoreStaff, UUID> {

    List<StoreStaff> findAllByStoreStoreId(UUID storeId);

    List<StoreStaff> findAllByUserUserId(UUID userId);

    Optional<StoreStaff> findByStoreStoreIdAndUserUserId(UUID storeId, UUID userId);

    boolean existsByStoreStoreIdAndUserUserId(UUID storeId, UUID userId);

    void deleteByStoreStoreIdAndUserUserId(UUID storeId, UUID userId);
}
