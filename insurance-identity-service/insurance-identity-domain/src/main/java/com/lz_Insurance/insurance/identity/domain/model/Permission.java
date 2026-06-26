package com.lz_Insurance.insurance.identity.domain.model;

import com.lz_Insurance.core.model.BaseDomainEntity;
import com.lz_Insurance.insurance.identity.domain.enumeration.OrganizationScope;
import com.lz_Insurance.insurance.identity.domain.enumeration.PermissionAction;
import com.lz_Insurance.insurance.identity.domain.enumeration.PermissionResource;
import com.lz_Insurance.insurance.identity.domain.support.DomainGuard;
import lombok.Getter;
import lombok.ToString;

/**
 * An atomic, grantable capability expressed as {@code resource:action:dataScope}
 * (e.g. {@code POLICY:READ:OWN}). {@code defaultScope} is the scope applied when a
 * role grants this permission without a tenant-specific override.
 */
@Getter
@ToString(callSuper = true)
public class Permission extends BaseDomainEntity {

    private final PermissionResource resource;
    private final PermissionAction action;
    private final OrganizationScope defaultScope;

    public Permission(PermissionResource resource, PermissionAction action, OrganizationScope defaultScope) {
        DomainGuard.notNull(resource, "resource");
        DomainGuard.notNull(action, "action");
        DomainGuard.notNull(defaultScope, "defaultScope");

        this.resource = resource;
        this.action = action;
        this.defaultScope = defaultScope;
    }

    /** Rebuilds a {@code Permission} from its persisted state (infrastructure mapper only). */
    public static Permission reconstitute(PermissionResource resource, PermissionAction action,
                                          OrganizationScope defaultScope) {
        return new Permission(resource, action, defaultScope);
    }

    /** Canonical {@code resource:action:scope} key. */
    public String key() {
        return "%s:%s:%s".formatted(resource, action, defaultScope);
    }
}
