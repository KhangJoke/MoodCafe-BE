package com.moodcafe.auth.mapper;

import com.moodcafe.auth.dto.role.response.RoleResponse;
import com.moodcafe.auth.entity.Role;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface RoleMapper {

    RoleResponse toResponse(Role role);
}
