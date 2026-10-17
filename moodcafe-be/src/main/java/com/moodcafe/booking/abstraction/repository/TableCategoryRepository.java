package com.moodcafe.booking.abstraction.repository;

import com.moodcafe.booking.entity.TableCategory;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TableCategoryRepository extends JpaRepository<TableCategory, UUID> {

    List<TableCategory> findAllByStoreStoreId(UUID storeId);

    List<TableCategory> findAllByStoreStoreIdAndIsActiveTrue(UUID storeId);

    Optional<TableCategory> findByCategoryIdAndStoreStoreId(UUID categoryId, UUID storeId);

    /**
     * Khóa dòng TableCategory bằng Pessimistic Write Lock khi tạo đơn giữ bàn
     * để chống Race Condition khi nhiều người cùng đặt chiếc bàn cuối cùng.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT tc FROM TableCategory tc WHERE tc.categoryId = :categoryId")
    Optional<TableCategory> findByIdWithLock(@Param("categoryId") UUID categoryId);
}
