package com.lz_Insurance.insurance.identity.domain.model;

import com.lz_Insurance.insurance.identity.domain.enumeration.BranchStatus;
import com.lz_Insurance.insurance.identity.domain.enumeration.BranchType;
import com.lz_Insurance.insurance.identity.domain.exception.DomainValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BranchTest {

    @Test
    @DisplayName("a HEADQUARTER branch constructs as a top-level ACTIVE node")
    void headquarterIsTopLevel() {
        Branch hq = new Branch("tenant-1", null, "Head Office", "HQ", BranchType.HEADQUARTER);

        assertThat(hq.isTopLevel()).isTrue();
        assertThat(hq.getStatus()).isEqualTo(BranchStatus.ACTIVE);
    }

    @Test
    @DisplayName("a HEADQUARTER branch with a parent is rejected")
    void headquarterWithParentRejected() {
        assertThatThrownBy(() ->
                new Branch("tenant-1", "parent-1", "Head Office", "HQ", BranchType.HEADQUARTER))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("HEADQUARTER");
    }

    @Test
    @DisplayName("a LOCAL branch may hang beneath a parent")
    void localBranchUnderParent() {
        Branch local = new Branch("tenant-1", "parent-1", "Downtown", "DT", BranchType.LOCAL);

        assertThat(local.isTopLevel()).isFalse();
        assertThat(local.getParentBranchId()).isEqualTo("parent-1");
    }

    @Test
    @DisplayName("blank tenantId is rejected")
    void blankTenantRejected() {
        assertThatThrownBy(() -> new Branch("", null, "Head Office", "HQ", BranchType.HEADQUARTER))
                .isInstanceOf(DomainValidationException.class);
    }

    @Test
    @DisplayName("null type is rejected")
    void nullTypeRejected() {
        assertThatThrownBy(() -> new Branch("tenant-1", null, "Office", "OF", null))
                .isInstanceOf(DomainValidationException.class);
    }

    @Test
    @DisplayName("deactivate then activate toggles status")
    void deactivateActivate() {
        Branch local = new Branch("tenant-1", "parent-1", "Downtown", "DT", BranchType.LOCAL);

        local.deactivate();
        assertThat(local.getStatus()).isEqualTo(BranchStatus.INACTIVE);

        local.activate();
        assertThat(local.getStatus()).isEqualTo(BranchStatus.ACTIVE);
    }
}
