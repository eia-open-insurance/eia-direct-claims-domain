package com.eia.directclaims.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ClaimDto(
        UUID id,
        UUID tenantId,
        String businessBook,
        String claimNumber,
        UUID policyId,
        UUID claimantPartyId,
        String status,
        LocalDate lossDate,
        LocalDate reportDate,
        String causeCode,
        BigDecimal reserveAmount,
        String reserveCurrency,
        int version,
        Instant createdAt,
        Instant updatedAt
) {
}
