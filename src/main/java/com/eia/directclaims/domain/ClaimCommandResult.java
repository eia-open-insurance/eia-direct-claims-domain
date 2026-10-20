package com.eia.directclaims.domain;

/**
 * Result of a forwarded create command: the public projection and the expert's
 * {@code Idempotent-Replay} header value ({@code null} when the caller supplied
 * no Idempotency-Key).
 */
record ClaimCommandResult<T>(T value, String replayHeader) {
}
