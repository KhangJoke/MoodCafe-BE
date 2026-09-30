package com.moodcafe.menu.service;

import com.moodcafe.menu.dto.request.CreateMenuCategoryRequest;
import com.moodcafe.menu.dto.request.CreateMenuItemRequest;
import com.moodcafe.menu.dto.request.UpdateMenuCategoryRequest;
import com.moodcafe.menu.dto.request.UpdateMenuItemRequest;
import com.moodcafe.menu.dto.response.MenuCategoryResponse;
import com.moodcafe.menu.dto.response.MenuItemResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface MenuService {

    List<MenuCategoryResponse> getCategories(UUID storeId);

    MenuCategoryResponse createCategory(UUID storeId, CreateMenuCategoryRequest request);

    MenuCategoryResponse updateCategory(UUID storeId, UUID categoryId, UpdateMenuCategoryRequest request);

    void deleteCategory(UUID storeId, UUID categoryId);

    Page<MenuItemResponse> getItems(UUID storeId, UUID categoryId, String q, Boolean isAvailable, Pageable pageable);

    MenuItemResponse createItem(UUID storeId, CreateMenuItemRequest request);

    MenuItemResponse updateItem(UUID storeId, UUID itemId, UpdateMenuItemRequest request);

    void deleteItem(UUID storeId, UUID itemId);
}
