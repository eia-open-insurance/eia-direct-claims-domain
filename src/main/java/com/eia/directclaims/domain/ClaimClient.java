package com.eia.directclaims.domain;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.UUID;

@Component
public class ClaimClient {
    private final RestClient client;

    public ClaimClient(RestClient.Builder builder,
                       @Value("${eia.direct-claims.expert-url:http://localhost:8087}") String baseUrl) {
        this.client = builder
                .baseUrl(baseUrl)
                .defaultHeader("X-EIA-Book", "DIRECT")
                .build();
    }

    public List<ClaimDto> findAll(UUID tenantId, ClaimStatus status) {
        return client.get()
                .uri(uri -> {
                    uri.path("/internal/v1/claims").queryParam("tenantId", tenantId);
                    if (status != null) {
                        uri.queryParam("status", status.name());
                    }
                    return uri.build();
                })
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
    }

    public ClaimDto findById(UUID tenantId, UUID id) {
        return client.get()
                .uri(uri -> uri.path("/internal/v1/claims/{id}")
                        .queryParam("tenantId", tenantId)
                        .build(id))
                .retrieve()
                .body(ClaimDto.class);
    }

    public ClaimDto create(UUID tenantId, CreateClaimCommand command) {
        return client.post()
                .uri("/internal/v1/claims")
                .body(new CreateClaimRequest(tenantId, command.policyId(), command.claimantPartyId(),
                        command.lossDate(), command.reportDate(), command.causeCode(),
                        command.claimType(), command.reserveAmount(), command.reserveCurrency(),
                        command.settlementAuthorityAmount(), command.settlementAuthorityCurrency()))
                .retrieve()
                .body(ClaimDto.class);
    }

    public ClaimDto changeStatus(UUID tenantId, UUID id, ClaimStatus status, int expectedVersion) {
        return client.patch()
                .uri(uri -> uri.path("/internal/v1/claims/{id}/status").build(id))
                .header("X-EIA-Tenant", tenantId.toString())
                .body(new StatusChangeRequest(status.name(), expectedVersion))
                .retrieve()
                .body(ClaimDto.class);
    }

    public ClaimDto close(UUID tenantId, UUID id, Integer expectedVersion) {
        return client.post()
                .uri(uri -> uri.path("/internal/v1/claims/{id}/close").build(id))
                .header("X-EIA-Tenant", tenantId.toString())
                .body(new CloseClaimRequest(expectedVersion))
                .retrieve()
                .body(ClaimDto.class);
    }

    /**
     * Assigns the claim at the version the caller observed. The DIRECT book is
     * a client-wide default header; the optional actor is forwarded only when
     * the caller supplied one. The expert keeps ownership of the 400/404/409
     * problem it returns.
     */
    public ClaimDto assign(UUID tenantId, UUID claimId, ClaimCommands.Assignment command, String actor) {
        return client.patch()
                .uri(uri -> uri.path("/internal/v1/claims/{id}/assignment").build(claimId))
                .header("X-EIA-Tenant", tenantId.toString())
                .headers(headers -> {
                    if (actor != null && !actor.isBlank()) {
                        headers.set("X-EIA-Actor", actor.trim());
                    }
                })
                .body(new AssignmentRequest(command.assignedAdjuster(), command.assignedTeam(),
                        command.expectedVersion()))
                .retrieve()
                .body(ClaimDto.class);
    }

    // ------------------------------------------------------------------ injuries
    public List<ClaimDetailDtos.Injury> findInjuries(UUID tenantId, UUID claimId) {
        return client.get()
                .uri(uri -> uri.path("/internal/v1/claims/{id}/injuries")
                        .queryParam("tenantId", tenantId)
                        .build(claimId))
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
    }

    public ClaimDetailDtos.Injury addInjury(UUID tenantId, UUID claimId, ClaimCommands.Injury command) {
        return client.post()
                .uri(uri -> uri.path("/internal/v1/claims/{id}/injuries").build(claimId))
                .header("X-EIA-Tenant", tenantId.toString())
                .body(command)
                .retrieve()
                .body(ClaimDetailDtos.Injury.class);
    }

