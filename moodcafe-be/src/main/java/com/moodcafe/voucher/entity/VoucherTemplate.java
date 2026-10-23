package com.moodcafe.voucher.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "voucher_templates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VoucherTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "template_id", nullable = false, updatable = false)
    private UUID templateId;

    @Column(name = "title", nullable = false, length = 150)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "face_value_vnd", nullable = false, precision = 12, scale = 2)
    private BigDecimal faceValueVnd;

    /**
     * Point cost in MoodPoints (1 point = 1 VND value).
     */
    @Column(name = "point_cost", nullable = false)
    private Long pointCost;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "display_order")
    @Builder.Default
    private Integer displayOrder = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
