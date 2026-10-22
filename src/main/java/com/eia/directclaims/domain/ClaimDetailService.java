package com.eia.directclaims.domain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Public API for the Direct Claims second slice. The domain performs the
 * contract-level validation the caller can fix (required fields, positive
 * money, dates) and forwards to the expert, which owns the invariants.
 */
@Service
public class ClaimDetailService {
    private static final Set<String> COVERAGE_POSITIONS = Set.of("COVERED", "DENIED", "DEFENSE_ONLY");
    private static final Set<String> TERMINAL_LIEN_STATUSES = Set.of("RESOLVED", "WAIVED", "PAID");
    private static final Set<String> LIEN_RESOLUTIONS = Set.of("PAID", "NEGOTIATED", "WAIVED", "DEFERRED");
    private static final Set<String> TASK_PRIORITIES = Set.of("LOW", "NORMAL", "HIGH", "URGENT");
    private static final Set<String> TASK_STATUSES = Set.of("OPEN", "COMPLETE", "CANCELLED");
    private static final Set<String> NOTE_TYPES = Set.of(
            "DIARY", "COVERAGE", "RESERVE", "LITIGATION", "SETTLEMENT", "GENERAL");
    private static final int MIN_POLICY_YEAR = 1900;
    private static final int MAX_POLICY_YEAR = 2200;
    private static final int MAX_LAYER_LENGTH = 32;
    private static final String EROSION_POSTED = "POSTED";
    private static final String EROSION_FAILED = "FAILED";

    private static final Logger log = LoggerFactory.getLogger(ClaimDetailService.class);

    private final ClaimClient client;
    private final PolicyErosionClient policyClient;
    private final PolicyMassTortClient massTortClient;

    public ClaimDetailService(ClaimClient client,
                              PolicyErosionClient policyClient,
                              PolicyMassTortClient massTortClient) {
        this.client = client;
        this.policyClient = policyClient;
        this.massTortClient = massTortClient;
    }

    // ------------------------------------------------------------------ injuries
    public List<ClaimDetailDtos.Injury> injuries(UUID tenantId, UUID claimId) {
        return client.findInjuries(tenantId, claimId);
    }

    public ClaimDetailDtos.Injury addInjury(UUID tenantId, UUID claimId, ClaimCommands.Injury command) {
        requireId(command.claimantPartyId(), "claimantPartyId");
        requireText(command.injuryType(), "injuryType");
        requireText(command.severity(), "severity");
        return client.addInjury(tenantId, claimId, command);
    }

    public ClaimDetailDtos.Injury updateInjury(UUID tenantId, UUID claimId, UUID injuryId,
                                               ClaimCommands.Injury command) {
        requireExpectedVersion(command.expectedVersion());
        if (command.injuryType() != null) {
            requireText(command.injuryType(), "injuryType");
        }
        if (command.severity() != null) {
            requireText(command.severity(), "severity");
        }
        return client.updateInjury(tenantId, claimId, injuryId, command);
    }

    // ------------------------------------------------------------------ exposures
    public List<ClaimDetailDtos.Exposure> exposures(UUID tenantId, UUID claimId) {
        return client.findExposures(tenantId, claimId);
    }

    public ClaimDetailDtos.Exposure addExposure(UUID tenantId, UUID claimId,
                                                ClaimCommands.Exposure command) {
        requireId(command.claimantPartyId(), "claimantPartyId");
        requireText(command.exposureType(), "exposureType");
        requireText(command.jurisdiction(), "jurisdiction");
        requireDate(command.exposureFrom(), "exposureFrom");
        ClaimDto claim = client.findById(tenantId, claimId);
        validateExposureDates(claim, command.exposureFrom(), command.exposureTo());
        return client.addExposure(tenantId, claimId, command);
    }

    public ClaimDetailDtos.Exposure updateExposure(UUID tenantId, UUID claimId, UUID exposureId,
                                                   ClaimCommands.Exposure command) {
        requireExpectedVersion(command.expectedVersion());
        if (command.exposureType() != null) {
            requireText(command.exposureType(), "exposureType");
        }
        if (command.jurisdiction() != null) {
            requireText(command.jurisdiction(), "jurisdiction");
        }
        if (command.exposureFrom() != null || command.exposureTo() != null) {
            ClaimDto claim = client.findById(tenantId, claimId);
            validateExposureDates(claim, command.exposureFrom(), command.exposureTo());
        }
        return client.updateExposure(tenantId, claimId, exposureId, command);
    }

