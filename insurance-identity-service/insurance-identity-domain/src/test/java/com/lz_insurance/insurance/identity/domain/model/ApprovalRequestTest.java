package com.lz_insurance.insurance.identity.domain.model;

import com.lz_insurance.insurance.identity.domain.enums.ApprovalStatus;
import com.lz_insurance.insurance.identity.domain.exception.ApprovalAlreadyDecidedException;
import com.lz_insurance.insurance.identity.domain.exception.DomainValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ApprovalRequestTest {

    private ApprovalRequest pending() {
        return new ApprovalRequest("profile-1", "requester-1", "please review");
    }

    @Test
    @DisplayName("a new request is PENDING with a requestedAt timestamp")
    void newRequestIsPending() {
        ApprovalRequest request = pending();

        assertThat(request.isPending()).isTrue();
        assertThat(request.getStatus()).isEqualTo(ApprovalStatus.PENDING);
        assertThat(request.getRequestedAt()).isNotNull();
        assertThat(request.getReviewedAt()).isNull();
    }

    @Test
    @DisplayName("blank identityProfileId is rejected")
    void blankProfileRejected() {
        assertThatThrownBy(() -> new ApprovalRequest("", "requester-1", null))
                .isInstanceOf(DomainValidationException.class);
    }

    @Test
    @DisplayName("approve transitions to APPROVED and records the reviewer")
    void approve() {
        ApprovalRequest request = pending();

        request.approve("reviewer-9", "looks good");

        assertThat(request.getStatus()).isEqualTo(ApprovalStatus.APPROVED);
        assertThat(request.getReviewedById()).isEqualTo("reviewer-9");
        assertThat(request.getReviewNotes()).isEqualTo("looks good");
        assertThat(request.getReviewedAt()).isNotNull();
    }

    @Test
    @DisplayName("reject transitions to REJECTED")
    void reject() {
        ApprovalRequest request = pending();

        request.reject("reviewer-9", "missing documents");

        assertThat(request.getStatus()).isEqualTo(ApprovalStatus.REJECTED);
        assertThat(request.getReviewedById()).isEqualTo("reviewer-9");
    }

    @Test
    @DisplayName("a decided request cannot be approved or rejected again")
    void cannotDecideTwice() {
        ApprovalRequest request = pending();
        request.approve("reviewer-9", "ok");

        assertThatThrownBy(() -> request.approve("reviewer-2", "again"))
                .isInstanceOf(ApprovalAlreadyDecidedException.class);
        assertThatThrownBy(() -> request.reject("reviewer-2", "no"))
                .isInstanceOf(ApprovalAlreadyDecidedException.class);
    }

    @Test
    @DisplayName("a blank reviewer id is rejected")
    void blankReviewerRejected() {
        ApprovalRequest request = pending();
        assertThatThrownBy(() -> request.approve(" ", "ok"))
                .isInstanceOf(DomainValidationException.class);
    }
}
