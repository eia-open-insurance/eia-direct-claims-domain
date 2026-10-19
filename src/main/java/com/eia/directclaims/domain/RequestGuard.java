package com.eia.directclaims.domain;

import java.util.UUID;

/**
 * Header-driven request context. Direct Claims is fixed to the DIRECT book;
 * any other or missing book value is a 404.
 */
public final class RequestGuard {
    private RequestGuard() {
    }

    public static void requireDirectBook(String bookHeader) {
        if (!"DIRECT".equals(bookHeader)) {
            throw new BookAccessDeniedException();
        }
    }

    public static UUID requireTenant(String tenantHeader) {
        if (tenantHeader == null || tenantHeader.isBlank()) {
            throw new DomainValidationException("X-EIA-Tenant header is required");
        }
        try {
            return UUID.fromString(tenantHeader.trim());
        } catch (IllegalArgumentException ex) {
            throw new DomainValidationException("X-EIA-Tenant must be a UUID");
        }
    }
}
