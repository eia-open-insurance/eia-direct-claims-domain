package com.eia.directclaims.domain;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Public Direct Claims second-slice API. Every route keeps the DIRECT book wall
 * and takes the tenant from X-EIA-Tenant; any other book is a 404.
 */
@RestController
@RequestMapping("/api/v1/claims/{claimId}")
public class ClaimDetailController {
    private final ClaimDetailService service;

    public ClaimDetailController(ClaimDetailService service) {
        this.service = service;
    }

    // ------------------------------------------------------------------ injuries
    @GetMapping("/injuries")
    public List<ClaimDetailDtos.Injury> injuries(
            @RequestHeader(value = "X-EIA-Tenant", required = false) String tenantHeader,
            @RequestHeader(value = "X-EIA-Book", required = false) String book,
            @PathVariable UUID claimId) {
        RequestGuard.requireDirectBook(book);
        return service.injuries(RequestGuard.requireTenant(tenantHeader), claimId);
    }

    @PostMapping("/injuries")
    @ResponseStatus(HttpStatus.CREATED)
    public ClaimDetailDtos.Injury addInjury(
            @RequestHeader(value = "X-EIA-Tenant", required = false) String tenantHeader,
            @RequestHeader(value = "X-EIA-Book", required = false) String book,
            @PathVariable UUID claimId,
            @Valid @RequestBody ClaimCommands.Injury command) {
        RequestGuard.requireDirectBook(book);
        return service.addInjury(RequestGuard.requireTenant(tenantHeader), claimId, command);
    }

    @PatchMapping("/injuries/{injuryId}")
    public ClaimDetailDtos.Injury updateInjury(
            @RequestHeader(value = "X-EIA-Tenant", required = false) String tenantHeader,
            @RequestHeader(value = "X-EIA-Book", required = false) String book,
            @PathVariable UUID claimId,
            @PathVariable UUID injuryId,
            @Valid @RequestBody ClaimCommands.Injury command) {
        RequestGuard.requireDirectBook(book);
        return service.updateInjury(RequestGuard.requireTenant(tenantHeader), claimId, injuryId, command);
    }

    // ------------------------------------------------------------------ exposures
    @GetMapping("/exposures")
    public List<ClaimDetailDtos.Exposure> exposures(
            @RequestHeader(value = "X-EIA-Tenant", required = false) String tenantHeader,
            @RequestHeader(value = "X-EIA-Book", required = false) String book,
            @PathVariable UUID claimId) {
        RequestGuard.requireDirectBook(book);
        return service.exposures(RequestGuard.requireTenant(tenantHeader), claimId);
    }

    @PostMapping("/exposures")
    @ResponseStatus(HttpStatus.CREATED)
    public ClaimDetailDtos.Exposure addExposure(
            @RequestHeader(value = "X-EIA-Tenant", required = false) String tenantHeader,
            @RequestHeader(value = "X-EIA-Book", required = false) String book,
            @PathVariable UUID claimId,
            @Valid @RequestBody ClaimCommands.Exposure command) {
        RequestGuard.requireDirectBook(book);
        return service.addExposure(RequestGuard.requireTenant(tenantHeader), claimId, command);
    }

    @PatchMapping("/exposures/{exposureId}")
    public ClaimDetailDtos.Exposure updateExposure(
            @RequestHeader(value = "X-EIA-Tenant", required = false) String tenantHeader,
            @RequestHeader(value = "X-EIA-Book", required = false) String book,
            @PathVariable UUID claimId,
            @PathVariable UUID exposureId,
            @Valid @RequestBody ClaimCommands.Exposure command) {
        RequestGuard.requireDirectBook(book);
        return service.updateExposure(RequestGuard.requireTenant(tenantHeader), claimId, exposureId, command);
    }

    // ------------------------------------------------------------------ reserves
    @GetMapping("/reserves")
    public ClaimDetailDtos.ReserveSummary reserves(
            @RequestHeader(value = "X-EIA-Tenant", required = false) String tenantHeader,
            @RequestHeader(value = "X-EIA-Book", required = false) String book,
            @PathVariable UUID claimId) {
        RequestGuard.requireDirectBook(book);
        return service.reserves(RequestGuard.requireTenant(tenantHeader), claimId);
    }

    @PostMapping("/reserves")
    @ResponseStatus(HttpStatus.CREATED)
    public ClaimDetailDtos.ReserveEntry changeReserve(
            @RequestHeader(value = "X-EIA-Tenant", required = false) String tenantHeader,
            @RequestHeader(value = "X-EIA-Book", required = false) String book,
            @PathVariable UUID claimId,
            @Valid @RequestBody ClaimCommands.ReserveChange command) {
        RequestGuard.requireDirectBook(book);
        return service.changeReserve(RequestGuard.requireTenant(tenantHeader), claimId, command);
    }

