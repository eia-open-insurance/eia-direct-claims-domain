package com.eia.directclaims.domain;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Read models for the Direct Claims second slice. Money is serialized as a
 * decimal string, matching SPEC §7.6, §10.3, and the expert.
 */
public final class ClaimDetailDtos {
    private ClaimDetailDtos() {
    }

    public record Injury(
            UUID id,
            UUID tenantId,
            UUID claimId,
            UUID claimantPartyId,
            String injuryType,
            String bodyPart,
            List<String> diagnosisCodes,
            String description,
            String severity,
            LocalDate diagnosisDate,
            int version,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    public record Exposure(
            UUID id,
            UUID tenantId,
            UUID claimId,
            UUID claimantPartyId,
            String exposureType,
            String productName,
            String substanceName,
            String siteName,
            UUID employerPartyId,
            String jobTitle,
            String jurisdiction,
            LocalDate exposureFrom,
            LocalDate exposureTo,
            String intensity,
            int version,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    public record ReserveEntry(
            UUID id,
            UUID tenantId,
            UUID claimId,
            String component,
            @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal amount,
            String currency,
            LocalDate effectiveDate,
            String reasonCode,
            String reasonText,
            String authority,
            int sequence,
            boolean current,
            int version,
            Instant createdAt
    ) {
    }

    public record ReserveComponent(
            String component,
            @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal currentAmount,
            String currency
    ) {
    }

    public record ReserveSummary(
            UUID claimId,
            String currency,
            @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal totalCurrentAmount,
            List<ReserveComponent> components,
            List<ReserveEntry> history
    ) {
    }

    public record CoverageDecision(
            UUID id,
            UUID tenantId,
            UUID claimId,
            String position,
            String basis,
            String policyProvisions,
            String allocationMethod,
            @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal allocationPct,
            LocalDate decisionDate,
            UUID reviewerPartyId,
            String status,
            int version,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    public record Litigation(
            UUID id,
            UUID tenantId,
            UUID claimId,
            String matterType,
            String jurisdiction,
            UUID counselPartyId,
            LocalDate filingDate,
            LocalDate trialDate,
            String status,
            String caseNumber,
            int version,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    public record Settlement(
            UUID id,
            UUID tenantId,
            UUID claimId,
            String settlementNumber,
            @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal amount,
            String currency,
            String structure,
            String releaseScope,
            String approvalAuthority,
            String paymentInstructions,
            UUID releaseDocumentId,
            String status,
            UUID authorizedBy,
            Instant authorizedAt,
            Instant executedAt,
            int version,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    public record Recovery(
            UUID id,
            UUID tenantId,
            UUID claimId,
            String recoveryType,
            @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal amount,
            String currency,
            String source,
            String status,
            LocalDate recoveryDate,
            int version,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    public record Lien(
            UUID id,
            UUID tenantId,
            UUID claimId,
            String lienType,
            String lienholder,
            @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal amount,
            String currency,
            String status,
            String resolutionType,
            Instant resolvedAt,
            int version,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    /**
     * A diary task on a claim. The public projection deliberately omits the
     * expert's {@code tenantId}; the tenant is implied by the request context.
     */
    public record Task(
            UUID id,
            UUID claimId,
            String taskNumber,
            String title,
            String description,
            LocalDate dueDate,
            String priority,
            String status,
            String assignedTo,
            long version,
            Instant createdAt,
            Instant updatedAt,
            Instant completedAt
    ) {
    }

    /**
     * An append-only claim note. Notes have no version and no update route.
     */
    public record Note(
            UUID id,
            UUID claimId,
            String noteNumber,
            String noteType,
            String body,
            String author,
            Instant createdAt
    ) {
    }
}
