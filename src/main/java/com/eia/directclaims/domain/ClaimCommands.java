package com.eia.directclaims.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Public commands for the Direct Claims second slice. The domain adds the
 * tenant header when it forwards a command to the expert.
 */
public final class ClaimCommands {
    private ClaimCommands() {
    }

    public record Injury(
            UUID claimantPartyId,
            String injuryType,
            String bodyPart,
            List<String> diagnosisCodes,
            String description,
            String severity,
            LocalDate diagnosisDate,
            Integer expectedVersion
    ) {
    }

    public record Exposure(
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
            Integer expectedVersion
    ) {
    }

    public record ReserveChange(
            String component,
            BigDecimal amount,
            String currency,
            LocalDate effectiveDate,
            String reasonCode,
            String reasonText,
            String authority,
            Integer expectedVersion
    ) {
    }

    public record CoverageDecision(
            String position,
            String basis,
            String policyProvisions,
            String allocationMethod,
            BigDecimal allocationPct,
            LocalDate decisionDate,
            UUID reviewerPartyId
    ) {
    }

    /**
     * Command to split part of a claim across a policy year and layer. The
     * expert owns the policy match, the percentage total, and every other
     * invariant; the domain validates the caller-fixable contract fields first.
     * {@code limitId} optionally names the policy limit the allocation erodes;
     * when present the domain posts the erosion fact to the Policy domain.
     */
    public record Allocation(
            UUID policyId,
            Integer policyYear,
            String layer,
            BigDecimal allocationPct,
            BigDecimal allocatedAmount,
            UUID limitId
    ) {
    }

    public record Litigation(
            String matterType,
            String jurisdiction,
            UUID counselPartyId,
            LocalDate filingDate,
            LocalDate trialDate,
            String status,
            String caseNumber,
            Integer expectedVersion
    ) {
    }

    public record Settlement(
            BigDecimal amount,
            String currency,
            String structure,
            String releaseScope,
            String paymentInstructions
    ) {
    }

    public record SettlementApproval(
            String approvalAuthority,
            UUID authorizedBy,
            Integer expectedVersion
    ) {
    }

    public record SettlementExecution(
            UUID releaseDocumentId,
            String paymentInstructions,
            Integer expectedVersion
    ) {
    }

    /**
     * Links the payment that funds a settlement, or clears the link when
     * {@code paymentId} is null. The expert owns the settlement state checks and
     * stamps {@code paymentLinkedAt}.
     */
    public record SettlementPayment(UUID paymentId) {
    }

    public record Recovery(
            String recoveryType,
            BigDecimal amount,
            String currency,
            String source,
            String status,
            LocalDate recoveryDate,
            Integer expectedVersion
    ) {
    }

    public record Lien(
            String lienType,
            String lienholder,
            BigDecimal amount,
            String currency,
            String status,
            String resolutionType,
            Integer expectedVersion
    ) {
    }

    /** Command to open a diary task; the expert assigns the task number and OPEN status. */
    public record Task(
            String title,
            String description,
            LocalDate dueDate,
            String priority,
            String assignedTo
    ) {
    }

    /** Command to move a task to OPEN, COMPLETE, or CANCELLED at the observed version. */
    public record TaskStatusChange(
            String status,
            Long version
    ) {
    }

    /** Command to append an immutable claim note. */
    public record Note(
            String noteType,
            String body,
            String author
    ) {
    }

    /**
     * Command to assign or reassign a claim to an adjuster and, optionally, a
     * team. The expert owns the optimistic concurrency check: a stale
     * {@code expectedVersion} is a 409, never an overwrite.
     */
    public record Assignment(
            @NotBlank(message = "assignedAdjuster is required")
            @Size(max = 128, message = "assignedAdjuster must be at most 128 characters")
            String assignedAdjuster,

            @Size(max = 64, message = "assignedTeam must be at most 64 characters")
            String assignedTeam,

            @NotNull(message = "expectedVersion is required")
            Integer expectedVersion
    ) {
    }

    public record CloseClaim(Integer expectedVersion) {
    }
}