    // ------------------------------------------------------------------ coverage
    @GetMapping("/coverage")
    public ClaimDetailDtos.CoverageDecision coverage(
            @RequestHeader(value = "X-EIA-Tenant", required = false) String tenantHeader,
            @RequestHeader(value = "X-EIA-Book", required = false) String book,
            @PathVariable UUID claimId) {
        RequestGuard.requireDirectBook(book);
        return service.coverage(RequestGuard.requireTenant(tenantHeader), claimId);
    }

    @PostMapping("/coverage")
    @ResponseStatus(HttpStatus.CREATED)
    public ClaimDetailDtos.CoverageDecision decideCoverage(
            @RequestHeader(value = "X-EIA-Tenant", required = false) String tenantHeader,
            @RequestHeader(value = "X-EIA-Book", required = false) String book,
            @PathVariable UUID claimId,
            @Valid @RequestBody ClaimCommands.CoverageDecision command) {
        RequestGuard.requireDirectBook(book);
        return service.decideCoverage(RequestGuard.requireTenant(tenantHeader), claimId, command);
    }

    // ------------------------------------------------------------------ allocations
    @GetMapping("/allocations")
    public ClaimDetailDtos.AllocationSummary allocations(
            @RequestHeader(value = "X-EIA-Tenant", required = false) String tenantHeader,
            @RequestHeader(value = "X-EIA-Book", required = false) String book,
            @PathVariable UUID claimId) {
        RequestGuard.requireDirectBook(book);
        return service.allocations(RequestGuard.requireTenant(tenantHeader), claimId);
    }

    @PostMapping("/allocations")
    @ResponseStatus(HttpStatus.CREATED)
    public ClaimDetailDtos.Allocation allocate(
            @RequestHeader(value = "X-EIA-Tenant", required = false) String tenantHeader,
            @RequestHeader(value = "X-EIA-Book", required = false) String book,
            @PathVariable UUID claimId,
            @Valid @RequestBody ClaimCommands.Allocation command) {
        RequestGuard.requireDirectBook(book);
        return service.allocate(RequestGuard.requireTenant(tenantHeader), claimId, command);
    }

    /**
     * Retries the policy erosion posting for an allocation recorded on the
     * claim. The allocation is located through the expert list, posted to the
     * Policy domain, and returned with its resulting erosion status.
     */
    @PostMapping("/allocations/{allocationId}/erosion")
    public ClaimDetailDtos.Allocation retryAllocationErosion(
            @RequestHeader(value = "X-EIA-Tenant", required = false) String tenantHeader,
            @RequestHeader(value = "X-EIA-Book", required = false) String book,
            @PathVariable UUID claimId,
            @PathVariable UUID allocationId) {
        RequestGuard.requireDirectBook(book);
        return service.retryErosion(RequestGuard.requireTenant(tenantHeader), claimId, allocationId);
    }

    // ------------------------------------------------------------------ litigation
    @GetMapping("/litigation")
    public List<ClaimDetailDtos.Litigation> litigation(
            @RequestHeader(value = "X-EIA-Tenant", required = false) String tenantHeader,
            @RequestHeader(value = "X-EIA-Book", required = false) String book,
            @PathVariable UUID claimId) {
        RequestGuard.requireDirectBook(book);
        return service.litigation(RequestGuard.requireTenant(tenantHeader), claimId);
    }

    @PostMapping("/litigation")
    @ResponseStatus(HttpStatus.CREATED)
    public ClaimDetailDtos.Litigation addLitigation(
            @RequestHeader(value = "X-EIA-Tenant", required = false) String tenantHeader,
            @RequestHeader(value = "X-EIA-Book", required = false) String book,
            @PathVariable UUID claimId,
            @Valid @RequestBody ClaimCommands.Litigation command) {
        RequestGuard.requireDirectBook(book);
        return service.addLitigation(RequestGuard.requireTenant(tenantHeader), claimId, command);
    }

    @PatchMapping("/litigation/{litigationId}")
    public ClaimDetailDtos.Litigation updateLitigation(
            @RequestHeader(value = "X-EIA-Tenant", required = false) String tenantHeader,
            @RequestHeader(value = "X-EIA-Book", required = false) String book,
            @PathVariable UUID claimId,
            @PathVariable UUID litigationId,
            @Valid @RequestBody ClaimCommands.Litigation command) {
        RequestGuard.requireDirectBook(book);
        return service.updateLitigation(RequestGuard.requireTenant(tenantHeader), claimId, litigationId,
                command);
    }

