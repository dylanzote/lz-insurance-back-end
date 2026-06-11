package com.lz_Insurance.persistence.listener;

import com.lz_Insurance.core.model.BaseDomainEntity;
import jakarta.persistence.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
public class AuditLogListener {

    @PrePersist
    public void prePersist(BaseDomainEntity entity) {
        LocalDateTime now = LocalDateTime.now();
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        log.debug("Pre-persist audit for entity: {}", entity.getClass().getSimpleName());
    }

    @PreUpdate
    public void preUpdate(BaseDomainEntity entity) {
        entity.setUpdatedAt(LocalDateTime.now());
        entity.setVersion(entity.getVersion() + 1);
        log.debug("Pre-update audit for entity: {}", entity.getClass().getSimpleName());
    }

    @PreRemove
    public void preRemove(BaseDomainEntity entity) {
        log.debug("Pre-remove audit for entity: {}", entity.getClass().getSimpleName());
    }

    @PostLoad
    public void postLoad(BaseDomainEntity entity) {
        log.debug("Post-load audit for entity: {}", entity.getClass().getSimpleName());
    }
}
