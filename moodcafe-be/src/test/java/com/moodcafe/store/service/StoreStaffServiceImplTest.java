package com.moodcafe.store.service;

import com.moodcafe.auth.abstraction.repository.RoleRepository;
import com.moodcafe.auth.abstraction.repository.UserRepository;
import com.moodcafe.auth.abstraction.service.CurrentUserService;
import com.moodcafe.auth.entity.Role;
import com.moodcafe.auth.entity.User;
import com.moodcafe.shared.error.ErrorCode;
import com.moodcafe.shared.exceptions.AppException;
import com.moodcafe.store.abstraction.repository.StoreRepository;
import com.moodcafe.store.abstraction.repository.StoreRoleRepository;
import com.moodcafe.store.abstraction.repository.StoreStaffRepository;
import com.moodcafe.store.dto.request.CreateStaffAccountRequest;
import com.moodcafe.store.dto.response.StoreStaffResponse;
import com.moodcafe.store.entity.Store;
import com.moodcafe.store.entity.StoreRole;
import com.moodcafe.store.entity.StoreStaff;
import com.moodcafe.store.mapper.StoreStaffMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StoreStaffServiceImplTest {

    @Mock
    private StoreStaffRepository storeStaffRepository;

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private StoreRoleRepository storeRoleRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private StoreStaffMapper storeStaffMapper;

    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private StoreStaffServiceImpl storeStaffService;


    private User ownerUser;
    private Store store;
    private StoreRole ownerRole;
    private StoreRole staffRole;
    private Role merchantStaffSystemRole;
    private UUID storeId;

    @BeforeEach
    void setUp() {
        storeId = UUID.randomUUID();

        ownerUser = User.builder()
                .userId(UUID.randomUUID())
                .email("owner@moodcafe.com")
                .userName("Owner User")
                .role(Role.builder().name("CUSTOMER").build())
                .build();

        store = Store.builder()
                .storeId(storeId)
                .name("MoodCafe Branch 1")
                .build();

        ownerRole = StoreRole.builder()
                .storeRoleId(UUID.randomUUID())
                .name("OWNER")
                .build();

        staffRole = StoreRole.builder()
                .storeRoleId(UUID.randomUUID())
                .name("CASHIER")
                .build();

        merchantStaffSystemRole = Role.builder()
                .roleId(UUID.randomUUID())
                .name("MERCHANT_STAFF")
                .build();

        when(currentUserService.getCurrentUser()).thenReturn(ownerUser);
        when(currentUserService.isSystemAdmin()).thenReturn(false);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("createStaffAccount should succeed when executed by store owner with CASHIER role")
    void createStaffAccount_Success() {
        CreateStaffAccountRequest request = new CreateStaffAccountRequest();
        request.setName("Cashier Member");
        request.setEmail("cashier@moodcafe.com");
        request.setPassword("Password123@");
        request.setStoreRole("CASHIER");

        when(storeStaffRepository.findByStoreStoreIdAndUserUserId(storeId, ownerUser.getUserId()))
                .thenReturn(Optional.of(StoreStaff.builder().store(store).user(ownerUser).storeRole(ownerRole).build()));
        when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
        when(userRepository.existsByEmail("cashier@moodcafe.com")).thenReturn(false);
        when(roleRepository.findByName("MERCHANT_STAFF")).thenReturn(Optional.of(merchantStaffSystemRole));
        when(passwordEncoder.encode(request.getPassword())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(storeRoleRepository.findByName("CASHIER")).thenReturn(Optional.of(staffRole));
        when(storeStaffRepository.save(any(StoreStaff.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StoreStaffResponse mockResponse = new StoreStaffResponse();
        mockResponse.setStoreRole("CASHIER");
        when(storeStaffMapper.toResponse(any(StoreStaff.class))).thenReturn(mockResponse);

        StoreStaffResponse result = storeStaffService.createStaffAccount(storeId, request);

        assertThat(result).isNotNull();
        assertThat(result.getStoreRole()).isEqualTo("CASHIER");
        verify(userRepository).save(any(User.class));
        verify(storeStaffRepository).save(any(StoreStaff.class));
    }

    @Test
    @DisplayName("createStaffAccount should throw exception when email already exists")
    void createStaffAccount_WhenEmailExists_ThrowsException() {
        CreateStaffAccountRequest request = new CreateStaffAccountRequest();
        request.setName("Staff Member");
        request.setEmail("existing@moodcafe.com");
        request.setPassword("Password123@");
        request.setStoreRole("STAFF");

        when(storeStaffRepository.findByStoreStoreIdAndUserUserId(storeId, ownerUser.getUserId()))
                .thenReturn(Optional.of(StoreStaff.builder().store(store).user(ownerUser).storeRole(ownerRole).build()));
        when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
        when(userRepository.existsByEmail("existing@moodcafe.com")).thenReturn(true);

        assertThatThrownBy(() -> storeStaffService.createStaffAccount(storeId, request))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("Email đã tồn tại");

        verify(userRepository, never()).save(any(User.class));
        verify(storeStaffRepository, never()).save(any(StoreStaff.class));
    }

    @Test
    @DisplayName("requireStoreAccess should throw FORBIDDEN when user has no role in store")
    void requireStoreAccess_WhenUserHasNoAccess_ThrowsForbidden() {
        when(storeStaffRepository.findByStoreStoreIdAndUserUserId(storeId, ownerUser.getUserId()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> storeStaffService.requireStoreAccess(storeId, "OWNER"))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FORBIDDEN_STORE_ACCESS);
    }
}

