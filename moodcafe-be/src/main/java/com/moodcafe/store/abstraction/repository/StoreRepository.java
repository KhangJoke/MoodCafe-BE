package com.moodcafe.store.abstraction.repository;

import com.moodcafe.store.entity.Store;
import com.moodcafe.store.entity.enums.StoreStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface StoreRepository extends JpaRepository<Store, UUID>, JpaSpecificationExecutor<Store> {

    List<Store> findAllByStatus(StoreStatus status);

    long countByStatus(StoreStatus status);

    @Query("SELECT s.status, COUNT(s) FROM Store s GROUP BY s.status")
    List<Object[]> countGroupedByStatus();
}