    public ClaimDetailDtos.Injury updateInjury(UUID tenantId, UUID claimId, UUID injuryId,
                                               ClaimCommands.Injury command) {
        return client.patch()
                .uri(uri -> uri.path("/internal/v1/claims/{id}/injuries/{injuryId}")
                        .build(claimId, injuryId))
                .header("X-EIA-Tenant", tenantId.toString())
                .body(command)
                .retrieve()
                .body(ClaimDetailDtos.Injury.class);
    }

    // ------------------------------------------------------------------ exposures
    public List<ClaimDetailDtos.Exposure> findExposures(UUID tenantId, UUID claimId) {
        return client.get()
                .uri(uri -> uri.path("/internal/v1/claims/{id}/exposures")
                        .queryParam("tenantId", tenantId)
                        .build(claimId))
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
    }

    public ClaimDetailDtos.Exposure addExposure(UUID tenantId, UUID claimId,
                                                ClaimCommands.Exposure command) {
        return client.post()
                .uri(uri -> uri.path("/internal/v1/claims/{id}/exposures").build(claimId))
                .header("X-EIA-Tenant", tenantId.toString())
                .body(command)
                .retrieve()
                .body(ClaimDetailDtos.Exposure.class);
    }

    public ClaimDetailDtos.Exposure updateExposure(UUID tenantId, UUID claimId, UUID exposureId,
                                                   ClaimCommands.Exposure command) {
        return client.patch()
                .uri(uri -> uri.path("/internal/v1/claims/{id}/exposures/{exposureId}")
                        .build(claimId, exposureId))
                .header("X-EIA-Tenant", tenantId.toString())
                .body(command)
                .retrieve()
                .body(ClaimDetailDtos.Exposure.class);
    }

    // ------------------------------------------------------------------ reserves
    public ClaimDetailDtos.ReserveSummary findReserves(UUID tenantId, UUID claimId) {
        return client.get()
                .uri(uri -> uri.path("/internal/v1/claims/{id}/reserves")
                        .queryParam("tenantId", tenantId)
                        .build(claimId))
                .retrieve()
                .body(ClaimDetailDtos.ReserveSummary.class);
    }

    public ClaimDetailDtos.ReserveEntry changeReserve(UUID tenantId, UUID claimId,
                                                      ClaimCommands.ReserveChange command) {
        return client.post()
                .uri(uri -> uri.path("/internal/v1/claims/{id}/reserves").build(claimId))
                .header("X-EIA-Tenant", tenantId.toString())
                .body(command)
                .retrieve()
                .body(ClaimDetailDtos.ReserveEntry.class);
    }

    // ------------------------------------------------------------------ coverage
    public ClaimDetailDtos.CoverageDecision findCoverage(UUID tenantId, UUID claimId) {
        return client.get()
                .uri(uri -> uri.path("/internal/v1/claims/{id}/coverage")
                        .queryParam("tenantId", tenantId)
                        .build(claimId))
                .retrieve()
                .body(ClaimDetailDtos.CoverageDecision.class);
    }

    public ClaimDetailDtos.CoverageDecision decideCoverage(UUID tenantId, UUID claimId,
                                                           ClaimCommands.CoverageDecision command) {
        return client.post()
                .uri(uri -> uri.path("/internal/v1/claims/{id}/coverage").build(claimId))
                .header("X-EIA-Tenant", tenantId.toString())
                .body(command)
                .retrieve()
                .body(ClaimDetailDtos.CoverageDecision.class);
    }

    // ------------------------------------------------------------------ litigation
    public List<ClaimDetailDtos.Litigation> findLitigation(UUID tenantId, UUID claimId) {
        return client.get()
                .uri(uri -> uri.path("/internal/v1/claims/{id}/litigation")
                        .queryParam("tenantId", tenantId)
                        .build(claimId))
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
    }

    public ClaimDetailDtos.Litigation addLitigation(UUID tenantId, UUID claimId,
                                                    ClaimCommands.Litigation command) {
        return client.post()
                .uri(uri -> uri.path("/internal/v1/claims/{id}/litigation").build(claimId))
                .header("X-EIA-Tenant", tenantId.toString())
                .body(command)
                .retrieve()
                .body(ClaimDetailDtos.Litigation.class);
    }

