package com.moodcafe.booking.abstraction.repository;

import com.moodcafe.booking.entity.StoreBookingConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface StoreBookingConfigRepository extends JpaRepository<StoreBookingConfig, UUID> {

    Optional<StoreBookingConfig> findByStoreStoreId(UUID storeId);

    boolean existsByStoreStoreId(UUID storeId);
}
