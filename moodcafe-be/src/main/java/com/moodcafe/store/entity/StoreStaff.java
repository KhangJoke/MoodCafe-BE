package com.moodcafe.store.entity;

import com.moodcafe.auth.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "store_staffs",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_store_staff", columnNames = {"store_id", "user_id"})
        },
        indexes = {
                @Index(name = "idx_store_staffs_store_id", columnList = "store_id"),
                @Index(name = "idx_store_staffs_user_id", columnList = "user_id"),
                @Index(name = "idx_store_staffs_role_id", columnList = "store_role_id")
        }
)
@SQLDelete(sql = "UPDATE store_staffs SET is_deleted = true WHERE store_staff_id = ?")
@SQLRestriction("is_deleted = false")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoreStaff {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "store_staff_id", nullable = false, updatable = false)
    private UUID storeStaffId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false, foreignKey = @ForeignKey(name = "fk_store_staffs_store"))
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_store_staffs_user"))
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_role_id", nullable = false, foreignKey = @ForeignKey(name = "fk_store_staffs_role"))
    private StoreRole storeRole;

    @Builder.Default
    @Column(name = "status", nullable = false, length = 30)
    private String status = "ACTIVE";

    @Builder.Default
    @Column(name = "joined_at", nullable = false)
    private Instant joinedAt = Instant.now();

    @Builder.Default
    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
