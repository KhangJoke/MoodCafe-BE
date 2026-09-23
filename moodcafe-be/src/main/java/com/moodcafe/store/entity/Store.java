package com.moodcafe.store.entity;

import com.moodcafe.store.entity.enums.StoreStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "stores", indexes = {
                @Index(name = "idx_stores_status", columnList = "status"),
                @Index(name = "idx_stores_location", columnList = "latitude, longitude")
})
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

        @Column(name = "district", length = 100)
        private String district;

        @Column(name = "latitude", precision = 10, scale = 7)
        private BigDecimal latitude;

        @Column(name = "longitude", precision = 10, scale = 7)
        private BigDecimal longitude;

        @Column(name = "opening_time")
        private LocalTime openingTime;

        @Column(name = "closing_time")
        private LocalTime closingTime;


        @Column(name = "price_from")
        private Long priceFrom;

        @Column(name = "price_to")
        private Long priceTo;

        @Column(name = "phone", length = 20)
        private String phone;

        @Column(name = "email", length = 255)
        private String email;

        @Enumerated(EnumType.STRING)
        @Builder.Default
        @Column(name = "status", nullable = false, length = 30)
        private StoreStatus status = StoreStatus.PENDING;

        @Column(name = "reject_reason", columnDefinition = "TEXT")
        private String rejectReason;

        @Builder.Default
        @Column(name = "allow_resubmit", nullable = false)
        private boolean allowResubmit = true;

        @CreationTimestamp
        @Column(name = "created_at", nullable = false, updatable = false)
        private Instant createdAt;

        @UpdateTimestamp
        @Column(name = "updated_at", nullable = false)
        private Instant updatedAt;

        @Builder.Default
        @Column(name = "is_deleted", nullable = false)
        private boolean isDeleted = false;
}