    public ClaimDetailDtos.Litigation updateLitigation(UUID tenantId, UUID claimId, UUID litigationId,
                                                       ClaimCommands.Litigation command) {
        return client.patch()
                .uri(uri -> uri.path("/internal/v1/claims/{id}/litigation/{litigationId}")
                        .build(claimId, litigationId))
                .header("X-EIA-Tenant", tenantId.toString())
                .body(command)
                .retrieve()
                .body(ClaimDetailDtos.Litigation.class);
    }

    // ------------------------------------------------------------------ settlements
    public List<ClaimDetailDtos.Settlement> findSettlements(UUID tenantId, UUID claimId) {
        return client.get()
                .uri(uri -> uri.path("/internal/v1/claims/{id}/settlements")
                        .queryParam("tenantId", tenantId)
                        .build(claimId))
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
    }

    public ClaimDetailDtos.Settlement proposeSettlement(UUID tenantId, UUID claimId,
                                                        ClaimCommands.Settlement command) {
        return client.post()
                .uri(uri -> uri.path("/internal/v1/claims/{id}/settlements").build(claimId))
                .header("X-EIA-Tenant", tenantId.toString())
                .body(command)
                .retrieve()
                .body(ClaimDetailDtos.Settlement.class);
    }

    public ClaimDetailDtos.Settlement approveSettlement(UUID tenantId, UUID claimId, UUID settlementId,
                                                        ClaimCommands.SettlementApproval command) {
        return client.post()
                .uri(uri -> uri.path("/internal/v1/claims/{id}/settlements/{settlementId}/approve")
                        .build(claimId, settlementId))
                .header("X-EIA-Tenant", tenantId.toString())
                .body(command)
                .retrieve()
                .body(ClaimDetailDtos.Settlement.class);
    }

    public ClaimDetailDtos.Settlement executeSettlement(UUID tenantId, UUID claimId, UUID settlementId,
                                                        ClaimCommands.SettlementExecution command) {
        return client.post()
                .uri(uri -> uri.path("/internal/v1/claims/{id}/settlements/{settlementId}/execute")
                        .build(claimId, settlementId))
                .header("X-EIA-Tenant", tenantId.toString())
                .body(command)
                .retrieve()
                .body(ClaimDetailDtos.Settlement.class);
    }

    // ------------------------------------------------------------------ recoveries
    public List<ClaimDetailDtos.Recovery> findRecoveries(UUID tenantId, UUID claimId) {
        return client.get()
                .uri(uri -> uri.path("/internal/v1/claims/{id}/recoveries")
                        .queryParam("tenantId", tenantId)
                        .build(claimId))
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
    }

    public ClaimDetailDtos.Recovery recordRecovery(UUID tenantId, UUID claimId,
                                                   ClaimCommands.Recovery command) {
        return client.post()
                .uri(uri -> uri.path("/internal/v1/claims/{id}/recoveries").build(claimId))
                .header("X-EIA-Tenant", tenantId.toString())
                .body(command)
                .retrieve()
                .body(ClaimDetailDtos.Recovery.class);
    }

    public ClaimDetailDtos.Recovery updateRecovery(UUID tenantId, UUID claimId, UUID recoveryId,
                                                   ClaimCommands.Recovery command) {
        return client.patch()
                .uri(uri -> uri.path("/internal/v1/claims/{id}/recoveries/{recoveryId}")
                        .build(claimId, recoveryId))
                .header("X-EIA-Tenant", tenantId.toString())
                .body(command)
                .retrieve()
                .body(ClaimDetailDtos.Recovery.class);
    }

    // ------------------------------------------------------------------ liens
    public List<ClaimDetailDtos.Lien> findLiens(UUID tenantId, UUID claimId) {
        return client.get()
                .uri(uri -> uri.path("/internal/v1/claims/{id}/liens")
                        .queryParam("tenantId", tenantId)
                        .build(claimId))
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
    }

    public ClaimDetailDtos.Lien recordLien(UUID tenantId, UUID claimId, ClaimCommands.Lien command) {
        return client.post()
                .uri(uri -> uri.path("/internal/v1/claims/{id}/liens").build(claimId))
                .header("X-EIA-Tenant", tenantId.toString())
                .body(command)
                .retrieve()
                .body(ClaimDetailDtos.Lien.class);
    }

