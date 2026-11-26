package com.moodcafe.menu;

import com.moodcafe.menu.dto.request.CreateMenuItemRequest;
import com.moodcafe.menu.dto.request.MenuItemSizeRequest;
import com.moodcafe.menu.dto.request.UpdateMenuItemRequest;
import com.moodcafe.menu.dto.response.MenuItemResponse;
import com.moodcafe.menu.entity.MenuCategory;
import com.moodcafe.menu.entity.MenuItem;
import com.moodcafe.menu.entity.MenuItemSize;
import com.moodcafe.menu.repository.MenuCategoryRepository;
import com.moodcafe.menu.repository.MenuItemRepository;
import com.moodcafe.menu.service.MenuServiceImpl;
import com.moodcafe.shared.exceptions.AppException;
import com.moodcafe.store.abstraction.repository.StoreRepository;
import com.moodcafe.store.abstraction.service.StoreStaffService;
import com.moodcafe.store.entity.Store;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MenuItemSizeServiceTest {

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

    @BeforeEach
    void setUp() {
        storeId = UUID.randomUUID();
        store = Store.builder().storeId(storeId).name("Mood Cafe").build();
        categoryId = UUID.randomUUID();
        category = MenuCategory.builder().categoryId(categoryId).store(store).name("Coffee").build();
    }

    @Test
    @DisplayName("createItem with sizes S/M/L computes price from default size and returns minPrice/maxPrice")
    void createItem_WithSizes_Success() {
        when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(itemRepository.save(any(MenuItem.class))).thenAnswer(inv -> {
            MenuItem saved = inv.getArgument(0);
            saved.setItemId(UUID.randomUUID());
            saved.getSizes().forEach(s -> s.setSizeId(UUID.randomUUID()));
            return saved;
        });

        CreateMenuItemRequest req = CreateMenuItemRequest.builder()
                .categoryId(categoryId)
                .name("Trà Đào Cam Sả")
                .sizes(List.of(
                        MenuItemSizeRequest.builder().sizeName("S").price(new BigDecimal("35000")).displayOrder(0).build(),
                        MenuItemSizeRequest.builder().sizeName("M").price(new BigDecimal("42000")).displayOrder(1).isDefault(true).build(),
                        MenuItemSizeRequest.builder().sizeName("L").price(new BigDecimal("49000")).displayOrder(2).build()
                ))
                .build();

        MenuItemResponse res = menuService.createItem(storeId, req);

        assertThat(res.getHasSizes()).isTrue();
        assertThat(res.getPrice()).isEqualByComparingTo("42000"); // default size M
        assertThat(res.getMinPrice()).isEqualByComparingTo("35000");
        assertThat(res.getMaxPrice()).isEqualByComparingTo("49000");
        assertThat(res.getSizes()).hasSize(3);
        assertThat(res.getSizes().get(1).getIsDefault()).isTrue();
    }

    @Test
    @DisplayName("createItem with duplicate size names (case-insensitive) throws BAD_REQUEST")
    void createItem_DuplicateSizeName_Throws() {
        when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));

        CreateMenuItemRequest req = CreateMenuItemRequest.builder()
                .categoryId(categoryId)
                .name("Latte")
                .sizes(List.of(
                        MenuItemSizeRequest.builder().sizeName("M").price(new BigDecimal("40000")).build(),
                        MenuItemSizeRequest.builder().sizeName("m").price(new BigDecimal("45000")).build()
                ))
                .build();

        assertThatThrownBy(() -> menuService.createItem(storeId, req))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("Trùng tên size");
    }

    @Test
    @DisplayName("createItem without sizes and without price throws BAD_REQUEST")
    void createItem_NoPriceAndNoSizes_Throws() {
        when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));

        CreateMenuItemRequest req = CreateMenuItemRequest.builder()
                .categoryId(categoryId)
                .name("Bánh Croissant")
                .build();

        assertThatThrownBy(() -> menuService.createItem(storeId, req))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("Giá món không được để trống");
    }

    @Test
    @DisplayName("updateItem with sizes=null preserves existing sizes; sizes=[] clears sizes")
    void updateItem_NullPreservesAndEmptyClearsSizes() {
        UUID itemId = UUID.randomUUID();
        MenuItem existing = MenuItem.builder()
                .itemId(itemId)
                .store(store)
                .category(category)
                .name("Cà phê sữa")
                .price(new BigDecimal("35000"))
                .isAvailable(true)
                .sizes(new ArrayList<>())
                .build();
        existing.getSizes().add(MenuItemSize.builder()
                .sizeId(UUID.randomUUID())
                .menuItem(existing)
                .sizeName("M")
                .price(new BigDecimal("35000"))
                .isDefault(true)
                .isAvailable(true)
                .build());

        when(itemRepository.findByItemIdAndStoreStoreId(itemId, storeId)).thenReturn(Optional.of(existing));
        when(itemRepository.save(any(MenuItem.class))).thenAnswer(inv -> inv.getArgument(0));

        // 1. Update availability only (sizes = null) -> keeps existing sizes
        MenuItemResponse res1 = menuService.updateItem(storeId, itemId, UpdateMenuItemRequest.builder()
                .isAvailable(false)
                .build());
        assertThat(res1.getHasSizes()).isTrue();
        assertThat(res1.getSizes()).hasSize(1);

        // 2. Clear sizes (sizes = []) and set single price -> converts to single-price item
        MenuItemResponse res2 = menuService.updateItem(storeId, itemId, UpdateMenuItemRequest.builder()
                .price(new BigDecimal("30000"))
                .sizes(List.of())
                .build());
        assertThat(res2.getHasSizes()).isFalse();
        assertThat(res2.getSizes()).isEmpty();
        assertThat(res2.getPrice()).isEqualByComparingTo("30000");
        assertThat(res2.getMinPrice()).isEqualByComparingTo("30000");
        assertThat(res2.getMaxPrice()).isEqualByComparingTo("30000");
    }
}
