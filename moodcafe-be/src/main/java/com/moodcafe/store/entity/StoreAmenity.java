package com.moodcafe.store.entity;

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

import java.time.Instant;
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

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
