package com.moodcafe.menu.repository;

import com.moodcafe.menu.entity.MenuItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface MenuItemRepository extends JpaRepository<MenuItem, UUID>, JpaSpecificationExecutor<MenuItem> {

    long countByCategoryCategoryIdAndIsDeletedFalse(UUID categoryId);

    Optional<MenuItem> findByItemIdAndStoreStoreId(UUID itemId, UUID storeId);

    Optional<MenuItem> findByItemIdAndStoreStoreIdAndIsDeletedFalse(UUID itemId, UUID storeId);
}