    private static void validateExposureDates(ClaimDto claim, LocalDate from, LocalDate to) {
        if (from != null && from.isAfter(claim.reportDate())) {
            throw new DomainValidationException("exposureFrom cannot be after reportDate "
                    + claim.reportDate());
        }
        if (to != null) {
            if (from != null && to.isBefore(from)) {
                throw new DomainValidationException("exposureTo cannot be before exposureFrom");
            }
            if (to.isAfter(claim.reportDate())) {
                throw new DomainValidationException("exposureTo cannot be after reportDate "
                        + claim.reportDate());
            }
        }
    }

    // ------------------------------------------------------------------ reserves
    public ClaimDetailDtos.ReserveSummary reserves(UUID tenantId, UUID claimId) {
        return client.findReserves(tenantId, claimId);
    }

    public ClaimDetailDtos.ReserveEntry changeReserve(UUID tenantId, UUID claimId,
                                                      ClaimCommands.ReserveChange command) {
        requireText(command.component(), "component");
        nonNegative(command.amount(), "amount");
        requireText(command.reasonCode(), "reasonCode");
        requireExpectedVersion(command.expectedVersion());
        return client.changeReserve(tenantId, claimId, command);
    }

    // ------------------------------------------------------------------ coverage
    public ClaimDetailDtos.CoverageDecision coverage(UUID tenantId, UUID claimId) {
        return client.findCoverage(tenantId, claimId);
    }

    public ClaimDetailDtos.CoverageDecision decideCoverage(UUID tenantId, UUID claimId,
                                                           ClaimCommands.CoverageDecision command) {
        String position = requireText(command.position(), "position");
        if (!COVERAGE_POSITIONS.contains(position.toUpperCase(Locale.ROOT))) {
            throw new DomainValidationException("Unknown position: " + command.position());
        }
        requireText(command.basis(), "basis");
        requireId(command.reviewerPartyId(), "reviewerPartyId");
        requireDate(command.decisionDate(), "decisionDate");
        BigDecimal pct = command.allocationPct();
        if (pct != null && (pct.signum() < 0 || pct.compareTo(BigDecimal.ONE) > 0)) {
            throw new DomainValidationException("allocationPct must be between 0 and 1");
        }
        return client.decideCoverage(tenantId, claimId, command);
    }

    // ------------------------------------------------------------------ allocations
    public ClaimDetailDtos.AllocationSummary allocations(UUID tenantId, UUID claimId) {
        return client.findAllocations(tenantId, claimId);
    }

    public ClaimDetailDtos.Allocation allocate(UUID tenantId, UUID claimId,
                                               ClaimCommands.Allocation command) {
        requireId(command.policyId(), "policyId");
        if (command.policyYear() == null) {
            throw new DomainValidationException("policyYear is required");
        }
        if (command.policyYear() < MIN_POLICY_YEAR || command.policyYear() > MAX_POLICY_YEAR) {
            throw new DomainValidationException("policyYear must be between " + MIN_POLICY_YEAR
                    + " and " + MAX_POLICY_YEAR);
        }
        String layer = requireText(command.layer(), "layer");
        if (layer.length() > MAX_LAYER_LENGTH) {
            throw new DomainValidationException("layer must be at most " + MAX_LAYER_LENGTH
                    + " characters");
        }
        requireAllocationPct(command.allocationPct());
        nonNegative(command.allocatedAmount(), "allocatedAmount");
        requireMassTortCoverageBlock(tenantId, command.policyId(), command.policyYear());
        ClaimDetailDtos.Allocation allocation = client.createAllocation(tenantId, claimId, command);
        if (command.limitId() == null) {
            return allocation;
        }
        return postErosion(tenantId, claimId, allocation, command.limitId());
    }

    /**
     * A COMPLETE Policy mass-tort profile bounds the policy years a long-tail
     * claim may be allocated to. The block is inclusive, so both the start and
     * end year are allowed. A DRAFT profile, a profile without a complete
     * block, an absent profile (404), and an unavailable Policy service all
     * leave the allocation allowed: the profile read must never block a claim
     * create by being unavailable.
     */
    private void requireMassTortCoverageBlock(UUID tenantId, UUID policyId, int policyYear) {
        Optional<PolicyMassTortClient.MassTortProfile> profile =
                massTortClient.findProfile(tenantId, policyId);
        if (profile.isEmpty()) {
            return;
        }
        PolicyMassTortClient.MassTortProfile massTort = profile.get();
        if (!massTort.isComplete() || !massTort.hasCoverageBlock()) {
            return;
        }
        if (policyYear < massTort.coverageBlockStart().getYear()
                || policyYear > massTort.coverageBlockEnd().getYear()) {
            throw new MassTortYearOutsideBlockException(policyYear,
                    massTort.coverageBlockStart(), massTort.coverageBlockEnd());
        }
    }

