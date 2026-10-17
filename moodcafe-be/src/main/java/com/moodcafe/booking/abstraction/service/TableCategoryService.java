package com.moodcafe.booking.abstraction.service;

import com.moodcafe.booking.dto.request.CreateTableCategoryRequest;
import com.moodcafe.booking.dto.request.UpdateTableCategoryRequest;
import com.moodcafe.booking.dto.response.TableCategoryResponse;

import java.util.List;
import java.util.UUID;

public interface TableCategoryService {

    List<TableCategoryResponse> getTableCategories(UUID storeId);

    List<TableCategoryResponse> getActiveTableCategories(UUID storeId);

    TableCategoryResponse getTableCategory(UUID storeId, UUID categoryId);

    TableCategoryResponse createTableCategory(UUID storeId, CreateTableCategoryRequest request);

    TableCategoryResponse updateTableCategory(UUID storeId, UUID categoryId, UpdateTableCategoryRequest request);

    void deleteTableCategory(UUID storeId, UUID categoryId);
}
