package com.eia.directclaims.domain;

import java.util.List;
import java.util.UUID;

/**
 * Envelope for the common search endpoint. The response is always scoped to the
 * caller's tenant and the DIRECT book, so every item uses the {@code DIRECT_CLAIM}
 * type and links back to the direct-claims workbench.
 */
public record ClaimSearchResponse(String query, int limit, List<Item> items) {
    public record Item(
            UUID id,
            String type,
            String label,
            String secondary,
            String status,
            String href
    ) {
    }
}
