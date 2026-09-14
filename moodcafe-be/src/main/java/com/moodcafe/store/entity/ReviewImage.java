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
        name = "review_images",
        indexes = {
                @Index(name = "idx_review_images_review_id", columnList = "review_id")
        }
)
@SQLDelete(sql = "UPDATE review_images SET is_deleted = true WHERE review_image_id = ?")
@SQLRestriction("is_deleted = false")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewImage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "review_image_id", nullable = false, updatable = false)
    private UUID reviewImageId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "review_id", nullable = false, foreignKey = @ForeignKey(name = "fk_review_images_review"))
    private StoreReview review;

    @Column(name = "image_url", nullable = false, length = 500)
    private String imageUrl;

    @Builder.Default
    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
