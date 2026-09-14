package com.moodcafe.store.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "store_amenities",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_store_amenity", columnNames = {"store_id", "amenity_id"})
        },
        indexes = {
                @Index(name = "idx_store_amenities_store_id", columnList = "store_id"),
                @Index(name = "idx_store_amenities_amenity_id", columnList = "amenity_id")
        }
)
@SQLDelete(sql = "UPDATE store_amenities SET is_deleted = true WHERE store_amenity_id = ?")
@SQLRestriction("is_deleted = false")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoreAmenity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "store_amenity_id", nullable = false, updatable = false)
    private UUID storeAmenityId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false, foreignKey = @ForeignKey(name = "fk_store_amenities_store"))
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "amenity_id", nullable = false, foreignKey = @ForeignKey(name = "fk_store_amenities_amenity"))
    private Amenity amenity;

    @Builder.Default
    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
