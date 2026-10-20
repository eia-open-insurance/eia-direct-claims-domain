package com.eia.directclaims.domain;

import java.util.UUID;

/** Raised when an allocation is absent from the expert's tenant-scoped list. */
public class AllocationNotFoundException extends RuntimeException {
    public AllocationNotFoundException(UUID id) {
        super("Claim allocation not found: " + id);
    }
}
