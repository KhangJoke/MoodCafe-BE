package com.moodcafe.tag.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "vibe_tags",
        indexes = {
                @Index(name = "idx_vibe_tags_name", columnList = "name", unique = true),
                @Index(name = "idx_vibe_tags_category", columnList = "category")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MasterTag {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "vibe_tag_id", nullable = false, updatable = false)
    private UUID tagId;

    @Column(name = "name", nullable = false, unique = true, length = 100)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Builder.Default
    @Column(name = "tag_type", nullable = false, length = 30)
    private String tagType = "PRIMARY";

    @Builder.Default
    @Column(name = "category", nullable = false, length = 30)
    private String category = "VIBE";

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
