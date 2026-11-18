package com.moodcafe.store.service;

import com.moodcafe.auth.abstraction.repository.UserRepository;
import com.moodcafe.auth.abstraction.service.CurrentUserService;
import com.moodcafe.auth.entity.Role;
import com.moodcafe.auth.entity.User;
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
import com.moodcafe.store.dto.request.AdminStoreImportItemRequest;
import com.moodcafe.store.dto.request.AdminStoreImportRequest;
import com.moodcafe.store.dto.response.AdminStoreImportResponse;
import com.moodcafe.store.dto.response.AdminStoreImportResponse.ImportOutcome;
import com.moodcafe.store.entity.Store;
import com.moodcafe.store.entity.StoreImage;
import com.moodcafe.store.entity.StoreRole;
import com.moodcafe.store.entity.StoreSchedule;
import com.moodcafe.store.entity.StoreStaff;
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
import com.moodcafe.tag.entity.StoreTag;
import com.moodcafe.tag.entity.Tag;
import com.moodcafe.tag.entity.TagCategory;
import com.moodcafe.tag.entity.enums.ControlType;
import com.moodcafe.tag.entity.enums.StoreTagStatus;
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
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StoreServiceImportTest {

    @Mock private StoreRepository storeRepository;
    @Mock private StoreRoleRepository storeRoleRepository;
    @Mock private StoreStaffRepository storeStaffRepository;
    @Mock private StoreImageRepository storeImageRepository;
    @Mock private StoreTagRepository storeTagRepository;
    @Mock private TagRepository tagRepository;
    @Mock private TagCategoryRepository tagCategoryRepository;
    @Mock private StoreMapper storeMapper;
    @Mock private StoreImageMapper storeImageMapper;
    @Mock private StoreTagMapper storeTagMapper;
    @Mock private StoreStaffService storeStaffService;
    @Mock private CurrentUserService currentUserService;
    @Mock private FavoriteStoreRepository favoriteStoreRepository;
    @Mock private StoreReviewRepository storeReviewRepository;
    @Mock private UserPreferenceRepository userPreferenceRepository;
    @Mock private SystemConfigurationService configurationService;
    @Mock private StoreReviewMapper storeReviewMapper;
    @Mock private TagRatingRepository tagRatingRepository;
    @Mock private VisitVerificationRepository visitVerificationRepository;
    @Mock private SubscriptionService subscriptionService;
    @Mock private NotificationDispatcherService notificationDispatcherService;
    @Mock private StoreScheduleRepository storeScheduleRepository;
    @Mock private StoreScheduleMapper storeScheduleMapper;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private StoreServiceImpl storeService;

    private User ownerUser;
    private StoreRole ownerRole;
    private Tag noiseQuiet;
    private Tag noiseLively;
    private Tag vibeMinimal;
    private Tag purposeDeadline;
    private Tag amenityWifi;
    private Tag amenitySocket;

    @BeforeEach
    void setUp() {
        Role customerRole = Role.builder().roleId(UUID.randomUUID()).name("CUSTOMER").build();
        ownerUser = User.builder()
                .userId(UUID.randomUUID())
                .email("owner1@moodcafe.com")
                .fullName("Owner One")
                .role(customerRole)
                .active(true)
                .isDeleted(false)
                .build();

        ownerRole = StoreRole.builder()
                .storeRoleId(UUID.randomUUID())
                .name("OWNER")
                .build();

        TagCategory noiseCat = TagCategory.builder().tagCategoryId(UUID.randomUUID()).code("NOISE").name("Độ yên tĩnh & Âm thanh").controlType(ControlType.SLIDER).build();
        TagCategory vibeCat = TagCategory.builder().tagCategoryId(UUID.randomUUID()).code("VIBE").name("Vibe & Phong cách").controlType(ControlType.TAG_LIST).build();
        TagCategory purposeCat = TagCategory.builder().tagCategoryId(UUID.randomUUID()).code("PURPOSE").name("Mục đích sử dụng").controlType(ControlType.TAG_LIST).build();
        TagCategory amenityCat = TagCategory.builder().tagCategoryId(UUID.randomUUID()).code("AMENITY").name("Tiện ích quán").controlType(ControlType.TAG_LIST).build();

        noiseQuiet = Tag.builder().tagId(UUID.randomUUID()).name("Yên tĩnh").category(noiseCat).active(true).build();
        noiseLively = Tag.builder().tagId(UUID.randomUUID()).name("Sôi động").category(noiseCat).active(true).build();
        vibeMinimal = Tag.builder().tagId(UUID.randomUUID()).name("Tối giản (Minimalism)").category(vibeCat).active(true).build();
        purposeDeadline = Tag.builder().tagId(UUID.randomUUID()).name("Học bài / Chạy deadline").category(purposeCat).active(true).build();
        amenityWifi = Tag.builder().tagId(UUID.randomUUID()).name("Wifi").category(amenityCat).active(true).build();
        amenitySocket = Tag.builder().tagId(UUID.randomUUID()).name("Ổ cắm điện").category(amenityCat).active(true).build();

        lenient().when(storeRoleRepository.findByName("OWNER")).thenReturn(Optional.of(ownerRole));
        lenient().when(userRepository.findByEmail("owner1@moodcafe.com")).thenReturn(Optional.of(ownerUser));
        lenient().when(tagRepository.findAllByActiveTrue()).thenReturn(
                List.of(noiseQuiet, noiseLively, vibeMinimal, purposeDeadline, amenityWifi, amenitySocket)
        );
        lenient().when(storeRepository.findAllNormalizedNameAndAddress()).thenReturn(List.of());
        lenient().when(storeRepository.save(any(Store.class))).thenAnswer(inv -> {
            Store s = inv.getArgument(0);
            if (s.getStoreId() == null) s.setStoreId(UUID.randomUUID());
            return s;
        });
    }

    @Test
    @DisplayName("importStoresAdmin - Creates ACTIVE store with APPROVED tags (max 4 highlighted), images, 7-day schedules, and OWNER staff")
    void importStoresAdmin_Success() {
        AdminStoreImportItemRequest item = AdminStoreImportItemRequest.builder()
                .rowNumber(2)
                .name("  Katinat Đồng Khởi  ")
                .address("123 Đồng Khởi, Bến Nghé, Quận 1")
                .district("Quận 1")
                .latitude(new BigDecimal("10.7745123"))
                .longitude(new BigDecimal("106.7032456"))
                .openingTime(LocalTime.of(7, 0))
                .closingTime(LocalTime.of(22, 30))
                .priceFrom(45000L)
                .priceTo(85000L)
                .imageUrls(List.of(
                        "https://res.cloudinary.com/dy45rrkhf/image/upload/v1/moodcafe/stores/1.jpg",
                        "https://res.cloudinary.com/dy45rrkhf/image/upload/v1/moodcafe/stores/2.jpg"
                ))
                .tagNames(List.of("yên tĩnh", "Tối giản (Minimalism)", "Học bài / Chạy deadline", "Wifi", "Ổ cắm điện"))
                .build();

        AdminStoreImportRequest req = AdminStoreImportRequest.builder()
                .targetStatus(StoreStatus.ACTIVE)
                .skipDuplicates(true)
                .defaultOwnerEmail("owner1@moodcafe.com")
                .items(List.of(item))
                .build();

        AdminStoreImportResponse resp = storeService.importStoresAdmin(req);

        assertThat(resp.getTotalSubmitted()).isEqualTo(1);
        assertThat(resp.getCreatedCount()).isEqualTo(1);
        assertThat(resp.getSkippedCount()).isEqualTo(0);
        assertThat(resp.getFailedCount()).isEqualTo(0);
        assertThat(resp.getResults().get(0).getOutcome()).isEqualTo(ImportOutcome.CREATED);

        ArgumentCaptor<StoreImage> imageCaptor = ArgumentCaptor.forClass(StoreImage.class);
        verify(storeImageRepository, times(2)).save(imageCaptor.capture());
        assertThat(imageCaptor.getAllValues().get(0).isPrimary()).isTrue();
        assertThat(imageCaptor.getAllValues().get(1).isPrimary()).isFalse();

        ArgumentCaptor<StoreTag> tagCaptor = ArgumentCaptor.forClass(StoreTag.class);
        verify(storeTagRepository, times(5)).save(tagCaptor.capture());
        long highlightedCount = tagCaptor.getAllValues().stream().filter(StoreTag::isHighlighted).count();
        assertThat(highlightedCount).isEqualTo(4);
        assertThat(tagCaptor.getAllValues()).allMatch(st -> st.getStatus() == StoreTagStatus.APPROVED);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<StoreSchedule>> scheduleCaptor = ArgumentCaptor.forClass(List.class);
        verify(storeScheduleRepository).saveAll(scheduleCaptor.capture());
        assertThat(scheduleCaptor.getValue()).hasSize(7);

        verify(storeStaffRepository).save(any(StoreStaff.class));
    }

    @Test
    @DisplayName("importStoresAdmin - Rejects missing owner, unknown tags, multiple NOISE tags, and skips duplicates cleanly")
    void importStoresAdmin_ValidationAndIsolation() {
        when(storeRepository.findAllNormalizedNameAndAddress()).thenReturn(
                List.<Object[]>of(new Object[]{"existing cafe", "99 lê lợi, quận 1"})
        );

        AdminStoreImportItemRequest dupItem = AdminStoreImportItemRequest.builder()
                .rowNumber(2)
                .name("Existing Cafe")
                .address("99 Lê Lợi, Quận 1")
                .latitude(new BigDecimal("10.77"))
                .longitude(new BigDecimal("106.70"))
                .tagNames(List.of("Wifi"))
                .build();

        AdminStoreImportItemRequest missingOwnerItem = AdminStoreImportItemRequest.builder()
                .rowNumber(3)
                .name("No Owner Cafe")
                .address("10 Nguyễn Huệ, Quận 1")
                .latitude(new BigDecimal("10.77"))
                .longitude(new BigDecimal("106.70"))
                .tagNames(List.of("Wifi"))
                .build();

        AdminStoreImportItemRequest multiNoiseItem = AdminStoreImportItemRequest.builder()
                .rowNumber(4)
                .name("Multi Noise Cafe")
                .address("20 Nguyễn Huệ, Quận 1")
                .latitude(new BigDecimal("10.77"))
                .longitude(new BigDecimal("106.70"))
                .ownerEmail("owner1@moodcafe.com")
                .tagNames(List.of("Yên tĩnh", "Sôi động"))
                .build();

        AdminStoreImportItemRequest badTagItem = AdminStoreImportItemRequest.builder()
                .rowNumber(5)
                .name("Bad Tag Cafe")
                .address("30 Nguyễn Huệ, Quận 1")
                .latitude(new BigDecimal("10.77"))
                .longitude(new BigDecimal("106.70"))
                .ownerEmail("owner1@moodcafe.com")
                .tagNames(List.of("Tag Khong Ton Tai"))
                .build();

        AdminStoreImportItemRequest nonCloudinaryImageItem = AdminStoreImportItemRequest.builder()
                .rowNumber(6)
                .name("External Image Cafe")
                .address("35 Nguyễn Huệ, Quận 1")
                .latitude(new BigDecimal("10.77"))
                .longitude(new BigDecimal("106.70"))
                .ownerEmail("owner1@moodcafe.com")
                .imageUrls(List.of("https://images.unsplash.com/photo-1501339847302?w=800"))
                .tagNames(List.of("Wifi"))
                .build();

        AdminStoreImportItemRequest validItem = AdminStoreImportItemRequest.builder()
                .rowNumber(7)
                .name("Valid Cafe")
                .address("40 Nguyễn Huệ, Quận 1")
                .latitude(new BigDecimal("10.77"))
                .longitude(new BigDecimal("106.70"))
                .ownerEmail("owner1@moodcafe.com")
                .tagNames(List.of("Yên tĩnh", "Wifi"))
                .build();

        AdminStoreImportRequest req = AdminStoreImportRequest.builder()
                .targetStatus(StoreStatus.ACTIVE)
                .skipDuplicates(true)
                .items(List.of(dupItem, missingOwnerItem, multiNoiseItem, badTagItem, nonCloudinaryImageItem, validItem))
                .build();

        AdminStoreImportResponse resp = storeService.importStoresAdmin(req);

        assertThat(resp.getTotalSubmitted()).isEqualTo(6);
        assertThat(resp.getSkippedCount()).isEqualTo(1);
        assertThat(resp.getFailedCount()).isEqualTo(4);
        assertThat(resp.getCreatedCount()).isEqualTo(1);
        assertThat(resp.getResults().get(0).getOutcome()).isEqualTo(ImportOutcome.SKIPPED);
        assertThat(resp.getResults().get(1).getOutcome()).isEqualTo(ImportOutcome.FAILED);
        assertThat(resp.getResults().get(1).getMessage()).contains("Thiếu email chủ quán");
        assertThat(resp.getResults().get(2).getOutcome()).isEqualTo(ImportOutcome.FAILED);
        assertThat(resp.getResults().get(2).getMessage()).contains("NOISE");
        assertThat(resp.getResults().get(3).getOutcome()).isEqualTo(ImportOutcome.FAILED);
        assertThat(resp.getResults().get(3).getMessage()).contains("Tag Khong Ton Tai");
        assertThat(resp.getResults().get(4).getOutcome()).isEqualTo(ImportOutcome.FAILED);
        assertThat(resp.getResults().get(4).getMessage()).contains("Cloudinary");
        assertThat(resp.getResults().get(5).getOutcome()).isEqualTo(ImportOutcome.CREATED);
    }

    @Test
    @DisplayName("importStoresAdmin - Throws FORBIDDEN when non-admin user calls it")
    void importStoresAdmin_RequiresAdmin() {
        org.mockito.Mockito.doThrow(new AppException(ErrorCode.FORBIDDEN))
                .when(currentUserService).requireSystemAdmin();

        AdminStoreImportRequest req = AdminStoreImportRequest.builder()
                .items(List.of(AdminStoreImportItemRequest.builder()
                        .name("Test")
                        .address("Addr")
                        .latitude(BigDecimal.TEN)
                        .longitude(BigDecimal.TEN)
                        .build()))
                .build();

        assertThatThrownBy(() -> storeService.importStoresAdmin(req))
                .isInstanceOf(AppException.class);
    }
}
