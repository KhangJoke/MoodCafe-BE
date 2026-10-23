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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity(name = "ReservationTableCategory")
@Table(name = "table_categories", indexes = {
        @Index(name = "idx_table_categories_store", columnList = "store_id, is_active, is_deleted")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TableCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "category_id", nullable = false, updatable = false)
    private UUID categoryId;

    @Column(name = "store_id", nullable = false)
    private UUID storeId;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "max_capacity", nullable = false)
    @Builder.Default
    private Integer capacity = 2;

    @JdbcTypeCode(SqlTypes.BIGINT)
    @Column(name = "min_spend_amount", nullable = false)
    @Builder.Default
    private BigDecimal minSpendAmount = new BigDecimal("50000.00");

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

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
