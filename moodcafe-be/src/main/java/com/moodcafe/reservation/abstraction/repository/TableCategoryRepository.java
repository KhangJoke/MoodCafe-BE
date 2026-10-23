package com.moodcafe.reservation.abstraction.repository;

import com.moodcafe.reservation.entity.TableCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository("reservationTableCategoryRepository")
public interface TableCategoryRepository extends JpaRepository<TableCategory, UUID> {

    List<TableCategory> findByStoreIdAndIsActiveTrueAndIsDeletedFalse(UUID storeId);
}
