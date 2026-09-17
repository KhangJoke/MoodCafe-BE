package com.moodcafe.configuration.abstraction.repository;

import com.moodcafe.configuration.entity.SystemConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SystemConfigurationRepository extends JpaRepository<SystemConfiguration, UUID> {

    Optional<SystemConfiguration> findByConfigKey(String configKey);

    List<SystemConfiguration> findAllByConfigGroupOrderByConfigKeyAsc(String configGroup);

    List<SystemConfiguration> findAllByIsPublicTrueOrderByConfigGroupAsc();
}
