package com.moodcafe.store.abstraction.repository;

import com.moodcafe.store.entity.StoreSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StoreScheduleRepository extends JpaRepository<StoreSchedule, UUID> {

    List<StoreSchedule> findAllByStoreStoreId(UUID storeId);

    Optional<StoreSchedule> findByStoreStoreIdAndDayOfWeek(UUID storeId, DayOfWeek dayOfWeek);

    void deleteAllByStoreStoreId(UUID storeId);
}
