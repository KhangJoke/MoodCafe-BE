package com.moodcafe.booking.service;

import com.moodcafe.booking.abstraction.repository.TableCategoryRepository;
import com.moodcafe.booking.abstraction.service.TableCategoryService;
import com.moodcafe.booking.dto.request.CreateTableCategoryRequest;
import com.moodcafe.booking.dto.request.UpdateTableCategoryRequest;
import com.moodcafe.booking.dto.response.TableCategoryResponse;
import com.moodcafe.booking.entity.TableCategory;
import com.moodcafe.booking.mapper.BookingMapper;
import com.moodcafe.shared.error.ErrorCode;
import com.moodcafe.shared.exceptions.AppException;
import com.moodcafe.store.abstraction.repository.StoreRepository;
import com.moodcafe.store.abstraction.service.StoreStaffService;
import com.moodcafe.store.entity.Store;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TableCategoryServiceImpl implements TableCategoryService {

    private final TableCategoryRepository categoryRepository;
    private final StoreRepository storeRepository;
    private final StoreStaffService storeStaffService;
    private final BookingMapper bookingMapper;

    @Override
    @Transactional(readOnly = true)
    public List<TableCategoryResponse> getTableCategories(UUID storeId) {
        storeStaffService.requireStoreAccess(storeId, "OWNER", "MANAGER", "CASHIER");

        if (!storeRepository.existsById(storeId)) {
            throw new AppException(ErrorCode.STORE_NOT_FOUND);
        }

        return categoryRepository.findAllByStoreStoreId(storeId)
                .stream()
                .map(bookingMapper::toCategoryResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TableCategoryResponse> getActiveTableCategories(UUID storeId) {
        if (!storeRepository.existsById(storeId)) {
            throw new AppException(ErrorCode.STORE_NOT_FOUND);
        }

        return categoryRepository.findAllByStoreStoreIdAndIsActiveTrue(storeId)
                .stream()
                .map(bookingMapper::toCategoryResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TableCategoryResponse getTableCategory(UUID storeId, UUID categoryId) {
        TableCategory category = categoryRepository.findByCategoryIdAndStoreStoreId(categoryId, storeId)
                .orElseThrow(() -> new AppException(ErrorCode.TABLE_CATEGORY_NOT_FOUND));

        return bookingMapper.toCategoryResponse(category);
    }

    @Override
    @Transactional
    public TableCategoryResponse createTableCategory(UUID storeId, CreateTableCategoryRequest request) {
        storeStaffService.requireStoreAccess(storeId, "OWNER", "MANAGER");

        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new AppException(ErrorCode.STORE_NOT_FOUND));

        TableCategory category = TableCategory.builder()
                .store(store)
                .name(request.getName().trim())
                .description(request.getDescription() != null ? request.getDescription().trim() : null)
                .maxCapacity(request.getMaxCapacity())
                .appQuota(request.getAppQuota())
                .minSpendAmount(request.getMinSpendAmount())
                .imageUrl(request.getImageUrl() != null ? request.getImageUrl().trim() : null)
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();

        category = categoryRepository.save(category);
        return bookingMapper.toCategoryResponse(category);
    }

    @Override
    @Transactional
    public TableCategoryResponse updateTableCategory(UUID storeId, UUID categoryId, UpdateTableCategoryRequest request) {
        storeStaffService.requireStoreAccess(storeId, "OWNER", "MANAGER");

        TableCategory category = categoryRepository.findByCategoryIdAndStoreStoreId(categoryId, storeId)
                .orElseThrow(() -> new AppException(ErrorCode.TABLE_CATEGORY_NOT_FOUND));

        category.setName(request.getName().trim());
        category.setDescription(request.getDescription() != null ? request.getDescription().trim() : null);
        category.setMaxCapacity(request.getMaxCapacity());
        category.setAppQuota(request.getAppQuota());
        category.setMinSpendAmount(request.getMinSpendAmount());
        category.setImageUrl(request.getImageUrl() != null ? request.getImageUrl().trim() : null);
        category.setIsActive(request.getIsActive());

        category = categoryRepository.save(category);
        return bookingMapper.toCategoryResponse(category);
    }

    @Override
    @Transactional
    public void deleteTableCategory(UUID storeId, UUID categoryId) {
        storeStaffService.requireStoreAccess(storeId, "OWNER", "MANAGER");

        TableCategory category = categoryRepository.findByCategoryIdAndStoreStoreId(categoryId, storeId)
                .orElseThrow(() -> new AppException(ErrorCode.TABLE_CATEGORY_NOT_FOUND));

        categoryRepository.delete(category);
    }
}