    /**
     * Retries the policy erosion posting for an existing allocation. The
     * allocation is read from the expert's tenant-scoped list so the policy
     * posting always uses the persisted policy, limit, and allocated amount.
     */
    public ClaimDetailDtos.Allocation retryErosion(UUID tenantId, UUID claimId, UUID allocationId) {
        ClaimDetailDtos.Allocation allocation = findAllocation(tenantId, claimId, allocationId);
        if (allocation.limitId() == null) {
            throw new DomainValidationException("limitId is required to retry erosion for allocation "
                    + allocationId);
        }
        return postErosion(tenantId, claimId, allocation, allocation.limitId());
    }

    /**
     * Posts the allocation's erosion fact to the Policy domain and records the
     * outcome on the expert allocation. A policy failure is not allowed to fail
     * the caller's create or retry: the allocation is marked {@code FAILED} with
     * the policy message so it can be posted again later.
     */
    private ClaimDetailDtos.Allocation postErosion(UUID tenantId,
                                                   UUID claimId,
                                                   ClaimDetailDtos.Allocation allocation,
                                                   UUID limitId) {
        PolicyErosionClient.PolicyErosionResult result;
        try {
            result = policyClient.postErosion(tenantId, allocation.policyId(), limitId, claimId,
                    allocation.allocationId(), allocation.allocatedAmount(), Instant.now());
        } catch (RuntimeException ex) {
            // The policy client normally normalises its own failures; this keeps
            // an unexpected client fault from failing the allocation create.
            log.warn("Policy erosion for allocation {} threw: {}", allocation.allocationId(),
                    ex.getMessage());
            result = PolicyErosionClient.PolicyErosionResult.failure(
                    failureMessage(ex.getMessage()));
        }
        String status = result.posted() ? EROSION_POSTED : EROSION_FAILED;
        try {
            return client.updateAllocationErosion(tenantId, claimId, allocation.allocationId(),
                    status, result.message());
        } catch (RestClientException ex) {
            log.warn("Could not record {} erosion for allocation {}: {}", status,
                    allocation.allocationId(), ex.getMessage());
            return allocation;
        }
    }

    private static String failureMessage(String message) {
        if (message == null || message.isBlank()) {
            return PolicyErosionClient.UNAVAILABLE_MESSAGE;
        }
        return message.length() > PolicyErosionClient.MAX_MESSAGE_LENGTH
                ? message.substring(0, PolicyErosionClient.MAX_MESSAGE_LENGTH)
                : message;
    }

    private ClaimDetailDtos.Allocation findAllocation(UUID tenantId, UUID claimId,
                                                      UUID allocationId) {
        ClaimDetailDtos.AllocationSummary summary = client.findAllocations(tenantId, claimId);
        if (summary != null && summary.items() != null) {
            for (ClaimDetailDtos.Allocation allocation : summary.items()) {
                if (allocationId.equals(allocation.allocationId())) {
                    return allocation;
                }
            }
        }
        throw new AllocationNotFoundException(allocationId);
    }

    /**
     * The allocation percentage is a fraction in (0, 1]. It is rounded to six
     * decimals first so a value that rounds to zero or above one is rejected
     * exactly like the expert's numeric(9,6) column.
     */
    private static void requireAllocationPct(BigDecimal value) {
        if (value == null) {
            throw new DomainValidationException("allocationPct is required");
        }
        BigDecimal scaled = value.setScale(6, RoundingMode.HALF_UP);
        if (value.signum() <= 0 || value.compareTo(BigDecimal.ONE) > 0
                || scaled.signum() <= 0 || scaled.compareTo(BigDecimal.ONE) > 0) {
            throw new DomainValidationException(
                    "allocationPct must be greater than 0 and at most 1");
        }
    }

    // ------------------------------------------------------------------ litigation
    public List<ClaimDetailDtos.Litigation> litigation(UUID tenantId, UUID claimId) {
        return client.findLitigation(tenantId, claimId);
    }

    public ClaimDetailDtos.Litigation addLitigation(UUID tenantId, UUID claimId,
                                                    ClaimCommands.Litigation command) {
        requireText(command.matterType(), "matterType");
        requireText(command.jurisdiction(), "jurisdiction");
        requireId(command.counselPartyId(), "counselPartyId");
        requireDate(command.filingDate(), "filingDate");
        requireText(command.status(), "status");
        if (command.trialDate() != null && command.trialDate().isBefore(command.filingDate())) {
            throw new DomainValidationException("trialDate cannot be before filingDate");
        }
        return client.addLitigation(tenantId, claimId, command);
    }

