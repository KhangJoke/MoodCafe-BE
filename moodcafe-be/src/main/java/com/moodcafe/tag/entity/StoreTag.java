package com.moodcafe.tag.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
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
        name = "store_tags",
        indexes = {
                @Index(name = "idx_store_tags_store_id", columnList = "store_id"),
                @Index(name = "idx_store_tags_tag_id", columnList = "tag_id"),
                @Index(name = "idx_store_tags_status", columnList = "status")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoreTag {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "store_tag_id", nullable = false, updatable = false)
    private UUID storeTagId;

    @Column(name = "store_id", nullable = false)
    private UUID storeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tag_id", nullable = false)
    private Tag tag;

    @Builder.Default
    @Column(name = "status", nullable = false, length = 30)
    private String status = "PENDING";

    @Column(name = "proof_image_url", columnDefinition = "TEXT")
    private String proofImageUrl;

    @Column(name = "reject_reason", columnDefinition = "TEXT")
    private String rejectReason;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
