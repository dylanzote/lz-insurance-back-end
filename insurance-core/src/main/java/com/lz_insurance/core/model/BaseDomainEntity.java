package com.lz_insurance.core.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Framework-free base for domain models (id, version, audit fields, soft delete).
 *
 * <p><b>Intentional mirror of {@code com.lz_insurance.persistence.entity.BaseJpaEntity}.</b>
 * That class is the JPA-mapped counterpart used by persistence entities; the two
 * deliberately carry the same field set across the domain/infrastructure boundary and
 * are bridged by the MapStruct mappers — do NOT merge them to "remove the duplication":
 * the separation keeps JPA out of the domain.
 */
@Data
@SuperBuilder
@NoArgsConstructor
public abstract class BaseDomainEntity implements Serializable {

    private String id;

    private Long version;

    @NotNull
    private String createdBy;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    private String updatedBy;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;

    private boolean deleted;

    private LocalDateTime deletedAt;

    private String deletedBy;
}
