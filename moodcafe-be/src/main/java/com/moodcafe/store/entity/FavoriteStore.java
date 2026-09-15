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
        name = "favorite_stores",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_user_favorite_store", columnNames = {"user_id", "store_id"})
        },
        indexes = {
                @Index(name = "idx_favorite_stores_user_id", columnList = "user_id"),
                @Index(name = "idx_favorite_stores_store_id", columnList = "store_id")
        }
)
@SQLDelete(sql = "UPDATE favorite_stores SET is_deleted = true WHERE favorite_store_id = ?")
@SQLRestriction("is_deleted = false")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FavoriteStore {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "favorite_store_id", nullable = false, updatable = false)
    private UUID favoriteStoreId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_favorite_stores_user"))
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false, foreignKey = @ForeignKey(name = "fk_favorite_stores_store"))
    private Store store;

    @Builder.Default
    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
