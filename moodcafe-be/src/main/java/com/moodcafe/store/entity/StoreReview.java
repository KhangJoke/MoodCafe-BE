package com.moodcafe.store.entity;

import com.moodcafe.auth.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
        name = "reviews",
        indexes = {
                @Index(name = "idx_reviews_store_id", columnList = "store_id"),
                @Index(name = "idx_reviews_user_id", columnList = "user_id"),
                @Index(name = "idx_reviews_created_at", columnList = "created_at")
        }
)
@SQLDelete(sql = "UPDATE reviews SET is_deleted = true WHERE review_id = ?")
@SQLRestriction("is_deleted = false")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoreReview {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "review_id", nullable = false, updatable = false)
    private UUID reviewId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false, foreignKey = @ForeignKey(name = "fk_reviews_store"))
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_reviews_user"))
    private User user;

    @Column(name = "visit_verification_id")
    private UUID visitVerificationId;

    @Column(name = "overall_rating", nullable = false, precision = 2, scale = 1)
    private BigDecimal overallRating;

    @Column(name = "quietness_rating")
    private Integer quietnessRating;

    @Column(name = "lighting_rating")
    private Integer lightingRating;

    @Column(name = "seating_rating")
    private Integer seatingRating;

    @Column(name = "outlet_rating")
    private Integer outletRating;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Builder.Default
    @OneToMany(mappedBy = "review", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReviewImage> images = new ArrayList<>();

    @Builder.Default
    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public void addImage(ReviewImage image) {
        images.add(image);
        image.setReview(this);
    }
}
