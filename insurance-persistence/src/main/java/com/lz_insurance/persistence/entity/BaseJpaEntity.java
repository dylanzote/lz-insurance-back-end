package com.lz_insurance.persistence.entity;

import com.lz_insurance.core.util.IdGenerator;
import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Version;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * JPA mapped-superclass base for every persistence entity: surrogate {@code id},
 * optimistic-locking {@code version}, audit columns and a soft-delete flag.
 *
 * <p><b>Intentional mirror of {@link com.lz_insurance.core.model.BaseDomainEntity}.</b>
 * That class is the framework-free base for domain models; this one is its JPA
 * counterpart. They deliberately carry the same field set across the
 * domain/infrastructure boundary and are bridged by the MapStruct mappers — do NOT
 * merge them to "remove the duplication": the separation is the architecture.
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
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

    @CreatedBy
    @Column(name = "created_by", nullable = false, updatable = false)
    private String createdBy;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedBy
    @Column(name = "updated_by")
    private String updatedBy;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted", nullable = false)
    private boolean deleted;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "deleted_by")
    private String deletedBy;

    /**
     * Generates the surrogate {@code id} only. The people/time audit fields
     * ({@code createdBy}/{@code updatedBy}/{@code createdAt}/{@code updatedAt})
     * are populated by Spring Data JPA auditing via {@code AuditingEntityListener}
     * and the {@code auditorProvider}, not here.
     */
    @PrePersist
    void onCreate() {
        if (id == null || id.isEmpty()) {
            id = IdGenerator.generate();
        }
    }
}
