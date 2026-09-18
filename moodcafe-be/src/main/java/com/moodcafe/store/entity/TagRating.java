package com.moodcafe.store.entity;

import com.moodcafe.tag.entity.Tag;
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
        name = "tag_ratings",
        indexes = {
                @Index(name = "idx_tag_ratings_review_id", columnList = "review_id"),
                @Index(name = "idx_tag_ratings_tag_id", columnList = "tag_id"),
                @Index(name = "idx_tag_ratings_is_deleted", columnList = "is_deleted")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_review_tag_rating", columnNames = {"review_id", "tag_id"})
        }
)
@SQLDelete(sql = "UPDATE tag_ratings SET is_deleted = true WHERE tag_rating_id = ?")
@SQLRestriction("is_deleted = false")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TagRating {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "tag_rating_id", nullable = false, updatable = false)
    private UUID tagRatingId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "review_id", nullable = false, foreignKey = @ForeignKey(name = "fk_tag_ratings_review"))
    private StoreReview review;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tag_id", nullable = false, foreignKey = @ForeignKey(name = "fk_tag_ratings_tag"))
    private Tag tag;

    @Column(name = "score", nullable = false)
    private Integer score;

    @Builder.Default
    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
