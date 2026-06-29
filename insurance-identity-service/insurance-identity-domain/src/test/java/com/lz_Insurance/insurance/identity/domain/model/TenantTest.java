package com.lz_Insurance.insurance.identity.domain.model;

import com.lz_Insurance.insurance.identity.domain.enumeration.TenantStatus;
import com.lz_Insurance.insurance.identity.domain.exception.DomainValidationException;
import com.lz_Insurance.insurance.identity.domain.exception.InvalidStateTransitionException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TenantTest {

    private Tenant validTenant() {
        return new Tenant("Acme Insurance", "ACME", "ca");
    }

    @Test
    @DisplayName("constructs ACTIVE, un-bootstrapped, and upper-cases the country")
    void validConstruction() {
        Tenant tenant = validTenant();

        assertThat(tenant.getName()).isEqualTo("Acme Insurance");
        assertThat(tenant.getCode()).isEqualTo("ACME");
        assertThat(tenant.getCountry()).isEqualTo("CA");
        assertThat(tenant.isBootstrapped()).isFalse();
        assertThat(tenant.getStatus()).isEqualTo(TenantStatus.ACTIVE);
        assertThat(tenant.getHeadquarterBranchId()).isNull();
    }

    @Test
    @DisplayName("blank name is rejected")
    void blankNameRejected() {
        assertThatThrownBy(() -> new Tenant(" ", "ACME", "CA"))
                .isInstanceOf(DomainValidationException.class);
    }

    @Test
    @DisplayName("non alpha-2 country is rejected")
    void invalidCountryRejected() {
        assertThatThrownBy(() -> new Tenant("Acme", "ACME", "CAN"))
                .isInstanceOf(DomainValidationException.class);
    }

    @Test
    @DisplayName("markBootstrapped records HQ and flips the flag")
    void markBootstrapped() {
        Tenant tenant = validTenant();

        tenant.markBootstrapped("branch-hq-1");

        assertThat(tenant.isBootstrapped()).isTrue();
        assertThat(tenant.getHeadquarterBranchId()).isEqualTo("branch-hq-1");
    }

    @Test
    @DisplayName("markBootstrapped rejects a blank HQ id")
    void markBootstrappedBlankRejected() {
        Tenant tenant = validTenant();
        assertThatThrownBy(() -> tenant.markBootstrapped(""))
                .isInstanceOf(DomainValidationException.class);
    }

    @Test
    @DisplayName("suspend then reactivate toggles status")
    void suspendReactivate() {
        Tenant tenant = validTenant();

        tenant.suspend();
        assertThat(tenant.getStatus()).isEqualTo(TenantStatus.SUSPENDED);

        tenant.reactivate();
        assertThat(tenant.getStatus()).isEqualTo(TenantStatus.ACTIVE);
    }

    @Test
    @DisplayName("a deactivated tenant cannot be suspended or reactivated")
    void deactivatedIsTerminal() {
        Tenant tenant = validTenant();
        tenant.deactivate();
        assertThat(tenant.getStatus()).isEqualTo(TenantStatus.DEACTIVATED);

        assertThatThrownBy(tenant::suspend).isInstanceOf(InvalidStateTransitionException.class);
        assertThatThrownBy(tenant::reactivate).isInstanceOf(InvalidStateTransitionException.class);
    }
}