    public ClaimDetailDtos.Litigation updateLitigation(UUID tenantId, UUID claimId, UUID litigationId,
                                                       ClaimCommands.Litigation command) {
        requireExpectedVersion(command.expectedVersion());
        return client.updateLitigation(tenantId, claimId, litigationId, command);
    }

    // ------------------------------------------------------------------ settlements
    public List<ClaimDetailDtos.Settlement> settlements(UUID tenantId, UUID claimId) {
        return client.findSettlements(tenantId, claimId);
    }

    public ClaimDetailDtos.Settlement proposeSettlement(UUID tenantId, UUID claimId,
                                                        ClaimCommands.Settlement command) {
        positive(command.amount(), "amount");
        requireText(command.structure(), "structure");
        return client.proposeSettlement(tenantId, claimId, command);
    }

    public ClaimDetailDtos.Settlement approveSettlement(UUID tenantId, UUID claimId, UUID settlementId,
                                                        ClaimCommands.SettlementApproval command) {
        requireText(command.approvalAuthority(), "approvalAuthority");
        requireId(command.authorizedBy(), "authorizedBy");
        requireExpectedVersion(command.expectedVersion());
        return client.approveSettlement(tenantId, claimId, settlementId, command);
    }

    public ClaimDetailDtos.Settlement executeSettlement(UUID tenantId, UUID claimId, UUID settlementId,
                                                        ClaimCommands.SettlementExecution command) {
        requireId(command.releaseDocumentId(), "releaseDocumentId");
        requireExpectedVersion(command.expectedVersion());
        return client.executeSettlement(tenantId, claimId, settlementId, command);
    }

    /**
     * Links the settlement to its payment, or clears the link when
     * {@code paymentId} is null. The expert owns the settlement state and
     * duplicate-link checks; the domain forwards the command unchanged.
     */
    public ClaimDetailDtos.Settlement linkSettlementPayment(UUID tenantId,
                                                            UUID claimId,
                                                            UUID settlementId,
                                                            ClaimCommands.SettlementPayment command) {
        return client.linkSettlementPayment(tenantId, claimId, settlementId, command);
    }

    // ------------------------------------------------------------------ recoveries
    public List<ClaimDetailDtos.Recovery> recoveries(UUID tenantId, UUID claimId) {
        return client.findRecoveries(tenantId, claimId);
    }

    public ClaimDetailDtos.Recovery recordRecovery(UUID tenantId, UUID claimId,
                                                   ClaimCommands.Recovery command) {
        requireText(command.recoveryType(), "recoveryType");
        positive(command.amount(), "amount");
        requireText(command.source(), "source");
        String status = requireText(command.status(), "status");
        if ("RECEIVED".equals(status.toUpperCase(Locale.ROOT))) {
            requireDate(command.recoveryDate(), "recoveryDate");
        }
        return client.recordRecovery(tenantId, claimId, command);
    }

    public ClaimDetailDtos.Recovery updateRecovery(UUID tenantId, UUID claimId, UUID recoveryId,
                                                   ClaimCommands.Recovery command) {
        requireExpectedVersion(command.expectedVersion());
        if (command.status() != null && "RECEIVED".equals(command.status().toUpperCase(Locale.ROOT))) {
            requireDate(command.recoveryDate(), "recoveryDate");
        }
        return client.updateRecovery(tenantId, claimId, recoveryId, command);
    }

    // ------------------------------------------------------------------ liens
    public List<ClaimDetailDtos.Lien> liens(UUID tenantId, UUID claimId) {
        return client.findLiens(tenantId, claimId);
    }

    public ClaimDetailDtos.Lien recordLien(UUID tenantId, UUID claimId, ClaimCommands.Lien command) {
        requireText(command.lienType(), "lienType");
        requireText(command.lienholder(), "lienholder");
        nonNegative(command.amount(), "amount");
        String status = requireText(command.status(), "status").toUpperCase(Locale.ROOT);
        if (TERMINAL_LIEN_STATUSES.contains(status)) {
            String resolution = requireText(command.resolutionType(), "resolutionType");
            if (!LIEN_RESOLUTIONS.contains(resolution.toUpperCase(Locale.ROOT))) {
                throw new DomainValidationException("Unknown resolutionType: " + command.resolutionType());
            }
        }
        return client.recordLien(tenantId, claimId, command);
    }

