package com.moodcafe.menu.service;

import com.moodcafe.menu.dto.request.CreateMenuCategoryRequest;
import com.moodcafe.menu.dto.request.CreateMenuItemRequest;
import com.moodcafe.menu.dto.request.UpdateMenuCategoryRequest;
import com.moodcafe.menu.dto.request.UpdateMenuItemRequest;
import com.moodcafe.menu.dto.response.MenuCategoryResponse;
import com.moodcafe.menu.dto.response.MenuItemResponse;
import com.moodcafe.menu.entity.MenuCategory;
import com.moodcafe.menu.entity.MenuItem;
import com.moodcafe.menu.repository.MenuCategoryRepository;
import com.moodcafe.menu.repository.MenuItemRepository;
import com.moodcafe.shared.error.ErrorCode;
import com.moodcafe.shared.exceptions.AppException;
import com.moodcafe.store.abstraction.repository.StoreRepository;
import com.moodcafe.store.abstraction.service.StoreStaffService;
import com.moodcafe.store.entity.Store;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MenuServiceImpl implements MenuService {

    private final MenuCategoryRepository categoryRepository;
    private final MenuItemRepository itemRepository;
    private final StoreRepository storeRepository;
    private final StoreStaffService storeStaffService;

    @Override
    @Transactional(readOnly = true)
    public List<MenuCategoryResponse> getCategories(UUID storeId) {
        if (!storeRepository.existsById(storeId)) {
            throw new AppException(ErrorCode.STORE_NOT_FOUND);
        }

        List<MenuCategory> categories = categoryRepository
                .findByStoreStoreIdOrderByDisplayOrderAscCreatedAtAsc(storeId);

        return categories.stream().map(cat -> {
            long itemCount = itemRepository.countByCategoryCategoryIdAndIsDeletedFalse(cat.getCategoryId());
            return mapToCategoryResponse(cat, itemCount);
        }).toList();
    }

    @Override
    @Transactional
    public MenuCategoryResponse createCategory(UUID storeId, CreateMenuCategoryRequest request) {
        storeStaffService.requireStoreAccess(storeId, "OWNER", "MANAGER");

        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new AppException(ErrorCode.STORE_NOT_FOUND));

        String trimmedName = request.getName().trim();
        if (categoryRepository.existsByStoreStoreIdAndNameIgnoreCaseAndIsDeletedFalse(storeId, trimmedName)) {
            throw new AppException(ErrorCode.MENU_CATEGORY_NAME_EXISTS);
        }

        MenuCategory category = MenuCategory.builder()
                .store(store)
                .name(trimmedName)
                .displayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0)
                .build();

        category = categoryRepository.save(category);
        return mapToCategoryResponse(category, 0);
    }

    @Override
    @Transactional
    public MenuCategoryResponse updateCategory(UUID storeId, UUID categoryId, UpdateMenuCategoryRequest request) {
        storeStaffService.requireStoreAccess(storeId, "OWNER", "MANAGER");

        MenuCategory category = categoryRepository.findByCategoryIdAndStoreStoreId(categoryId, storeId)
                .orElseThrow(() -> new AppException(ErrorCode.MENU_CATEGORY_NOT_FOUND));

        String trimmedName = request.getName().trim();
        if (categoryRepository.existsByStoreStoreIdAndNameIgnoreCaseAndCategoryIdNotAndIsDeletedFalse(
                storeId, trimmedName, categoryId)) {
            throw new AppException(ErrorCode.MENU_CATEGORY_NAME_EXISTS);
        }

        category.setName(trimmedName);
        if (request.getDisplayOrder() != null) {
            category.setDisplayOrder(request.getDisplayOrder());
        }

        category = categoryRepository.save(category);
        long itemCount = itemRepository.countByCategoryCategoryIdAndIsDeletedFalse(categoryId);
        return mapToCategoryResponse(category, itemCount);
    }

    @Override
    @Transactional
    public void deleteCategory(UUID storeId, UUID categoryId) {
        storeStaffService.requireStoreAccess(storeId, "OWNER", "MANAGER");

        MenuCategory category = categoryRepository.findByCategoryIdAndStoreStoreId(categoryId, storeId)
                .orElseThrow(() -> new AppException(ErrorCode.MENU_CATEGORY_NOT_FOUND));

        long activeItems = itemRepository.countByCategoryCategoryIdAndIsDeletedFalse(categoryId);
        if (activeItems > 0) {
            throw new AppException(ErrorCode.CATEGORY_HAS_ITEMS);
        }

        category.setDeleted(true);
        category.setDeletedAt(Instant.now());
        categoryRepository.delete(category);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MenuItemResponse> getItems(UUID storeId, UUID categoryId, String q, Boolean isAvailable, Pageable pageable) {
        if (!storeRepository.existsById(storeId)) {
            throw new AppException(ErrorCode.STORE_NOT_FOUND);
        }

        Specification<MenuItem> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("store").get("storeId"), storeId));
            predicates.add(cb.equal(root.get("isDeleted"), false));

            if (categoryId != null) {
                predicates.add(cb.equal(root.get("category").get("categoryId"), categoryId));
            }
            if (q != null && !q.trim().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("name")), "%" + q.trim().toLowerCase() + "%"));
            }
            if (isAvailable != null) {
                predicates.add(cb.equal(root.get("isAvailable"), isAvailable));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return itemRepository.findAll(spec, pageable).map(this::mapToItemResponse);
    }

    @Override
    @Transactional
    public MenuItemResponse createItem(UUID storeId, CreateMenuItemRequest request) {
        storeStaffService.requireStoreAccess(storeId, "OWNER", "MANAGER");

        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new AppException(ErrorCode.STORE_NOT_FOUND));

        MenuCategory category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new AppException(ErrorCode.MENU_CATEGORY_NOT_FOUND));

        if (!category.getStore().getStoreId().equals(storeId)) {
            throw new AppException(ErrorCode.MENU_ITEM_STORE_MISMATCH);
        }

        MenuItem item = MenuItem.builder()
                .store(store)
                .category(category)
                .name(request.getName().trim())
                .description(request.getDescription())
                .price(request.getPrice())
                .imageUrl(request.getImageUrl())
                .isAvailable(request.getIsAvailable() != null ? request.getIsAvailable() : true)
                .build();

        item = itemRepository.save(item);
        return mapToItemResponse(item);
    }

    @Override
    @Transactional
    public MenuItemResponse updateItem(UUID storeId, UUID itemId, UpdateMenuItemRequest request) {
        storeStaffService.requireStoreAccess(storeId, "OWNER", "MANAGER");

        MenuItem item = itemRepository.findByItemIdAndStoreStoreId(itemId, storeId)
                .orElseThrow(() -> new AppException(ErrorCode.MENU_ITEM_NOT_FOUND));

        if (request.getCategoryId() != null) {
            MenuCategory newCategory = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new AppException(ErrorCode.MENU_CATEGORY_NOT_FOUND));
            if (!newCategory.getStore().getStoreId().equals(storeId)) {
                throw new AppException(ErrorCode.MENU_ITEM_STORE_MISMATCH);
            }
            item.setCategory(newCategory);
        }

        if (request.getName() != null && !request.getName().trim().isEmpty()) {
            item.setName(request.getName().trim());
        }
        if (request.getDescription() != null) {
            item.setDescription(request.getDescription());
        }
        if (request.getPrice() != null) {
            item.setPrice(request.getPrice());
        }
        if (request.getImageUrl() != null) {
            item.setImageUrl(request.getImageUrl());
        }
        if (request.getIsAvailable() != null) {
            item.setAvailable(request.getIsAvailable());
        }

        item = itemRepository.save(item);
        return mapToItemResponse(item);
    }

    @Override
    @Transactional
    public void deleteItem(UUID storeId, UUID itemId) {
        storeStaffService.requireStoreAccess(storeId, "OWNER", "MANAGER");

        MenuItem item = itemRepository.findByItemIdAndStoreStoreId(itemId, storeId)
                .orElseThrow(() -> new AppException(ErrorCode.MENU_ITEM_NOT_FOUND));

        item.setDeleted(true);
        item.setDeletedAt(Instant.now());
        itemRepository.delete(item);
    }

    private MenuCategoryResponse mapToCategoryResponse(MenuCategory category, long itemCount) {
        return MenuCategoryResponse.builder()
                .categoryId(category.getCategoryId())
                .storeId(category.getStore().getStoreId())
                .name(category.getName())
                .displayOrder(category.getDisplayOrder())
                .itemCount(itemCount)
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .build();
    }

    private MenuItemResponse mapToItemResponse(MenuItem item) {
        return MenuItemResponse.builder()
                .itemId(item.getItemId())
                .storeId(item.getStore().getStoreId())
                .categoryId(item.getCategory().getCategoryId())
                .categoryName(item.getCategory().getName())
                .name(item.getName())
                .description(item.getDescription())
                .price(item.getPrice())
                .imageUrl(item.getImageUrl())
                .isAvailable(item.isAvailable())
                .createdAt(item.getCreatedAt())
                .updatedAt(item.getUpdatedAt())
                .build();
    }
}
