package com.lz_Insurance.security.keycloak.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.BeanUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class KeyCloakUser {
    private String id;
    private String firstName;
    private String lastName;
    private String email;
    private String username;
    private String password;
    private boolean enabled;
    private boolean emailVerified;
    private Map<String, List<String>> attributes;
    private List<String> roles;

    public UserRepresentation toUserRepresentation() {
        List<CredentialRepresentation> credentialRepresentations = new ArrayList<>();
        CredentialRepresentation credentialRepresentation = new CredentialRepresentation();
        credentialRepresentation.setTemporary(false);
        credentialRepresentation.setValue(password);
        credentialRepresentation.setType(CredentialRepresentation.PASSWORD);
        credentialRepresentations.add(credentialRepresentation);
        return getUserRepresentation(credentialRepresentations);
    }

    private UserRepresentation getUserRepresentation(List<CredentialRepresentation> credentialRepresentations) {
        UserRepresentation userRepresentation = new UserRepresentation();
        userRepresentation.setFirstName(firstName);
        userRepresentation.setLastName(lastName);
        userRepresentation.setEmail(email);
        userRepresentation.setUsername(username != null ? username : email);
        userRepresentation.setEnabled(enabled);
        userRepresentation.setEmailVerified(emailVerified);
        userRepresentation.setCredentials(credentialRepresentations);

        if (attributes != null && !attributes.isEmpty()) {
            userRepresentation.setAttributes(attributes);
        }
        return userRepresentation;
    }

    public UserRepresentation toUpdateUserRepresentation() {
        UserRepresentation userRepresentation = new UserRepresentation();
        userRepresentation.setId(id);
        userRepresentation.setFirstName(firstName);
        userRepresentation.setLastName(lastName);
        userRepresentation.setEmail(email);
        userRepresentation.setUsername(username);
        userRepresentation.setEnabled(enabled);
        userRepresentation.setEmailVerified(emailVerified);

        if (attributes != null && !attributes.isEmpty()) {
            userRepresentation.setAttributes(attributes);
        }
        return userRepresentation;
    }

    public static KeyCloakUser toUser(UserRepresentation userRepresent) {
        KeyCloakUser keyCloakUser = new KeyCloakUser();
        BeanUtils.copyProperties(userRepresent, keyCloakUser);
        return keyCloakUser;
    }
}


