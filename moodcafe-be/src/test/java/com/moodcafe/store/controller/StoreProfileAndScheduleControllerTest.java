package com.moodcafe.store.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.moodcafe.shared.exceptions.GlobalExceptionHandler;
import com.moodcafe.store.abstraction.service.StoreService;
import com.moodcafe.store.abstraction.service.StoreStaffService;
import com.moodcafe.store.dto.request.StoreScheduleRequest;
import com.moodcafe.store.dto.request.UpdateStoreRequest;
import com.moodcafe.store.dto.response.StoreProfileResponse;
import com.moodcafe.store.dto.response.StoreResponse;
import com.moodcafe.store.dto.response.StoreScheduleResponse;
import com.moodcafe.store.entity.enums.StoreStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class StoreProfileAndScheduleControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private StoreService storeService;

    @Mock
    private StoreStaffService storeStaffService;

    @InjectMocks
    private StoreController storeController;

    private UUID storeId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(storeController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        storeId = UUID.randomUUID();
    }

    @Test
    @DisplayName("GET /api/stores/{storeId}/profile - 200 OK with StoreProfileResponse")
    void getStoreProfile_Success() throws Exception {
        StoreProfileResponse response = StoreProfileResponse.builder()
                .storeId(storeId)
                .name("Vintage Coffee")
                .address("123 Pasteur, Quan 1")
                .status(StoreStatus.ACTIVE)
                .openingTime(LocalTime.of(7, 0))
                .closingTime(LocalTime.of(22, 0))
                .priceFrom(30000L)
                .priceTo(70000L)
                .schedules(List.of(
                        StoreScheduleResponse.builder()
                                .dayOfWeek(DayOfWeek.MONDAY)
                                .dayNameVi("Thứ Hai")
                                .openTime(LocalTime.of(7, 0))
                                .closeTime(LocalTime.of(22, 0))
                                .isOpen(true)
                                .build()
                ))
                .build();

        when(storeService.getStoreProfile(storeId)).thenReturn(response);

        mockMvc.perform(get("/api/stores/{storeId}/profile", storeId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.storeId").value(storeId.toString()))
                .andExpect(jsonPath("$.data.name").value("Vintage Coffee"))
                .andExpect(jsonPath("$.data.schedules[0].dayOfWeek").value("MONDAY"))
                .andExpect(jsonPath("$.data.schedules[0].dayNameVi").value("Thứ Hai"));
    }

    @Test
    @DisplayName("GET /api/stores/{storeId}/schedules - 200 OK with list of schedules")
    void getStoreSchedules_Success() throws Exception {
        List<StoreScheduleResponse> schedules = List.of(
                StoreScheduleResponse.builder()
                        .dayOfWeek(DayOfWeek.MONDAY)
                        .dayNameVi("Thứ Hai")
                        .openTime(LocalTime.of(8, 0))
                        .closeTime(LocalTime.of(22, 0))
                        .isOpen(true)
                        .build()
        );

        when(storeService.getStoreSchedules(storeId)).thenReturn(schedules);

        mockMvc.perform(get("/api/stores/{storeId}/schedules", storeId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].dayOfWeek").value("MONDAY"));
    }

    @Test
    @DisplayName("PUT /api/stores/{storeId} with schedules - 200 OK")
    void updateStore_WithSchedules_Success() throws Exception {
        UpdateStoreRequest request = UpdateStoreRequest.builder()
                .name("Updated Cafe")
                .schedules(List.of(
                        StoreScheduleRequest.builder()
                                .dayOfWeek(DayOfWeek.MONDAY)
                                .openTime(LocalTime.of(7, 0))
                                .closeTime(LocalTime.of(21, 0))
                                .isOpen(true)
                                .build()
                ))
                .build();

        StoreResponse response = StoreResponse.builder()
                .storeId(storeId)
                .name("Updated Cafe")
                .schedules(List.of(
                        StoreScheduleResponse.builder()
                                .dayOfWeek(DayOfWeek.MONDAY)
                                .dayNameVi("Thứ Hai")
                                .openTime(LocalTime.of(7, 0))
                                .closeTime(LocalTime.of(21, 0))
                                .isOpen(true)
                                .build()
                ))
                .build();

        when(storeService.updateStore(eq(storeId), any(UpdateStoreRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/stores/{storeId}", storeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.name").value("Updated Cafe"))
                .andExpect(jsonPath("$.data.schedules[0].dayOfWeek").value("MONDAY"));
    }
}
