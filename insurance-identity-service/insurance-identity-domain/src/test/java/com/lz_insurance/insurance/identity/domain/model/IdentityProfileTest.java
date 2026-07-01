package com.lz_insurance.insurance.identity.domain.model;

import com.lz_insurance.insurance.identity.domain.enums.ExternalUserType;
import com.lz_insurance.insurance.identity.domain.enums.IdentityStatus;
import com.lz_insurance.insurance.identity.domain.enums.InternalUserType;
import com.lz_insurance.insurance.identity.domain.exception.DomainValidationException;
import com.lz_insurance.insurance.identity.domain.exception.InvalidStateTransitionException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IdentityProfileTest {

    private IdentityProfile internal() {
        return IdentityProfile.internal("tenant-1", "branch-1", InternalUserType.AGENT,
                "jane@acme.com", "Jane", "Doe");
    }

    private IdentityProfile external() {
        return IdentityProfile.external("tenant-1", null, ExternalUserType.POLICYHOLDER,
                "bob@gmail.com", "Bob", "Smith");
    }

    @Nested
    class Construction {

        @Test
        @DisplayName("internal profile carries internalUserType, a branch, and starts PENDING_APPROVAL")
        void internalProfile() {
            IdentityProfile p = internal();

            assertThat(p.getInternalUserType()).isEqualTo(InternalUserType.AGENT);
            assertThat(p.getExternalUserType()).isNull();
            assertThat(p.getBranchId()).isEqualTo("branch-1");
            assertThat(p.getStatus()).isEqualTo(IdentityStatus.PENDING_APPROVAL);
            assertThat(p.getKeycloakUserId()).isNull();
            assertThat(p.isPasswordChangeRequired()).isFalse();
        }

        @Test
        @DisplayName("external profile carries externalUserType and may have no branch")
        void externalProfile() {
            IdentityProfile p = external();

            assertThat(p.getExternalUserType()).isEqualTo(ExternalUserType.POLICYHOLDER);
            assertThat(p.getInternalUserType()).isNull();
            assertThat(p.getBranchId()).isNull();
        }

        @Test
        @DisplayName("internal profile without a branch is rejected")
        void internalRequiresBranch() {
            assertThatThrownBy(() -> IdentityProfile.internal("tenant-1", null,
                    InternalUserType.AGENT, "jane@acme.com", "Jane", "Doe"))
                    .isInstanceOf(DomainValidationException.class);
        }

        @Test
        @DisplayName("blank email is rejected")
        void blankEmailRejected() {
            assertThatThrownBy(() -> IdentityProfile.internal("tenant-1", "branch-1",
                    InternalUserType.AGENT, " ", "Jane", "Doe"))
                    .isInstanceOf(DomainValidationException.class);
        }
    }

    @Nested
    class Lifecycle {

        @Test
        @DisplayName("activate attaches the Keycloak id and moves PENDING_APPROVAL -> ACTIVE")
        void activate() {
            IdentityProfile p = internal();

            p.activate("kc-123");

            assertThat(p.getStatus()).isEqualTo(IdentityStatus.ACTIVE);
            assertThat(p.getKeycloakUserId()).isEqualTo("kc-123");
            assertThat(p.isPasswordChangeRequired()).isTrue();
        }

        @Test
        @DisplayName("activate with a blank Keycloak id is rejected")
        void activateBlankRejected() {
            IdentityProfile p = internal();
            assertThatThrownBy(() -> p.activate(" "))
                    .isInstanceOf(DomainValidationException.class);
        }

        @Test
        @DisplayName("suspend requires ACTIVE; cannot suspend a PENDING profile")
        void suspendFromPendingRejected() {
            IdentityProfile p = internal();
            assertThatThrownBy(p::suspend).isInstanceOf(InvalidStateTransitionException.class);
        }

        @Test
        @DisplayName("ACTIVE -> SUSPENDED -> ACTIVE round trip")
        void suspendReactivate() {
            IdentityProfile p = internal();
            p.activate("kc-123");

            p.suspend();
            assertThat(p.getStatus()).isEqualTo(IdentityStatus.SUSPENDED);

            p.reactivate();
            assertThat(p.getStatus()).isEqualTo(IdentityStatus.ACTIVE);
        }

        @Test
        @DisplayName("deactivate is allowed from ACTIVE and SUSPENDED, and is terminal")
        void deactivate() {
            IdentityProfile p = internal();
            p.activate("kc-123");
            p.deactivate();

            assertThat(p.getStatus()).isEqualTo(IdentityStatus.DEACTIVATED);
            assertThatThrownBy(() -> p.activate("kc-999"))
                    .isInstanceOf(InvalidStateTransitionException.class);
        }

        @Test
        @DisplayName("deactivate from PENDING_APPROVAL is rejected")
        void deactivateFromPendingRejected() {
            IdentityProfile p = internal();
            assertThatThrownBy(p::deactivate).isInstanceOf(InvalidStateTransitionException.class);
        }
    }

    @Nested
    class ForcedPasswordChange {

        @Test
        @DisplayName("completePasswordChange clears the flag for an ACTIVE profile with a pending change")
        void completesFromActive() {
            IdentityProfile p = internal();
            p.activate("kc-123");

            p.completePasswordChange();

            assertThat(p.isPasswordChangeRequired()).isFalse();
            assertThat(p.getStatus()).isEqualTo(IdentityStatus.ACTIVE);
        }

        @Test
        @DisplayName("completePasswordChange on a PENDING profile (never activated) is rejected")
        void rejectedWhenNotActive() {
            IdentityProfile p = internal();
            assertThatThrownBy(p::completePasswordChange)
                    .isInstanceOf(InvalidStateTransitionException.class);
        }

        @Test
        @DisplayName("completePasswordChange is rejected when there is no pending change (already completed)")
        void rejectedWhenNothingPending() {
            IdentityProfile p = internal();
            p.activate("kc-123");
            p.completePasswordChange();

            assertThatThrownBy(p::completePasswordChange)
                    .isInstanceOf(InvalidStateTransitionException.class);
        }
    }
}
