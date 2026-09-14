package com.moodcafe.auth.abstraction.service;

import com.moodcafe.auth.entity.Role;

public interface RoleService {

    Role getRoleByName(String name);
}
