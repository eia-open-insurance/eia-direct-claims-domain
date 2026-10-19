package com.eia.directclaims.domain;

/**
 * Optional optimistic-concurrency guard on the public close transaction.
 */
public record CloseClaimCommand(Integer expectedVersion) {
}
