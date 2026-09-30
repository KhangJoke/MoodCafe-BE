package com.moodcafe.menu;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.moodcafe.menu.controller.StoreMenuController;
import com.moodcafe.menu.dto.request.CreateMenuCategoryRequest;
import com.moodcafe.menu.dto.request.CreateMenuItemRequest;
import com.moodcafe.menu.dto.request.UpdateMenuCategoryRequest;
import com.moodcafe.menu.dto.request.UpdateMenuItemRequest;
import com.moodcafe.menu.dto.response.MenuCategoryResponse;
import com.moodcafe.menu.dto.response.MenuItemResponse;
import com.moodcafe.menu.service.MenuService;
import com.moodcafe.shared.error.ErrorCode;
import com.moodcafe.shared.exceptions.AppException;
import com.moodcafe.shared.exceptions.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class StoreMenuControllerTest {

    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @Mock
    private MenuService menuService;

    @InjectMocks
    private StoreMenuController storeMenuController;

    private UUID storeId;
    private UUID categoryId;
    private UUID itemId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(storeMenuController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        storeId = UUID.randomUUID();
        categoryId = UUID.randomUUID();
        itemId = UUID.randomUUID();
    }

    @Test
    @DisplayName("GET /api/stores/{storeId}/menu/categories - 200 OK")
    void getCategories_Returns200AndList() throws Exception {
        MenuCategoryResponse cat = MenuCategoryResponse.builder()
                .categoryId(categoryId)
                .storeId(storeId)
                .name("Coffee")
                .displayOrder(1)
                .itemCount(3)
                .createdAt(Instant.now())
                .build();

        when(menuService.getCategories(storeId)).thenReturn(List.of(cat));

        mockMvc.perform(get("/api/stores/{storeId}/menu/categories", storeId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].name").value("Coffee"))
                .andExpect(jsonPath("$.data[0].itemCount").value(3));
    }

    @Test
    @DisplayName("POST /api/stores/{storeId}/menu/categories - 201 Created")
    void createCategory_Valid_Returns201() throws Exception {
        CreateMenuCategoryRequest request = CreateMenuCategoryRequest.builder()
                .name("Pastries")
                .displayOrder(2)
                .build();

        MenuCategoryResponse response = MenuCategoryResponse.builder()
                .categoryId(categoryId)
                .storeId(storeId)
                .name("Pastries")
                .displayOrder(2)
                .itemCount(0)
                .createdAt(Instant.now())
                .build();

        when(menuService.createCategory(eq(storeId), any(CreateMenuCategoryRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/stores/{storeId}/menu/categories", storeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.name").value("Pastries"));
    }

    @Test
    @DisplayName("POST /api/stores/{storeId}/menu/categories - 400 Bad Request on blank name")
    void createCategory_BlankName_Returns400() throws Exception {
        CreateMenuCategoryRequest request = CreateMenuCategoryRequest.builder()
                .name("")
                .build();

        mockMvc.perform(post("/api/stores/{storeId}/menu/categories", storeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value(ErrorCode.VALIDATION_ERROR.name()));
    }

    @Test
    @DisplayName("POST /api/stores/{storeId}/menu/categories - 409 Conflict when name exists")
    void createCategory_DuplicateName_Returns409() throws Exception {
        CreateMenuCategoryRequest request = CreateMenuCategoryRequest.builder()
                .name("Coffee")
                .build();

        when(menuService.createCategory(eq(storeId), any(CreateMenuCategoryRequest.class)))
                .thenThrow(new AppException(ErrorCode.MENU_CATEGORY_NAME_EXISTS));

        mockMvc.perform(post("/api/stores/{storeId}/menu/categories", storeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value(ErrorCode.MENU_CATEGORY_NAME_EXISTS.name()));
    }

    @Test
    @DisplayName("PATCH /api/stores/{storeId}/menu/categories/{categoryId} - 200 OK")
    void updateCategory_Valid_Returns200() throws Exception {
        UpdateMenuCategoryRequest request = UpdateMenuCategoryRequest.builder()
                .name("Cold Brew & Tea")
                .displayOrder(5)
                .build();

        MenuCategoryResponse response = MenuCategoryResponse.builder()
                .categoryId(categoryId)
                .storeId(storeId)
                .name("Cold Brew & Tea")
                .displayOrder(5)
                .itemCount(2)
                .build();

        when(menuService.updateCategory(eq(storeId), eq(categoryId), any(UpdateMenuCategoryRequest.class)))
                .thenReturn(response);

        mockMvc.perform(patch("/api/stores/{storeId}/menu/categories/{categoryId}", storeId, categoryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.name").value("Cold Brew & Tea"));
    }

    @Test
    @DisplayName("DELETE /api/stores/{storeId}/menu/categories/{categoryId} - 200 OK")
    void deleteCategory_Success_Returns200() throws Exception {
        doNothing().when(menuService).deleteCategory(storeId, categoryId);

        mockMvc.perform(delete("/api/stores/{storeId}/menu/categories/{categoryId}", storeId, categoryId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("DELETE /api/stores/{storeId}/menu/categories/{categoryId} - 409 Conflict when category has items")
    void deleteCategory_HasItems_Returns409() throws Exception {
        doThrow(new AppException(ErrorCode.CATEGORY_HAS_ITEMS))
                .when(menuService).deleteCategory(storeId, categoryId);

        mockMvc.perform(delete("/api/stores/{storeId}/menu/categories/{categoryId}", storeId, categoryId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value(ErrorCode.CATEGORY_HAS_ITEMS.name()));
    }

    @Test
    @DisplayName("GET /api/stores/{storeId}/menu/items - 200 OK with Page")
    void getItems_Returns200AndPage() throws Exception {
        MenuItemResponse itemRes = MenuItemResponse.builder()
                .itemId(itemId)
                .storeId(storeId)
                .categoryId(categoryId)
                .categoryName("Coffee")
                .name("Espresso")
                .price(new BigDecimal("30000"))
                .isAvailable(true)
                .build();

        PageImpl<MenuItemResponse> page = new PageImpl<>(List.of(itemRes), PageRequest.of(0, 20), 1);
        when(menuService.getItems(eq(storeId), any(), any(), any(), any())).thenReturn(page);

        mockMvc.perform(get("/api/stores/{storeId}/menu/items", storeId)
                        .param("q", "Espresso")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.content[0].name").value("Espresso"));
    }

    @Test
    @DisplayName("POST /api/stores/{storeId}/menu/items - 201 Created")
    void createItem_Valid_Returns201() throws Exception {
        CreateMenuItemRequest request = CreateMenuItemRequest.builder()
                .categoryId(categoryId)
                .name("Caramel Macchiato")
                .description("Sweet caramel")
                .price(new BigDecimal("45000"))
                .isAvailable(true)
                .build();

        MenuItemResponse response = MenuItemResponse.builder()
                .itemId(itemId)
                .storeId(storeId)
                .categoryId(categoryId)
                .categoryName("Coffee")
                .name("Caramel Macchiato")
                .price(new BigDecimal("45000"))
                .isAvailable(true)
                .build();

        when(menuService.createItem(eq(storeId), any(CreateMenuItemRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/stores/{storeId}/menu/items", storeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.name").value("Caramel Macchiato"));
    }

    @Test
    @DisplayName("POST /api/stores/{storeId}/menu/items - 400 Bad Request on negative price")
    void createItem_NegativePrice_Returns400() throws Exception {
        CreateMenuItemRequest request = CreateMenuItemRequest.builder()
                .categoryId(categoryId)
                .name("Negative Price Item")
                .price(new BigDecimal("-1000"))
                .build();

        mockMvc.perform(post("/api/stores/{storeId}/menu/items", storeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value(ErrorCode.VALIDATION_ERROR.name()));
    }

    @Test
    @DisplayName("PATCH /api/stores/{storeId}/menu/items/{itemId} - 200 OK")
    void updateItem_Valid_Returns200() throws Exception {
        UpdateMenuItemRequest request = UpdateMenuItemRequest.builder()
                .isAvailable(false)
                .build();

        MenuItemResponse response = MenuItemResponse.builder()
                .itemId(itemId)
                .storeId(storeId)
                .categoryId(categoryId)
                .name("Espresso")
                .price(new BigDecimal("30000"))
                .isAvailable(false)
                .build();

        when(menuService.updateItem(eq(storeId), eq(itemId), any(UpdateMenuItemRequest.class)))
                .thenReturn(response);

        mockMvc.perform(patch("/api/stores/{storeId}/menu/items/{itemId}", storeId, itemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.isAvailable").value(false));
    }

    @Test
    @DisplayName("DELETE /api/stores/{storeId}/menu/items/{itemId} - 200 OK")
    void deleteItem_Success_Returns200() throws Exception {
        doNothing().when(menuService).deleteItem(storeId, itemId);

        mockMvc.perform(delete("/api/stores/{storeId}/menu/items/{itemId}", storeId, itemId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