    public ClaimDetailDtos.Lien updateLien(UUID tenantId, UUID claimId, UUID lienId,
                                           ClaimCommands.Lien command) {
        return client.patch()
                .uri(uri -> uri.path("/internal/v1/claims/{id}/liens/{lienId}")
                        .build(claimId, lienId))
                .header("X-EIA-Tenant", tenantId.toString())
                .body(command)
                .retrieve()
                .body(ClaimDetailDtos.Lien.class);
    }

    // ------------------------------------------------------------------ tasks
    public List<ClaimDetailDtos.Task> findTasks(UUID tenantId, UUID claimId) {
        return client.get()
                .uri(uri -> uri.path("/internal/v1/claims/{id}/tasks").build(claimId))
                .header("X-EIA-Tenant", tenantId.toString())
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
    }

    public ClaimDetailDtos.Task addTask(UUID tenantId, UUID claimId, ClaimCommands.Task command) {
        return addTask(tenantId, claimId, command, null).value();
    }

    /**
     * Forwards the caller's Idempotency-Key to the expert and reads back the
     * {@code Idempotent-Replay} header so the domain can preserve it.
     */
    public ClaimCommandResult<ClaimDetailDtos.Task> addTask(UUID tenantId,
                                                            UUID claimId,
                                                            ClaimCommands.Task command,
                                                            String idempotencyKey) {
        return postForCommand("/internal/v1/claims/{id}/tasks", tenantId, claimId, command,
                ClaimDetailDtos.Task.class, idempotencyKey);
    }

    public ClaimDetailDtos.Task changeTaskStatus(UUID tenantId, UUID claimId, UUID taskId,
                                                 ClaimCommands.TaskStatusChange command) {
        return client.patch()
                .uri(uri -> uri.path("/internal/v1/claims/{id}/tasks/{taskId}")
                        .build(claimId, taskId))
                .header("X-EIA-Tenant", tenantId.toString())
                .body(command)
                .retrieve()
                .body(ClaimDetailDtos.Task.class);
    }

    // ------------------------------------------------------------------ notes
    public List<ClaimDetailDtos.Note> findNotes(UUID tenantId, UUID claimId) {
        return client.get()
                .uri(uri -> uri.path("/internal/v1/claims/{id}/notes").build(claimId))
                .header("X-EIA-Tenant", tenantId.toString())
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
    }

    public ClaimDetailDtos.Note addNote(UUID tenantId, UUID claimId, ClaimCommands.Note command) {
        return addNote(tenantId, claimId, command, null).value();
    }

    /**
     * Forwards the caller's Idempotency-Key to the expert and reads back the
     * {@code Idempotent-Replay} header so the domain can preserve it.
     */
    public ClaimCommandResult<ClaimDetailDtos.Note> addNote(UUID tenantId,
                                                            UUID claimId,
                                                            ClaimCommands.Note command,
                                                            String idempotencyKey) {
        return postForCommand("/internal/v1/claims/{id}/notes", tenantId, claimId, command,
                ClaimDetailDtos.Note.class, idempotencyKey);
    }

    private <T> ClaimCommandResult<T> postForCommand(String path,
                                                     UUID tenantId,
                                                     UUID claimId,
                                                     Object command,
                                                     Class<T> responseType,
                                                     String idempotencyKey) {
        ResponseEntity<T> response = client.post()
                .uri(uri -> uri.path(path).build(claimId))
                .header("X-EIA-Tenant", tenantId.toString())
                .headers(headers -> {
                    if (idempotencyKey != null && !idempotencyKey.isBlank()) {
                        headers.set("Idempotency-Key", idempotencyKey);
                    }
                })
                .body(command)
                .retrieve()
                .toEntity(responseType);
        return new ClaimCommandResult<>(response.getBody(),
                response.getHeaders().getFirst("Idempotent-Replay"));
    }

    record CreateClaimRequest(
            UUID tenantId,
            UUID policyId,
            UUID claimantPartyId,
            java.time.LocalDate lossDate,
            java.time.LocalDate reportDate,
            String causeCode,
            String claimType,
            java.math.BigDecimal reserveAmount,
            String reserveCurrency,
            java.math.BigDecimal settlementAuthorityAmount,
            String settlementAuthorityCurrency
    ) {
    }

    record StatusChangeRequest(String status, Integer expectedVersion) {
    }

    record CloseClaimRequest(Integer expectedVersion) {
    }

    record AssignmentRequest(String assignedAdjuster, String assignedTeam, Integer expectedVersion) {
    }
}
