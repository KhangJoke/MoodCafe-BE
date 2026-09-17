package com.moodcafe.store.abstraction.repository;

import com.moodcafe.store.entity.Store;
import com.moodcafe.store.entity.enums.StoreStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface StoreRepository extends JpaRepository<Store, UUID> {

    List<Store> findAllByStatus(StoreStatus status);

    @Query("SELECT DISTINCT s.district FROM Store s WHERE s.status = 'ACTIVE' AND s.district IS NOT NULL AND s.district <> '' ORDER BY s.district")
    List<String> findDistinctActiveDistricts();
}
