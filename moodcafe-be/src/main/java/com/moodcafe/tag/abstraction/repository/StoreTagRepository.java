package com.moodcafe.tag.abstraction.repository;

import com.moodcafe.tag.entity.StoreTag;
import com.moodcafe.tag.entity.enums.StoreTagStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StoreTagRepository extends JpaRepository<StoreTag, UUID> {

    List<StoreTag> findAllByStoreId(UUID storeId);

    List<StoreTag> findAllByStoreIdAndStatus(UUID storeId, StoreTagStatus status);

    Optional<StoreTag> findByStoreIdAndTagTagId(UUID storeId, UUID tagId);

    List<StoreTag> findAllByStoreIdAndTagCategoryTagCategoryId(UUID storeId, UUID tagCategoryId);

    List<StoreTag> findAllByStatusOrderByCreatedAtDesc(StoreTagStatus status);
}
