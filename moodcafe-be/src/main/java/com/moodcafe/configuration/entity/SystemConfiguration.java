package com.moodcafe.configuration.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "system_configurations",
        indexes = {
                @Index(name = "idx_system_configs_group", columnList = "config_group"),
                @Index(name = "idx_system_configs_key", columnList = "config_key"),
                @Index(name = "idx_system_configs_public", columnList = "is_public"),
                @Index(name = "idx_system_configs_is_deleted", columnList = "is_deleted")
        }
)
@SQLDelete(sql = "UPDATE system_configurations SET is_deleted = true, deleted_at = CURRENT_TIMESTAMP WHERE config_id = ?")
@SQLRestriction("is_deleted = false")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemConfiguration {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "config_id", nullable = false, updatable = false)
    private UUID configId;

    @Column(name = "config_group", nullable = false, length = 50)
    private String configGroup;

    @Column(name = "config_key", nullable = false, length = 100)
    private String configKey;

    @Column(name = "config_value", nullable = false, columnDefinition = "TEXT")
    private String configValue;

    @Column(name = "data_type", nullable = false, length = 20)
    private String dataType;

    @Column(name = "display_name", nullable = false, length = 255)
    private String displayName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Builder.Default
    @Column(name = "is_public", nullable = false)
    private boolean isPublic = false;

    @Builder.Default
    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "updated_by")
    private UUID updatedBy;
}
