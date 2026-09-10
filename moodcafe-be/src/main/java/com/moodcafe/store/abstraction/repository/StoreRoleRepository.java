package com.moodcafe.store.abstraction.repository;

import com.moodcafe.store.entity.StoreRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface StoreRoleRepository extends JpaRepository<StoreRole, UUID> {

    Optional<StoreRole> findByName(String name);
}
