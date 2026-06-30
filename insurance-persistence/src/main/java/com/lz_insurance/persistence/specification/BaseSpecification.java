package com.lz_insurance.persistence.specification;

import com.lz_insurance.persistence.entity.BaseJpaEntity;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public abstract class BaseSpecification<T extends BaseJpaEntity> implements Specification<T> {

    protected String id;
    protected String createdBy;
    protected String updatedBy;
    protected LocalDateTime createdFrom;
    protected LocalDateTime createdTo;
    protected LocalDateTime updatedFrom;
    protected LocalDateTime updatedTo;
    protected boolean excludeDeleted = false;
    protected boolean includeDeletedOnly = false;

    public BaseSpecification<T> withId(String id) {
        this.id = id;
        return this;
    }

    public BaseSpecification<T> withCreatedBy(String createdBy) {
        this.createdBy = createdBy;
        return this;
    }

    public BaseSpecification<T> withUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
        return this;
    }

    public BaseSpecification<T> createdBetween(LocalDateTime from, LocalDateTime to) {
        this.createdFrom = from;
        this.createdTo = to;
        return this;
    }

    public BaseSpecification<T> updatedBetween(LocalDateTime from, LocalDateTime to) {
        this.updatedFrom = from;
        this.updatedTo = to;
        return this;
    }

    public BaseSpecification<T> notDeleted() {
        this.excludeDeleted = true;
        this.includeDeletedOnly = false;
        return this;
    }

    public BaseSpecification<T> onlyDeleted() {
        this.includeDeletedOnly = true;
        this.excludeDeleted = false;
        return this;
    }

    @Override
    public Predicate toPredicate(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
        List<Predicate> predicates = new ArrayList<>();


        if (StringUtils.hasText(id)) {
            predicates.add(cb.equal(root.get("id"), id));
        }

        if (StringUtils.hasText(createdBy)) {
            predicates.add(cb.equal(root.get("createdBy"), createdBy));
        }

        if (StringUtils.hasText(updatedBy)) {
            predicates.add(cb.equal(root.get("updatedBy"), updatedBy));
        }

        if (createdFrom != null && createdTo != null) {
            predicates.add(cb.between(root.get("createdAt"), createdFrom, createdTo));
        } else if (createdFrom != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), createdFrom));
        } else if (createdTo != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), createdTo));
        }

        if (updatedFrom != null && updatedTo != null) {
            predicates.add(cb.between(root.get("updatedAt"), updatedFrom, updatedTo));
        } else if (updatedFrom != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("updatedAt"), updatedFrom));
        } else if (updatedTo != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("updatedAt"), updatedTo));
        }

        if (excludeDeleted) {
            predicates.add(cb.isFalse(root.get("deleted")));
        } else if (includeDeletedOnly) {
            predicates.add(cb.isTrue(root.get("deleted")));
        }

        return cb.and(predicates.toArray(new Predicate[0]));
    }

}