    // ------------------------------------------------------------------ settlements
    @GetMapping("/settlements")
    public List<ClaimDetailDtos.Settlement> settlements(
            @RequestHeader(value = "X-EIA-Tenant", required = false) String tenantHeader,
            @RequestHeader(value = "X-EIA-Book", required = false) String book,
            @PathVariable UUID claimId) {
        RequestGuard.requireDirectBook(book);
        return service.settlements(RequestGuard.requireTenant(tenantHeader), claimId);
    }

    @PostMapping("/settlements")
    @ResponseStatus(HttpStatus.CREATED)
    public ClaimDetailDtos.Settlement proposeSettlement(
            @RequestHeader(value = "X-EIA-Tenant", required = false) String tenantHeader,
            @RequestHeader(value = "X-EIA-Book", required = false) String book,
            @PathVariable UUID claimId,
            @Valid @RequestBody ClaimCommands.Settlement command) {
        RequestGuard.requireDirectBook(book);
        return service.proposeSettlement(RequestGuard.requireTenant(tenantHeader), claimId, command);
    }

    @PostMapping("/settlements/{settlementId}/approve")
    public ClaimDetailDtos.Settlement approveSettlement(
            @RequestHeader(value = "X-EIA-Tenant", required = false) String tenantHeader,
            @RequestHeader(value = "X-EIA-Book", required = false) String book,
            @PathVariable UUID claimId,
            @PathVariable UUID settlementId,
            @Valid @RequestBody ClaimCommands.SettlementApproval command) {
        RequestGuard.requireDirectBook(book);
        return service.approveSettlement(RequestGuard.requireTenant(tenantHeader), claimId, settlementId,
                command);
    }

    @PostMapping("/settlements/{settlementId}/execute")
    public ClaimDetailDtos.Settlement executeSettlement(
            @RequestHeader(value = "X-EIA-Tenant", required = false) String tenantHeader,
            @RequestHeader(value = "X-EIA-Book", required = false) String book,
            @PathVariable UUID claimId,
            @PathVariable UUID settlementId,
            @Valid @RequestBody ClaimCommands.SettlementExecution command) {
        RequestGuard.requireDirectBook(book);
        return service.executeSettlement(RequestGuard.requireTenant(tenantHeader), claimId, settlementId,
                command);
    }

    /**
     * Links the payment that funds a settlement, or clears the link when
     * {@code paymentId} is null. The domain forwards the tenant and DIRECT book
     * to the expert, which owns the state checks.
     */
    @PatchMapping("/settlements/{settlementId}/payment")
    public ClaimDetailDtos.Settlement linkSettlementPayment(
            @RequestHeader(value = "X-EIA-Tenant", required = false) String tenantHeader,
            @RequestHeader(value = "X-EIA-Book", required = false) String book,
            @PathVariable UUID claimId,
            @PathVariable UUID settlementId,
            @Valid @RequestBody ClaimCommands.SettlementPayment command) {
        RequestGuard.requireDirectBook(book);
        return service.linkSettlementPayment(RequestGuard.requireTenant(tenantHeader), claimId,
                settlementId, command);
    }

    // ------------------------------------------------------------------ recoveries
    @GetMapping("/recoveries")
    public List<ClaimDetailDtos.Recovery> recoveries(
            @RequestHeader(value = "X-EIA-Tenant", required = false) String tenantHeader,
            @RequestHeader(value = "X-EIA-Book", required = false) String book,
            @PathVariable UUID claimId) {
        RequestGuard.requireDirectBook(book);
        return service.recoveries(RequestGuard.requireTenant(tenantHeader), claimId);
    }

    @PostMapping("/recoveries")
    @ResponseStatus(HttpStatus.CREATED)
    public ClaimDetailDtos.Recovery recordRecovery(
            @RequestHeader(value = "X-EIA-Tenant", required = false) String tenantHeader,
            @RequestHeader(value = "X-EIA-Book", required = false) String book,
            @PathVariable UUID claimId,
            @Valid @RequestBody ClaimCommands.Recovery command) {
        RequestGuard.requireDirectBook(book);
        return service.recordRecovery(RequestGuard.requireTenant(tenantHeader), claimId, command);
    }

    @PatchMapping("/recoveries/{recoveryId}")
    public ClaimDetailDtos.Recovery updateRecovery(
            @RequestHeader(value = "X-EIA-Tenant", required = false) String tenantHeader,
            @RequestHeader(value = "X-EIA-Book", required = false) String book,
            @PathVariable UUID claimId,
            @PathVariable UUID recoveryId,
            @Valid @RequestBody ClaimCommands.Recovery command) {
        RequestGuard.requireDirectBook(book);
        return service.updateRecovery(RequestGuard.requireTenant(tenantHeader), claimId, recoveryId, command);
    }

