package com.eia.directclaims.domain;

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

    public record CloseClaim(Integer expectedVersion) {
    }
}
