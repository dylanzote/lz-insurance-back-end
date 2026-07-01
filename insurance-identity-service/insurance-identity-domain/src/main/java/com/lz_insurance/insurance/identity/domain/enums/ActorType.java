package com.lz_insurance.insurance.identity.domain.enums;

/**
 * Discriminator that tells every downstream service how to treat an identity.
 * Surfaced as the {@code actorType} claim in the JWT.
 */
public enum ActorType {
    INTERNAL,
    EXTERNAL
}
