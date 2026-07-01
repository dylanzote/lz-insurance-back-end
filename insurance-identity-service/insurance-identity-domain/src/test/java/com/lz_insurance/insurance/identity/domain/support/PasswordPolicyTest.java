package com.lz_insurance.insurance.identity.domain.support;

import com.lz_insurance.insurance.identity.domain.exception.DomainValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("PasswordPolicy — complexity rules enforced by our domain before any Keycloak call")
class PasswordPolicyTest {

    @Test
    @DisplayName("a 12+ char password with upper, lower, digit and special passes")
    void acceptsCompliantPassword() {
        assertThatCode(() -> PasswordPolicy.validate("Str0ng-Passw0rd!"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("null or blank is rejected")
    void rejectsBlank() {
        assertThatThrownBy(() -> PasswordPolicy.validate(null))
                .isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> PasswordPolicy.validate("   "))
                .isInstanceOf(DomainValidationException.class);
    }

    @Test
    @DisplayName("shorter than the minimum length is rejected")
    void rejectsTooShort() {
        // 11 chars but otherwise compliant
        assertThatThrownBy(() -> PasswordPolicy.validate("Ab1!efghij"))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("at least " + PasswordPolicy.MIN_LENGTH);
    }

    @ParameterizedTest
    @DisplayName("missing a required character class is rejected")
    @ValueSource(strings = {
            "alllowercase1!",   // no uppercase
            "ALLUPPERCASE1!",   // no lowercase
            "NoDigitsHere!!",   // no digit
            "NoSpecialChar12"   // no special
    })
    void rejectsMissingCharacterClass(String password) {
        assertThatThrownBy(() -> PasswordPolicy.validate(password))
                .isInstanceOf(DomainValidationException.class);
    }

    @Test
    @DisplayName("the offending field name is carried on the exception")
    void attributesFailureToField() {
        assertThatThrownBy(() -> PasswordPolicy.validate("weak", "temporaryPassword"))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("temporaryPassword");
    }
}