    // ------------------------------------------------------------------ liens
    @GetMapping("/liens")
    public List<ClaimDetailDtos.Lien> liens(
            @RequestHeader(value = "X-EIA-Tenant", required = false) String tenantHeader,
            @RequestHeader(value = "X-EIA-Book", required = false) String book,
            @PathVariable UUID claimId) {
        RequestGuard.requireDirectBook(book);
        return service.liens(RequestGuard.requireTenant(tenantHeader), claimId);
    }

    @PostMapping("/liens")
    @ResponseStatus(HttpStatus.CREATED)
    public ClaimDetailDtos.Lien recordLien(
            @RequestHeader(value = "X-EIA-Tenant", required = false) String tenantHeader,
            @RequestHeader(value = "X-EIA-Book", required = false) String book,
            @PathVariable UUID claimId,
            @Valid @RequestBody ClaimCommands.Lien command) {
        RequestGuard.requireDirectBook(book);
        return service.recordLien(RequestGuard.requireTenant(tenantHeader), claimId, command);
    }

    @PatchMapping("/liens/{lienId}")
    public ClaimDetailDtos.Lien updateLien(
            @RequestHeader(value = "X-EIA-Tenant", required = false) String tenantHeader,
            @RequestHeader(value = "X-EIA-Book", required = false) String book,
            @PathVariable UUID claimId,
            @PathVariable UUID lienId,
            @Valid @RequestBody ClaimCommands.Lien command) {
        RequestGuard.requireDirectBook(book);
        return service.updateLien(RequestGuard.requireTenant(tenantHeader), claimId, lienId, command);
    }

    // ------------------------------------------------------------------ tasks
    @GetMapping("/tasks")
    public List<ClaimDetailDtos.Task> tasks(
            @RequestHeader(value = "X-EIA-Tenant", required = false) String tenantHeader,
            @RequestHeader(value = "X-EIA-Book", required = false) String book,
            @PathVariable UUID claimId) {
        RequestGuard.requireDirectBook(book);
        return service.tasks(RequestGuard.requireTenant(tenantHeader), claimId);
    }

    @PostMapping("/tasks")
    public ResponseEntity<ClaimDetailDtos.Task> addTask(
            @RequestHeader(value = "X-EIA-Tenant", required = false) String tenantHeader,
            @RequestHeader(value = "X-EIA-Book", required = false) String book,
            @PathVariable UUID claimId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody ClaimCommands.Task command) {
        RequestGuard.requireDirectBook(book);
        ClaimCommandResult<ClaimDetailDtos.Task> result = service.addTask(
                RequestGuard.requireTenant(tenantHeader), claimId, command, idempotencyKey);
        ResponseEntity.BodyBuilder response = ResponseEntity.status(HttpStatus.CREATED);
        if (result.replayHeader() != null) {
            response.header("Idempotent-Replay", result.replayHeader());
        }
        return response.body(result.value());
    }

    @PatchMapping("/tasks/{taskId}")
    public ClaimDetailDtos.Task changeTaskStatus(
            @RequestHeader(value = "X-EIA-Tenant", required = false) String tenantHeader,
            @RequestHeader(value = "X-EIA-Book", required = false) String book,
            @PathVariable UUID claimId,
            @PathVariable UUID taskId,
            @Valid @RequestBody ClaimCommands.TaskStatusChange command) {
        RequestGuard.requireDirectBook(book);
        return service.changeTaskStatus(RequestGuard.requireTenant(tenantHeader), claimId, taskId,
                command);
    }

    // ------------------------------------------------------------------ notes
    @GetMapping("/notes")
    public List<ClaimDetailDtos.Note> notes(
            @RequestHeader(value = "X-EIA-Tenant", required = false) String tenantHeader,
            @RequestHeader(value = "X-EIA-Book", required = false) String book,
            @PathVariable UUID claimId) {
        RequestGuard.requireDirectBook(book);
        return service.notes(RequestGuard.requireTenant(tenantHeader), claimId);
    }

    @PostMapping("/notes")
    public ResponseEntity<ClaimDetailDtos.Note> addNote(
            @RequestHeader(value = "X-EIA-Tenant", required = false) String tenantHeader,
            @RequestHeader(value = "X-EIA-Book", required = false) String book,
            @PathVariable UUID claimId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody ClaimCommands.Note command) {
        RequestGuard.requireDirectBook(book);
        ClaimCommandResult<ClaimDetailDtos.Note> result = service.addNote(
                RequestGuard.requireTenant(tenantHeader), claimId, command, idempotencyKey);
        ResponseEntity.BodyBuilder response = ResponseEntity.status(HttpStatus.CREATED);
        if (result.replayHeader() != null) {
            response.header("Idempotent-Replay", result.replayHeader());
        }
        return response.body(result.value());
    }
}
