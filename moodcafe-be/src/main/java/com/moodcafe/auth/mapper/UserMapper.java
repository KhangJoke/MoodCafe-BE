package com.moodcafe.auth.mapper;

import com.moodcafe.auth.dto.user.request.UserCommonRequest;
import com.moodcafe.auth.dto.user.response.UserResponse;
import com.moodcafe.auth.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(
        componentModel = "spring",
        uses = RoleMapper.class
)
public interface UserMapper {

    UserResponse toResponse(User user);

    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "emailVerified", ignore = true)
    @Mapping(target = "requirePasswordChange", ignore = true)
    @Mapping(target = "firstLogin", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateUser(
            UserCommonRequest request,
            @MappingTarget User user
    );
}