package com.moodcafe.store.entity;

import com.moodcafe.auth.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
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
    private LocalDateTime joinedAt = LocalDateTime.now();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
