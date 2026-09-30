package com.moodcafe.menu;

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
import com.moodcafe.menu.service.MenuServiceImpl;
import com.moodcafe.shared.error.ErrorCode;
import com.moodcafe.shared.exceptions.AppException;
import com.moodcafe.store.abstraction.repository.StoreRepository;
import com.moodcafe.store.abstraction.service.StoreStaffService;
import com.moodcafe.store.entity.Store;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MenuServiceTest {

    @Mock
    private MenuCategoryRepository categoryRepository;

    @Mock
    private MenuItemRepository itemRepository;

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private StoreStaffService storeStaffService;

    @InjectMocks
    private MenuServiceImpl menuService;

    private UUID storeId;
    private Store store;
    private UUID categoryId;
    private MenuCategory category;
    private UUID itemId;
    private MenuItem item;

    @BeforeEach
    void setUp() {
        storeId = UUID.randomUUID();
        store = Store.builder()
                .storeId(storeId)
                .name("Mood Cafe Da Nang")
                .build();

        categoryId = UUID.randomUUID();
        category = MenuCategory.builder()
                .categoryId(categoryId)
                .store(store)
                .name("Coffee")
                .displayOrder(1)
                .isDeleted(false)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        itemId = UUID.randomUUID();
        item = MenuItem.builder()
                .itemId(itemId)
                .store(store)
                .category(category)
                .name("Bac Xiu")
                .description("Delicious coffee with sweet condensed milk")
                .price(new BigDecimal("35000"))
                .imageUrl("https://example.com/bacxiu.jpg")
                .isAvailable(true)
                .isDeleted(false)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Nested
    @DisplayName("Category Management Tests")
    class CategoryTests {

        @Test
        @DisplayName("getCategories - returns list with itemCount")
        void getCategories_Success() {
            when(storeRepository.existsById(storeId)).thenReturn(true);
            when(categoryRepository.findByStoreStoreIdOrderByDisplayOrderAscCreatedAtAsc(storeId))
                    .thenReturn(List.of(category));
            when(itemRepository.countByCategoryCategoryIdAndIsDeletedFalse(categoryId)).thenReturn(5L);

            List<MenuCategoryResponse> result = menuService.getCategories(storeId);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getCategoryId()).isEqualTo(categoryId);
            assertThat(result.get(0).getName()).isEqualTo("Coffee");
            assertThat(result.get(0).getItemCount()).isEqualTo(5L);
        }

        @Test
        @DisplayName("getCategories - store not found throws STORE_NOT_FOUND")
        void getCategories_StoreNotFound_ThrowsException() {
            when(storeRepository.existsById(storeId)).thenReturn(false);

            assertThatThrownBy(() -> menuService.getCategories(storeId))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getErrorCode()).isEqualTo(ErrorCode.STORE_NOT_FOUND));
        }

        @Test
        @DisplayName("createCategory - success")
        void createCategory_Success() {
            CreateMenuCategoryRequest request = CreateMenuCategoryRequest.builder()
                    .name("Tea & Fruit")
                    .displayOrder(2)
                    .build();

            when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
            when(categoryRepository.existsByStoreStoreIdAndNameIgnoreCaseAndIsDeletedFalse(storeId, "Tea & Fruit"))
                    .thenReturn(false);
            when(categoryRepository.save(any(MenuCategory.class))).thenAnswer(invocation -> {
                MenuCategory saved = invocation.getArgument(0);
                saved.setCategoryId(UUID.randomUUID());
                saved.setCreatedAt(Instant.now());
                saved.setUpdatedAt(Instant.now());
                return saved;
            });

            MenuCategoryResponse response = menuService.createCategory(storeId, request);

            verify(storeStaffService).requireStoreAccess(storeId, "OWNER", "MANAGER");
            assertThat(response).isNotNull();
            assertThat(response.getName()).isEqualTo("Tea & Fruit");
            assertThat(response.getDisplayOrder()).isEqualTo(2);
            assertThat(response.getItemCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("createCategory - duplicate name throws MENU_CATEGORY_NAME_EXISTS")
        void createCategory_DuplicateName_ThrowsException() {
            CreateMenuCategoryRequest request = CreateMenuCategoryRequest.builder()
                    .name("Coffee")
                    .build();

            when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
            when(categoryRepository.existsByStoreStoreIdAndNameIgnoreCaseAndIsDeletedFalse(storeId, "Coffee"))
                    .thenReturn(true);

            assertThatThrownBy(() -> menuService.createCategory(storeId, request))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getErrorCode()).isEqualTo(ErrorCode.MENU_CATEGORY_NAME_EXISTS));
        }

        @Test
        @DisplayName("createCategory - forbidden access throws FORBIDDEN_STORE_ACCESS")
        void createCategory_ForbiddenAccess_ThrowsException() {
            doThrow(new AppException(ErrorCode.FORBIDDEN_STORE_ACCESS))
                    .when(storeStaffService).requireStoreAccess(storeId, "OWNER", "MANAGER");

            CreateMenuCategoryRequest request = CreateMenuCategoryRequest.builder().name("Juice").build();

            assertThatThrownBy(() -> menuService.createCategory(storeId, request))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN_STORE_ACCESS));
        }

        @Test
        @DisplayName("updateCategory - success")
        void updateCategory_Success() {
            UpdateMenuCategoryRequest request = UpdateMenuCategoryRequest.builder()
                    .name("Special Coffee")
                    .displayOrder(10)
                    .build();

            when(categoryRepository.findByCategoryIdAndStoreStoreId(categoryId, storeId))
                    .thenReturn(Optional.of(category));
            when(categoryRepository.existsByStoreStoreIdAndNameIgnoreCaseAndCategoryIdNotAndIsDeletedFalse(
                    storeId, "Special Coffee", categoryId)).thenReturn(false);
            when(categoryRepository.save(any(MenuCategory.class))).thenReturn(category);
            when(itemRepository.countByCategoryCategoryIdAndIsDeletedFalse(categoryId)).thenReturn(3L);

            MenuCategoryResponse response = menuService.updateCategory(storeId, categoryId, request);

            verify(storeStaffService).requireStoreAccess(storeId, "OWNER", "MANAGER");
            assertThat(response.getName()).isEqualTo("Special Coffee");
            assertThat(response.getDisplayOrder()).isEqualTo(10);
            assertThat(response.getItemCount()).isEqualTo(3L);
        }

        @Test
        @DisplayName("updateCategory - category not found throws MENU_CATEGORY_NOT_FOUND")
        void updateCategory_NotFound_ThrowsException() {
            UpdateMenuCategoryRequest request = UpdateMenuCategoryRequest.builder().name("Drinks").build();
            when(categoryRepository.findByCategoryIdAndStoreStoreId(categoryId, storeId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> menuService.updateCategory(storeId, categoryId, request))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getErrorCode()).isEqualTo(ErrorCode.MENU_CATEGORY_NOT_FOUND));
        }

        @Test
        @DisplayName("updateCategory - duplicate name throws MENU_CATEGORY_NAME_EXISTS")
        void updateCategory_DuplicateName_ThrowsException() {
            UpdateMenuCategoryRequest request = UpdateMenuCategoryRequest.builder().name("Existing Cat").build();
            when(categoryRepository.findByCategoryIdAndStoreStoreId(categoryId, storeId))
                    .thenReturn(Optional.of(category));
            when(categoryRepository.existsByStoreStoreIdAndNameIgnoreCaseAndCategoryIdNotAndIsDeletedFalse(
                    storeId, "Existing Cat", categoryId)).thenReturn(true);

            assertThatThrownBy(() -> menuService.updateCategory(storeId, categoryId, request))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getErrorCode()).isEqualTo(ErrorCode.MENU_CATEGORY_NAME_EXISTS));
        }

        @Test
        @DisplayName("deleteCategory - success when no items in category")
        void deleteCategory_Success() {
            when(categoryRepository.findByCategoryIdAndStoreStoreId(categoryId, storeId))
                    .thenReturn(Optional.of(category));
            when(itemRepository.countByCategoryCategoryIdAndIsDeletedFalse(categoryId)).thenReturn(0L);

            menuService.deleteCategory(storeId, categoryId);

            verify(storeStaffService).requireStoreAccess(storeId, "OWNER", "MANAGER");
            assertThat(category.isDeleted()).isTrue();
            assertThat(category.getDeletedAt()).isNotNull();
            verify(categoryRepository).delete(category);
        }

        @Test
        @DisplayName("deleteCategory - category has active items throws CATEGORY_HAS_ITEMS (409)")
        void deleteCategory_HasItems_ThrowsException() {
            when(categoryRepository.findByCategoryIdAndStoreStoreId(categoryId, storeId))
                    .thenReturn(Optional.of(category));
            when(itemRepository.countByCategoryCategoryIdAndIsDeletedFalse(categoryId)).thenReturn(4L);

            assertThatThrownBy(() -> menuService.deleteCategory(storeId, categoryId))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getErrorCode()).isEqualTo(ErrorCode.CATEGORY_HAS_ITEMS));

            verify(categoryRepository, never()).delete(any());
        }
    }

    @Nested
    @DisplayName("Menu Item Management Tests")
    class MenuItemTests {

        @Test
        @DisplayName("getItems - returns paginated items with filters")
        void getItems_Success() {
            when(storeRepository.existsById(storeId)).thenReturn(true);
            Pageable pageable = PageRequest.of(0, 10);
            Page<MenuItem> page = new PageImpl<>(List.of(item), pageable, 1);
            when(itemRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

            Page<MenuItemResponse> result = menuService.getItems(storeId, categoryId, "Bac", true, pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getName()).isEqualTo("Bac Xiu");
            assertThat(result.getContent().get(0).getPrice()).isEqualTo(new BigDecimal("35000"));
        }

        @Test
        @DisplayName("createItem - success")
        void createItem_Success() {
            CreateMenuItemRequest request = CreateMenuItemRequest.builder()
                    .categoryId(categoryId)
                    .name("Caramel Macchiato")
                    .description("Sweet caramel syrup with espresso")
                    .price(new BigDecimal("49000"))
                    .imageUrl("https://example.com/caramel.jpg")
                    .isAvailable(true)
                    .build();

            when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
            when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
            when(itemRepository.save(any(MenuItem.class))).thenAnswer(inv -> {
                MenuItem saved = inv.getArgument(0);
                saved.setItemId(UUID.randomUUID());
                saved.setCreatedAt(Instant.now());
                saved.setUpdatedAt(Instant.now());
                return saved;
            });

            MenuItemResponse response = menuService.createItem(storeId, request);

            verify(storeStaffService).requireStoreAccess(storeId, "OWNER", "MANAGER");
            assertThat(response).isNotNull();
            assertThat(response.getName()).isEqualTo("Caramel Macchiato");
            assertThat(response.getPrice()).isEqualTo(new BigDecimal("49000"));
            assertThat(response.getCategoryId()).isEqualTo(categoryId);
            assertThat(response.getCategoryName()).isEqualTo("Coffee");
        }

        @Test
        @DisplayName("createItem - category from another store throws MENU_ITEM_STORE_MISMATCH")
        void createItem_StoreMismatch_ThrowsException() {
            Store anotherStore = Store.builder().storeId(UUID.randomUUID()).build();
            MenuCategory otherCategory = MenuCategory.builder()
                    .categoryId(UUID.randomUUID())
                    .store(anotherStore)
                    .build();

            CreateMenuItemRequest request = CreateMenuItemRequest.builder()
                    .categoryId(otherCategory.getCategoryId())
                    .name("Latte")
                    .price(new BigDecimal("40000"))
                    .build();

            when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
            when(categoryRepository.findById(otherCategory.getCategoryId())).thenReturn(Optional.of(otherCategory));

            assertThatThrownBy(() -> menuService.createItem(storeId, request))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getErrorCode()).isEqualTo(ErrorCode.MENU_ITEM_STORE_MISMATCH));
        }

        @Test
        @DisplayName("updateItem - success changing price and availability")
        void updateItem_Success() {
            UpdateMenuItemRequest request = UpdateMenuItemRequest.builder()
                    .name("Bac Xiu Sai Gon")
                    .price(new BigDecimal("39000"))
                    .isAvailable(false)
                    .build();

            when(itemRepository.findByItemIdAndStoreStoreId(itemId, storeId))
                    .thenReturn(Optional.of(item));
            when(itemRepository.save(any(MenuItem.class))).thenReturn(item);

            MenuItemResponse response = menuService.updateItem(storeId, itemId, request);

            verify(storeStaffService).requireStoreAccess(storeId, "OWNER", "MANAGER");
            assertThat(response.getName()).isEqualTo("Bac Xiu Sai Gon");
            assertThat(response.getPrice()).isEqualTo(new BigDecimal("39000"));
            assertThat(response.getIsAvailable()).isFalse();
        }

        @Test
        @DisplayName("updateItem - item not found throws MENU_ITEM_NOT_FOUND")
        void updateItem_NotFound_ThrowsException() {
            UpdateMenuItemRequest request = UpdateMenuItemRequest.builder()
                    .name("Cappuccino")
                    .build();

            when(itemRepository.findByItemIdAndStoreStoreId(itemId, storeId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> menuService.updateItem(storeId, itemId, request))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getErrorCode()).isEqualTo(ErrorCode.MENU_ITEM_NOT_FOUND));
        }

        @Test
        @DisplayName("deleteItem - success soft deletes item")
        void deleteItem_Success() {
            when(itemRepository.findByItemIdAndStoreStoreId(itemId, storeId))
                    .thenReturn(Optional.of(item));

            menuService.deleteItem(storeId, itemId);

            verify(storeStaffService).requireStoreAccess(storeId, "OWNER", "MANAGER");
            assertThat(item.isDeleted()).isTrue();
            assertThat(item.getDeletedAt()).isNotNull();
            verify(itemRepository).delete(item);
        }

        @Test
        @DisplayName("deleteItem - item not found throws MENU_ITEM_NOT_FOUND")
        void deleteItem_NotFound_ThrowsException() {
            when(itemRepository.findByItemIdAndStoreStoreId(itemId, storeId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> menuService.deleteItem(storeId, itemId))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getErrorCode()).isEqualTo(ErrorCode.MENU_ITEM_NOT_FOUND));
        }
    }
}
