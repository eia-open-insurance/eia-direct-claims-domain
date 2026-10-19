package com.eia.directclaims.domain;

/**
 * A request that is not for the DIRECT book is indistinguishable from a
 * missing resource. The API maps this to 404, never 403 (SPEC §3.6.2).
 */
public class BookAccessDeniedException extends RuntimeException {
    public BookAccessDeniedException() {
        super("Not found");
    }
}
