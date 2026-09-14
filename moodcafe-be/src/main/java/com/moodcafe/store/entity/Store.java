package com.moodcafe.store.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(
        name = "stores",
        indexes = {
                @Index(name = "idx_stores_status", columnList = "status"),
                @Index(name = "idx_stores_location", columnList = "latitude, longitude")
        }
)
@SQLDelete(sql = "UPDATE stores SET is_deleted = true WHERE store_id = ?")
@SQLRestriction("is_deleted = false")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Store {

        @Id
        @GeneratedValue(strategy = GenerationType.UUID)
        @Column(name = "store_id", nullable = false, updatable = false)
        private UUID storeId;

        @Column(name = "name", nullable = false, length = 255)
        private String name;

        @Column(name = "description", columnDefinition = "TEXT")
        private String description;

        @Column(name = "address", nullable = false, length = 500)
        private String address;

        @Column(name = "latitude", precision = 10, scale = 7)
        private BigDecimal latitude;

        @Column(name = "longitude", precision = 10, scale = 7)
        private BigDecimal longitude;

        @Column(name = "opening_time")
        private LocalTime openingTime;

        @Column(name = "closing_time")
        private LocalTime closingTime;

        @Column(name = "price_range", length = 50)
        private String priceRange;

        @Column(name = "phone", length = 20)
        private String phone;

        @Column(name = "email", length = 255)
        private String email;

        @Builder.Default
        @Column(name = "status", nullable = false, length = 30)
        private String status = "PENDING";

        @Builder.Default
        @Column(name = "is_deleted", nullable = false)
        private boolean isDeleted = false;

        @CreationTimestamp
        @Column(name = "created_at", nullable = false, updatable = false)
        private LocalDateTime createdAt;

        @UpdateTimestamp
        @Column(name = "updated_at", nullable = false)
        private LocalDateTime updatedAt;
    }
