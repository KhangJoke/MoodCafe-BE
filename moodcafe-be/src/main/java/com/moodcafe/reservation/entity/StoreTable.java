package com.moodcafe.reservation.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "store_tables", indexes = {
        @Index(name = "idx_store_tables_store", columnList = "store_id, is_deleted"),
        @Index(name = "idx_store_tables_category", columnList = "category_id, is_deleted")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoreTable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "table_id", nullable = false, updatable = false)
    private UUID tableId;

    @Column(name = "store_id", nullable = false)
    private UUID storeId;

    @Column(name = "category_id", nullable = false)
    private UUID categoryId;

    @Column(name = "table_number", nullable = false, length = 50)
    private String tableNumber;

    /**
     * AVAILABLE, OCCUPIED, RESERVED, MAINTENANCE
     */
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private String status = "AVAILABLE";

    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
