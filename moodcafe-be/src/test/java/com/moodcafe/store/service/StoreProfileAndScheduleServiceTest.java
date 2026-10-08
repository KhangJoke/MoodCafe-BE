package com.moodcafe.store.service;

import com.moodcafe.auth.abstraction.service.CurrentUserService;
import com.moodcafe.configuration.abstraction.service.SystemConfigurationService;
import com.moodcafe.notification.abstraction.service.NotificationDispatcherService;
import com.moodcafe.shared.error.ErrorCode;
import com.moodcafe.shared.exceptions.AppException;
import com.moodcafe.store.abstraction.repository.FavoriteStoreRepository;
import com.moodcafe.store.abstraction.repository.StoreImageRepository;
import com.moodcafe.store.abstraction.repository.StoreRepository;
import com.moodcafe.store.abstraction.repository.StoreReviewRepository;
import com.moodcafe.store.abstraction.repository.StoreRoleRepository;
import com.moodcafe.store.abstraction.repository.StoreScheduleRepository;
import com.moodcafe.store.abstraction.repository.StoreStaffRepository;
import com.moodcafe.store.abstraction.repository.TagRatingRepository;
import com.moodcafe.store.abstraction.repository.VisitVerificationRepository;
import com.moodcafe.store.abstraction.service.StoreStaffService;
import com.moodcafe.store.dto.request.StoreScheduleRequest;
import com.moodcafe.store.dto.request.UpdateStoreRequest;
import com.moodcafe.store.dto.response.StoreProfileResponse;
import com.moodcafe.store.dto.response.StoreResponse;
import com.moodcafe.store.dto.response.StoreScheduleResponse;
import com.moodcafe.store.entity.Store;
import com.moodcafe.store.entity.StoreSchedule;
import com.moodcafe.store.entity.enums.StoreStatus;
import com.moodcafe.store.mapper.StoreImageMapper;
import com.moodcafe.store.mapper.StoreMapper;
import com.moodcafe.store.mapper.StoreReviewMapper;
import com.moodcafe.store.mapper.StoreScheduleMapper;
import com.moodcafe.subscription.abstraction.service.SubscriptionService;
import com.moodcafe.tag.abstraction.repository.StoreTagRepository;
import com.moodcafe.tag.abstraction.repository.TagCategoryRepository;
import com.moodcafe.tag.abstraction.repository.TagRepository;
import com.moodcafe.tag.abstraction.repository.UserPreferenceRepository;
import com.moodcafe.tag.mapper.StoreTagMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StoreProfileAndScheduleServiceTest {

    @Mock
    private StoreRepository storeRepository;
    @Mock
    private StoreRoleRepository storeRoleRepository;
    @Mock
    private StoreStaffRepository storeStaffRepository;
    @Mock
    private StoreImageRepository storeImageRepository;
    @Mock
    private StoreTagRepository storeTagRepository;
    @Mock
    private TagRepository tagRepository;
    @Mock
    private TagCategoryRepository tagCategoryRepository;
    @Mock
    private StoreMapper storeMapper;
    @Mock
    private StoreImageMapper storeImageMapper;
    @Mock
    private StoreTagMapper storeTagMapper;
    @Mock
    private StoreStaffService storeStaffService;
    @Mock
    private CurrentUserService currentUserService;
    @Mock
    private FavoriteStoreRepository favoriteStoreRepository;
    @Mock
    private StoreReviewRepository storeReviewRepository;
    @Mock
    private UserPreferenceRepository userPreferenceRepository;
    @Mock
    private SystemConfigurationService configurationService;
    @Mock
    private StoreReviewMapper storeReviewMapper;
    @Mock
    private TagRatingRepository tagRatingRepository;
    @Mock
    private VisitVerificationRepository visitVerificationRepository;
    @Mock
    private SubscriptionService subscriptionService;
    @Mock
    private NotificationDispatcherService notificationDispatcherService;
    @Mock
    private StoreScheduleRepository storeScheduleRepository;
    @Mock
    private StoreScheduleMapper storeScheduleMapper;

    @InjectMocks
    private StoreServiceImpl storeService;

    private UUID storeId;
    private Store store;

    @BeforeEach
    void setUp() {
        storeId = UUID.randomUUID();
        store = Store.builder()
                .storeId(storeId)
                .name("Acoustic Chill Cafe")
                .description("Không gian nhạc acoustic nhẹ nhàng")
                .address("100 Vo Thi Sau, Quan 3, TP.HCM")
                .latitude(new BigDecimal("10.7850"))
                .longitude(new BigDecimal("106.6900"))
                .openingTime(LocalTime.of(7, 30))
                .closingTime(LocalTime.of(22, 30))
                .priceFrom(35000L)
                .priceTo(65000L)
                .phone("0901234567")
                .email("acoustic@cafe.com")
                .status(StoreStatus.ACTIVE)
                .allowResubmit(true)
                .build();
    }

    @Test
    @DisplayName("getStoreProfile - Success: Returns store profile with images, tags, and fallback 7-day schedules")
    void getStoreProfile_Success() {
        when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
        when(storeImageRepository.findAllByStoreStoreId(storeId)).thenReturn(Collections.emptyList());
        when(storeTagRepository.findAllByStoreId(storeId)).thenReturn(Collections.emptyList());
        when(storeScheduleRepository.findAllByStoreStoreId(storeId)).thenReturn(Collections.emptyList());

        StoreProfileResponse profile = storeService.getStoreProfile(storeId);

        assertThat(profile).isNotNull();
        assertThat(profile.getStoreId()).isEqualTo(storeId);
        assertThat(profile.getName()).isEqualTo("Acoustic Chill Cafe");
        assertThat(profile.getOpeningTime()).isEqualTo(LocalTime.of(7, 30));
        assertThat(profile.getClosingTime()).isEqualTo(LocalTime.of(22, 30));
        assertThat(profile.getSchedules()).hasSize(7);
        assertThat(profile.getSchedules().get(0).getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);

        verify(storeStaffService).requireStoreAccess(storeId, "OWNER", "MANAGER");
    }

    @Test
    @DisplayName("updateStore with schedules - Success: Upserts daily schedules during store update")
    void updateStore_WithSchedules_Success() {
        when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
        when(storeRepository.save(any(Store.class))).thenReturn(store);
        when(storeScheduleRepository.findAllByStoreStoreId(storeId)).thenReturn(Collections.emptyList());

        StoreResponse mockResponse = StoreResponse.builder()
                .storeId(storeId)
                .name("Updated Cafe")
                .schedules(new ArrayList<>())
                .build();
        when(storeMapper.toResponse(store)).thenReturn(mockResponse);

        UpdateStoreRequest request = UpdateStoreRequest.builder()
                .name("Updated Cafe")
                .schedules(List.of(
                        StoreScheduleRequest.builder()
                                .dayOfWeek(DayOfWeek.MONDAY)
                                .openTime(LocalTime.of(8, 0))
                                .closeTime(LocalTime.of(22, 0))
                                .isOpen(true)
                                .build(),
                        StoreScheduleRequest.builder()
                                .dayOfWeek(DayOfWeek.SUNDAY)
                                .isOpen(false)
                                .build()
                ))
                .build();

        StoreResponse response = storeService.updateStore(storeId, request);

        assertThat(response).isNotNull();
        verify(storeStaffService).requireStoreAccess(storeId, "OWNER", "MANAGER");
        verify(storeScheduleRepository).saveAll(any());
    }

    @Test
    @DisplayName("updateStoreSchedules - Fails when open day lacks openTime or closeTime")
    void updateStoreSchedules_OpenDayMissingTime_ThrowsException() {
        when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));

        List<StoreScheduleRequest> requests = List.of(
                StoreScheduleRequest.builder()
                        .dayOfWeek(DayOfWeek.MONDAY)
                        .isOpen(true)
                        .openTime(null)
                        .closeTime(null)
                        .build()
        );

        assertThatThrownBy(() -> storeService.updateStoreSchedules(storeId, requests))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("đang mở cửa thì phải nhập giờ mở cửa và giờ đóng cửa");
    }

    @Test
    @DisplayName("getStoreSchedules - Returns saved custom schedules sorted Monday through Sunday")
    void getStoreSchedules_WithCustomSchedules_ReturnsSorted() {
        when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));

        StoreSchedule friSchedule = StoreSchedule.builder()
                .scheduleId(UUID.randomUUID())
                .store(store)
                .dayOfWeek(DayOfWeek.FRIDAY)
                .openTime(LocalTime.of(8, 0))
                .closeTime(LocalTime.of(23, 0))
                .isOpen(true)
                .build();
        StoreSchedule monSchedule = StoreSchedule.builder()
                .scheduleId(UUID.randomUUID())
                .store(store)
                .dayOfWeek(DayOfWeek.MONDAY)
                .openTime(LocalTime.of(7, 0))
                .closeTime(LocalTime.of(22, 0))
                .isOpen(true)
                .build();

        when(storeScheduleRepository.findAllByStoreStoreId(storeId)).thenReturn(List.of(friSchedule, monSchedule));
        when(storeScheduleMapper.toResponse(monSchedule)).thenReturn(StoreScheduleResponse.builder()
                .dayOfWeek(DayOfWeek.MONDAY)
                .dayNameVi("Thứ Hai")
                .openTime(LocalTime.of(7, 0))
                .closeTime(LocalTime.of(22, 0))
                .isOpen(true)
                .build());
        when(storeScheduleMapper.toResponse(friSchedule)).thenReturn(StoreScheduleResponse.builder()
                .dayOfWeek(DayOfWeek.FRIDAY)
                .dayNameVi("Thứ Sáu")
                .openTime(LocalTime.of(8, 0))
                .closeTime(LocalTime.of(23, 0))
                .isOpen(true)
                .build());

        List<StoreScheduleResponse> schedules = storeService.getStoreSchedules(storeId);

        assertThat(schedules).hasSize(2);
        assertThat(schedules.get(0).getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
        assertThat(schedules.get(1).getDayOfWeek()).isEqualTo(DayOfWeek.FRIDAY);
    }
}
