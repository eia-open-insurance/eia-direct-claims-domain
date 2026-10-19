package com.eia.directclaims.domain;

import com.fasterxml.jackson.annotation.JsonFormat;

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
        String claimType,
        String status,
        LocalDate lossDate,
        LocalDate reportDate,
        String causeCode,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal reserveAmount,
        String reserveCurrency,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal settlementAuthorityAmount,
        String settlementAuthorityCurrency,
        boolean litigationFlag,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal currentReserveAmount,
        int version,
        Instant createdAt,
        Instant updatedAt
) {
}
