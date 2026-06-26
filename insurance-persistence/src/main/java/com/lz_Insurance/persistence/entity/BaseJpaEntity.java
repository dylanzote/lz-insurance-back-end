package com.lz_Insurance.persistence.entity;

import com.lz_Insurance.core.util.IdGenerator;
import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Version;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * JPA mapped-superclass base for every persistence entity: surrogate {@code id},
 * optimistic-locking {@code version}, audit columns and a soft-delete flag.
 *
 * <p><b>Intentional mirror of {@link com.lz_Insurance.core.model.BaseDomainEntity}.</b>
 * That class is the framework-free base for domain models; this one is its JPA
 * counterpart. They deliberately carry the same field set across the
 * domain/infrastructure boundary and are bridged by the MapStruct mappers — do NOT
 * merge them to "remove the duplication": the separation is the architecture.
 */
@MappedSuperclass
@Data
@SuperBuilder
@NoArgsConstructor
public abstract class BaseJpaEntity implements Serializable {

    @Id
    @Column(name = "id", nullable = false, updatable = false, length = 36)
    private String id;

    @Version
    @Column(name = "version")
    private Long version;

    @Column(name = "created_by", nullable = false, updatable = false)
    private String createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_by")
    private String updatedBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted", nullable = false)
    private boolean deleted;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "deleted_by")
    private String deletedBy;

    @PrePersist
    void onCreate() {
        if (id == null || id.isEmpty()) {
            id = IdGenerator.generate();
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (createdBy == null || createdBy.isEmpty()) {
            createdBy = "SYSTEM";
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
        if (updatedBy == null || updatedBy.isEmpty()) {
            updatedBy = "SYSTEM";
        }
    }
}