    public ClaimDetailDtos.Lien updateLien(UUID tenantId, UUID claimId, UUID lienId,
                                           ClaimCommands.Lien command) {
        requireExpectedVersion(command.expectedVersion());
        if (command.status() != null && TERMINAL_LIEN_STATUSES.contains(
                command.status().toUpperCase(Locale.ROOT))) {
            requireText(command.resolutionType(), "resolutionType");
        }
        return client.updateLien(tenantId, claimId, lienId, command);
    }

    // ------------------------------------------------------------------ tasks
    public List<ClaimDetailDtos.Task> tasks(UUID tenantId, UUID claimId) {
        return client.findTasks(tenantId, claimId);
    }

    public ClaimDetailDtos.Task addTask(UUID tenantId, UUID claimId, ClaimCommands.Task command) {
        validateTask(command);
        return client.addTask(tenantId, claimId, command);
    }

    /**
     * Forwards the optional Idempotency-Key and keeps the expert's replay
     * marker so the controller can return it to the caller.
     */
    public ClaimCommandResult<ClaimDetailDtos.Task> addTask(UUID tenantId,
                                                            UUID claimId,
                                                            ClaimCommands.Task command,
                                                            String idempotencyKey) {
        validateTask(command);
        return client.addTask(tenantId, claimId, command, idempotencyKey);
    }

    private static void validateTask(ClaimCommands.Task command) {
        requireText(command.title(), "title");
        requireDate(command.dueDate(), "dueDate");
        String priority = requireText(command.priority(), "priority");
        if (!TASK_PRIORITIES.contains(priority.toUpperCase(Locale.ROOT))) {
            throw new DomainValidationException("Unknown priority: " + command.priority());
        }
    }

    public ClaimDetailDtos.Task changeTaskStatus(UUID tenantId, UUID claimId, UUID taskId,
                                                 ClaimCommands.TaskStatusChange command) {
        String status = requireText(command.status(), "status");
        if (!TASK_STATUSES.contains(status.toUpperCase(Locale.ROOT))) {
            throw new DomainValidationException("Unknown task status: " + command.status());
        }
        if (command.version() == null) {
            throw new DomainValidationException("version is required");
        }
        return client.changeTaskStatus(tenantId, claimId, taskId, command);
    }

    // ------------------------------------------------------------------ notes
    public List<ClaimDetailDtos.Note> notes(UUID tenantId, UUID claimId) {
        return client.findNotes(tenantId, claimId);
    }

    public ClaimDetailDtos.Note addNote(UUID tenantId, UUID claimId, ClaimCommands.Note command) {
        validateNote(command);
        return client.addNote(tenantId, claimId, command);
    }

    /**
     * Forwards the optional Idempotency-Key and keeps the expert's replay
     * marker so the controller can return it to the caller.
     */
    public ClaimCommandResult<ClaimDetailDtos.Note> addNote(UUID tenantId,
                                                            UUID claimId,
                                                            ClaimCommands.Note command,
                                                            String idempotencyKey) {
        validateNote(command);
        return client.addNote(tenantId, claimId, command, idempotencyKey);
    }

    private static void validateNote(ClaimCommands.Note command) {
        String noteType = requireText(command.noteType(), "noteType");
        if (!NOTE_TYPES.contains(noteType.toUpperCase(Locale.ROOT))) {
            throw new DomainValidationException("Unknown noteType: " + command.noteType());
        }
        requireText(command.body(), "body");
        requireText(command.author(), "author");
    }

    // ------------------------------------------------------------------ helpers
    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new DomainValidationException(field + " is required");
        }
        return value.trim();
    }

    private static UUID requireId(UUID value, String field) {
        if (value == null) {
            throw new DomainValidationException(field + " is required");
        }
        return value;
    }

    private static LocalDate requireDate(LocalDate value, String field) {
        if (value == null) {
            throw new DomainValidationException(field + " is required");
        }
        return value;
    }

    private static void requireExpectedVersion(Integer expectedVersion) {
        if (expectedVersion == null) {
            throw new DomainValidationException("expectedVersion is required");
        }
    }

    private static void nonNegative(BigDecimal value, String field) {
        if (value == null) {
            throw new DomainValidationException(field + " is required");
        }
        if (value.signum() < 0) {
            throw new DomainValidationException(field + " must be zero or greater");
        }
    }

    private static void positive(BigDecimal value, String field) {
        if (value == null) {
            throw new DomainValidationException(field + " is required");
        }
        if (value.signum() <= 0) {
            throw new DomainValidationException(field + " must be greater than zero");
        }
    }
}
