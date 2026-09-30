package com.moodcafe.menu.repository;

import com.moodcafe.menu.entity.MenuCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MenuCategoryRepository extends JpaRepository<MenuCategory, UUID> {

    List<MenuCategory> findByStoreStoreIdOrderByDisplayOrderAscCreatedAtAsc(UUID storeId);

    List<MenuCategory> findByStoreStoreIdAndIsDeletedFalseOrderByDisplayOrderAscCreatedAtAsc(UUID storeId);

    Optional<MenuCategory> findByCategoryIdAndStoreStoreId(UUID categoryId, UUID storeId);

    Optional<MenuCategory> findByCategoryIdAndStoreStoreIdAndIsDeletedFalse(UUID categoryId, UUID storeId);

    boolean existsByStoreStoreIdAndNameIgnoreCaseAndIsDeletedFalse(UUID storeId, String name);

    boolean existsByStoreStoreIdAndNameIgnoreCaseAndCategoryIdNotAndIsDeletedFalse(UUID storeId, String name, UUID categoryId);
}
