package com.eia.directclaims.domain;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static com.eia.directclaims.domain.DomainFixtures.CLAIM_ID;
import static com.eia.directclaims.domain.DomainFixtures.COVERAGE_ID;
import static com.eia.directclaims.domain.DomainFixtures.EXPOSURE_ID;
import static com.eia.directclaims.domain.DomainFixtures.INJURY_ID;
import static com.eia.directclaims.domain.DomainFixtures.LIEN_ID;
import static com.eia.directclaims.domain.DomainFixtures.LITIGATION_ID;
import static com.eia.directclaims.domain.DomainFixtures.NOTE_ID;
import static com.eia.directclaims.domain.DomainFixtures.RECOVERY_ID;
import static com.eia.directclaims.domain.DomainFixtures.SETTLEMENT_ID;
import static com.eia.directclaims.domain.DomainFixtures.TASK_ID;
import static com.eia.directclaims.domain.DomainFixtures.TENANT;
import static com.eia.directclaims.domain.DomainFixtures.coverage;
import static com.eia.directclaims.domain.DomainFixtures.exposure;
import static com.eia.directclaims.domain.DomainFixtures.injury;
import static com.eia.directclaims.domain.DomainFixtures.lien;
import static com.eia.directclaims.domain.DomainFixtures.litigation;
import static com.eia.directclaims.domain.DomainFixtures.note;
import static com.eia.directclaims.domain.DomainFixtures.recovery;
import static com.eia.directclaims.domain.DomainFixtures.reserve;
import static com.eia.directclaims.domain.DomainFixtures.settlement;
import static com.eia.directclaims.domain.DomainFixtures.task;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ClaimDetailController.class)
class ClaimDetailControllerTest {
    private static final String BASE = "/api/v1/claims/" + CLAIM_ID;
    private static final String DIRECT = "DIRECT";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ClaimDetailService service;

    // ------------------------------------------------------------------ injuries
    @Test
    void listsInjuries() throws Exception {
        when(service.injuries(TENANT, CLAIM_ID)).thenReturn(List.of(injury()));

        mvc.perform(get(BASE + "/injuries").header("X-EIA-Tenant", TENANT).header("X-EIA-Book", DIRECT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].injuryType").value("MESOTHELIOMA"))
                .andExpect(jsonPath("$[0].diagnosisCodes[0]").value("C45.0"));
    }

    @Test
    void recordsAnInjury() throws Exception {
        when(service.addInjury(eq(TENANT), eq(CLAIM_ID), any(ClaimCommands.Injury.class)))
                .thenReturn(injury());

        mvc.perform(post(BASE + "/injuries")
                        .header("X-EIA-Tenant", TENANT)
                        .header("X-EIA-Book", DIRECT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "claimantPartyId": "33333333-3333-7333-8333-333333333333",
                                  "injuryType": "MESOTHELIOMA",
                                  "bodyPart": "LUNG",
                                  "diagnosisCodes": ["C45.0"],
                                  "severity": "SEVERE",
                                  "diagnosisDate": "2026-01-10"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.version").value(1));
    }

    @Test
    void updatesAnInjury() throws Exception {
        when(service.updateInjury(eq(TENANT), eq(CLAIM_ID), eq(INJURY_ID), any(ClaimCommands.Injury.class)))
                .thenReturn(injury());

        mvc.perform(patch(BASE + "/injuries/" + INJURY_ID)
                        .header("X-EIA-Tenant", TENANT)
                        .header("X-EIA-Book", DIRECT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"severity\": \"FATAL\", \"expectedVersion\": 1}"))
                .andExpect(status().isOk());
    }

    @Test
    void injuryRoutesKeepTheDirectBookWall() throws Exception {
        mvc.perform(get(BASE + "/injuries").header("X-EIA-Tenant", TENANT).header("X-EIA-Book", "ASSUMED"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
        verifyNoInteractions(service);
    }

    @Test
    void missingTenantIsAStructured400() throws Exception {
        mvc.perform(get(BASE + "/injuries").header("X-EIA-Book", DIRECT))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("X-EIA-Tenant header is required"));
        verifyNoInteractions(service);
    }

    // ------------------------------------------------------------------ exposures
    @Test
    void listsExposures() throws Exception {
        when(service.exposures(TENANT, CLAIM_ID)).thenReturn(List.of(exposure()));

        mvc.perform(get(BASE + "/exposures").header("X-EIA-Tenant", TENANT).header("X-EIA-Book", DIRECT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].jurisdiction").value("US-OH"));
    }

    @Test
    void recordsAnExposure() throws Exception {
        when(service.addExposure(eq(TENANT), eq(CLAIM_ID), any(ClaimCommands.Exposure.class)))
                .thenReturn(exposure());

        mvc.perform(post(BASE + "/exposures")
                        .header("X-EIA-Tenant", TENANT)
                        .header("X-EIA-Book", DIRECT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "claimantPartyId": "33333333-3333-7333-8333-333333333333",
                                  "exposureType": "SUBSTANCE",
                                  "substanceName": "Chrysotile asbestos",
                                  "jurisdiction": "US-OH",
                                  "exposureFrom": "2015-01-01",
                                  "exposureTo": "2020-12-31",
                                  "intensity": "HIGH"
                                }
                                """))
                .andExpect(status().isCreated());
    }

    @Test
    void updatesAnExposure() throws Exception {
        when(service.updateExposure(eq(TENANT), eq(CLAIM_ID), eq(EXPOSURE_ID),
                any(ClaimCommands.Exposure.class))).thenReturn(exposure());

        mvc.perform(patch(BASE + "/exposures/" + EXPOSURE_ID)
                        .header("X-EIA-Tenant", TENANT)
                        .header("X-EIA-Book", DIRECT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"intensity\": \"LOW\", \"expectedVersion\": 1}"))
                .andExpect(status().isOk());
    }

    // ------------------------------------------------------------------ reserves
    @Test
    void returnsReserveHistory() throws Exception {
        when(service.reserves(TENANT, CLAIM_ID)).thenReturn(new ClaimDetailDtos.ReserveSummary(
                CLAIM_ID, "USD", new BigDecimal("100000.0000"),
                List.of(new ClaimDetailDtos.ReserveComponent("INDEMNITY",
                        new BigDecimal("100000.0000"), "USD")),
                List.of(reserve())));

        mvc.perform(get(BASE + "/reserves").header("X-EIA-Tenant", TENANT).header("X-EIA-Book", DIRECT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCurrentAmount").isString())
                .andExpect(jsonPath("$.totalCurrentAmount").value("100000.0000"))
                .andExpect(jsonPath("$.history[0].current").value(true));
    }

    @Test
    void postsAReserveChange() throws Exception {
        when(service.changeReserve(eq(TENANT), eq(CLAIM_ID), any(ClaimCommands.ReserveChange.class)))
                .thenReturn(reserve());

        mvc.perform(post(BASE + "/reserves")
                        .header("X-EIA-Tenant", TENANT)
                        .header("X-EIA-Book", DIRECT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "component": "INDEMNITY",
                                  "amount": "100000.00",
                                  "currency": "USD",
                                  "effectiveDate": "2026-03-18",
                                  "reasonCode": "INITIAL",
                                  "reasonText": "Initial case reserve",
                                  "authority": "Manager",
                                  "expectedVersion": 0
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount").isString())
                .andExpect(jsonPath("$.amount").value("100000.0000"));
    }

    @Test
    void reserveRoutesKeepTheDirectBookWall() throws Exception {
        mvc.perform(post(BASE + "/reserves")
                        .header("X-EIA-Tenant", TENANT)
                        .header("X-EIA-Book", "ASSUMED")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotFound());
        verifyNoInteractions(service);
    }

    // ------------------------------------------------------------------ coverage
    @Test
    void returnsTheCoverageDecision() throws Exception {
        when(service.coverage(TENANT, CLAIM_ID)).thenReturn(coverage("COVERED"));

        mvc.perform(get(BASE + "/coverage").header("X-EIA-Tenant", TENANT).header("X-EIA-Book", DIRECT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.position").value("COVERED"))
                .andExpect(jsonPath("$.allocationPct").isString());
    }

    @Test
    void missingCoverageIsA404() throws Exception {
        when(service.coverage(TENANT, CLAIM_ID)).thenThrow(new ClaimNotFoundException(CLAIM_ID));

        mvc.perform(get(BASE + "/coverage").header("X-EIA-Tenant", TENANT).header("X-EIA-Book", DIRECT))
                .andExpect(status().isNotFound());
    }

    @Test
    void recordsACoverageDecision() throws Exception {
        when(service.decideCoverage(eq(TENANT), eq(CLAIM_ID), any(ClaimCommands.CoverageDecision.class)))
                .thenReturn(coverage("DEFENSE_ONLY"));

        mvc.perform(post(BASE + "/coverage")
                        .header("X-EIA-Tenant", TENANT)
                        .header("X-EIA-Book", DIRECT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "position": "DEFENSE_ONLY",
                                  "basis": "No indemnity trigger",
                                  "policyProvisions": "Coverage A",
                                  "allocationMethod": "PRO_RATA_TIME",
                                  "allocationPct": "0.25",
                                  "decisionDate": "2026-03-18",
                                  "reviewerPartyId": "44444444-4444-7444-8444-444444444444"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.position").value("DEFENSE_ONLY"));
    }

    // ------------------------------------------------------------------ litigation
    @Test
    void listsLitigation() throws Exception {
        when(service.litigation(TENANT, CLAIM_ID)).thenReturn(List.of(litigation()));

        mvc.perform(get(BASE + "/litigation").header("X-EIA-Tenant", TENANT).header("X-EIA-Book", DIRECT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].matterType").value("DEFENSE"));
    }

    @Test
    void recordsLitigation() throws Exception {
        when(service.addLitigation(eq(TENANT), eq(CLAIM_ID), any(ClaimCommands.Litigation.class)))
                .thenReturn(litigation());

        mvc.perform(post(BASE + "/litigation")
                        .header("X-EIA-Tenant", TENANT)
                        .header("X-EIA-Book", DIRECT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "matterType": "DEFENSE",
                                  "jurisdiction": "US-OH",
                                  "counselPartyId": "44444444-4444-7444-8444-444444444444",
                                  "filingDate": "2026-02-01",
                                  "trialDate": "2026-09-01",
                                  "status": "ACTIVE"
                                }
                                """))
                .andExpect(status().isCreated());
    }

    @Test
    void updatesLitigation() throws Exception {
        when(service.updateLitigation(eq(TENANT), eq(CLAIM_ID), eq(LITIGATION_ID),
                any(ClaimCommands.Litigation.class))).thenReturn(litigation());

        mvc.perform(patch(BASE + "/litigation/" + LITIGATION_ID)
                        .header("X-EIA-Tenant", TENANT)
                        .header("X-EIA-Book", DIRECT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"STAYED\", \"expectedVersion\": 1}"))
                .andExpect(status().isOk());
    }

    // ------------------------------------------------------------------ settlements
    @Test
    void listsSettlements() throws Exception {
        when(service.settlements(TENANT, CLAIM_ID)).thenReturn(List.of(settlement("PROPOSED")));

        mvc.perform(get(BASE + "/settlements").header("X-EIA-Tenant", TENANT).header("X-EIA-Book", DIRECT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].amount").isString());
    }

    @Test
    void proposesASettlement() throws Exception {
        when(service.proposeSettlement(eq(TENANT), eq(CLAIM_ID), any(ClaimCommands.Settlement.class)))
                .thenReturn(settlement("PROPOSED"));

        mvc.perform(post(BASE + "/settlements")
                        .header("X-EIA-Tenant", TENANT)
                        .header("X-EIA-Book", DIRECT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "amount": "60000.00",
                                  "currency": "USD",
                                  "structure": "LUMP_SUM",
                                  "releaseScope": "FULL",
                                  "paymentInstructions": "Wire to claimant trust account"
                                }
                                """))
                .andExpect(status().isCreated());
    }

    @Test
    void authorizesASettlement() throws Exception {
        when(service.approveSettlement(eq(TENANT), eq(CLAIM_ID), eq(SETTLEMENT_ID),
                any(ClaimCommands.SettlementApproval.class))).thenReturn(settlement("AUTHORIZED"));

        mvc.perform(post(BASE + "/settlements/" + SETTLEMENT_ID + "/approve")
                        .header("X-EIA-Tenant", TENANT)
                        .header("X-EIA-Book", DIRECT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "approvalAuthority": "Claims Committee",
                                  "authorizedBy": "44444444-4444-7444-8444-444444444444",
                                  "expectedVersion": 1
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AUTHORIZED"));
    }

    @Test
    void executesASettlement() throws Exception {
        when(service.executeSettlement(eq(TENANT), eq(CLAIM_ID), eq(SETTLEMENT_ID),
                any(ClaimCommands.SettlementExecution.class))).thenReturn(settlement("EXECUTED"));

        mvc.perform(post(BASE + "/settlements/" + SETTLEMENT_ID + "/execute")
                        .header("X-EIA-Tenant", TENANT)
                        .header("X-EIA-Book", DIRECT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "releaseDocumentId": "44444444-4444-7444-8444-444444444444",
                                  "paymentInstructions": "Wire funds",
                                  "expectedVersion": 2
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EXECUTED"));
    }

    // ------------------------------------------------------------------ recoveries
    @Test
    void listsRecoveries() throws Exception {
        when(service.recoveries(TENANT, CLAIM_ID)).thenReturn(List.of(recovery("RECEIVED")));

        mvc.perform(get(BASE + "/recoveries").header("X-EIA-Tenant", TENANT).header("X-EIA-Book", DIRECT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].recoveryType").value("SUBROGATION"));
    }

    @Test
    void recordsARecovery() throws Exception {
        when(service.recordRecovery(eq(TENANT), eq(CLAIM_ID), any(ClaimCommands.Recovery.class)))
                .thenReturn(recovery("RECEIVED"));

        mvc.perform(post(BASE + "/recoveries")
                        .header("X-EIA-Tenant", TENANT)
                        .header("X-EIA-Book", DIRECT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "recoveryType": "SUBROGATION",
                                  "amount": "20000.00",
                                  "currency": "USD",
                                  "source": "Acme",
                                  "status": "RECEIVED",
                                  "recoveryDate": "2026-05-01"
                                }
                                """))
                .andExpect(status().isCreated());
    }

    @Test
    void updatesARecovery() throws Exception {
        when(service.updateRecovery(eq(TENANT), eq(CLAIM_ID), eq(RECOVERY_ID),
                any(ClaimCommands.Recovery.class))).thenReturn(recovery("RECEIVED"));

        mvc.perform(patch(BASE + "/recoveries/" + RECOVERY_ID)
                        .header("X-EIA-Tenant", TENANT)
                        .header("X-EIA-Book", DIRECT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"RECEIVED\", \"recoveryDate\": \"2026-05-01\", \"expectedVersion\": 1}"))
                .andExpect(status().isOk());
    }

    // ------------------------------------------------------------------ liens
    @Test
    void listsLiens() throws Exception {
        when(service.liens(TENANT, CLAIM_ID)).thenReturn(List.of(lien("IDENTIFIED")));

        mvc.perform(get(BASE + "/liens").header("X-EIA-Tenant", TENANT).header("X-EIA-Book", DIRECT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].lienType").value("MEDICARE"));
    }

    @Test
    void recordsALien() throws Exception {
        when(service.recordLien(eq(TENANT), eq(CLAIM_ID), any(ClaimCommands.Lien.class)))
                .thenReturn(lien("IDENTIFIED"));

        mvc.perform(post(BASE + "/liens")
                        .header("X-EIA-Tenant", TENANT)
                        .header("X-EIA-Book", DIRECT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "lienType": "MEDICARE",
                                  "lienholder": "CMS",
                                  "amount": "15000.00",
                                  "currency": "USD",
                                  "status": "IDENTIFIED"
                                }
                                """))
                .andExpect(status().isCreated());
    }

    @Test
    void updatesALien() throws Exception {
        when(service.updateLien(eq(TENANT), eq(CLAIM_ID), eq(LIEN_ID), any(ClaimCommands.Lien.class)))
                .thenReturn(lien("RESOLVED"));

        mvc.perform(patch(BASE + "/liens/" + LIEN_ID)
                        .header("X-EIA-Tenant", TENANT)
                        .header("X-EIA-Book", DIRECT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"RESOLVED\", \"resolutionType\": \"NEGOTIATED\", \"expectedVersion\": 1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"));
    }

    // ------------------------------------------------------------------ tasks
    @Test
    void listsTasksWithTheExactPublicProjection() throws Exception {
        when(service.tasks(TENANT, CLAIM_ID)).thenReturn(List.of(task("OPEN")));

        mvc.perform(get(BASE + "/tasks")
                        .header("X-EIA-Tenant", TENANT)
                        .header("X-EIA-Book", DIRECT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(TASK_ID.toString()))
                .andExpect(jsonPath("$[0].claimId").value(CLAIM_ID.toString()))
                .andExpect(jsonPath("$[0].taskNumber").value("TSK-2026-0000001"))
                .andExpect(jsonPath("$[0].title").value("Obtain updated medical records"))
                .andExpect(jsonPath("$[0].description").value("Provider records requested at FNOL"))
                .andExpect(jsonPath("$[0].dueDate").value("2026-04-01"))
                .andExpect(jsonPath("$[0].priority").value("HIGH"))
                .andExpect(jsonPath("$[0].status").value("OPEN"))
                .andExpect(jsonPath("$[0].assignedTo").value("A. Adjuster"))
                .andExpect(jsonPath("$[0].version").value(1))
                .andExpect(jsonPath("$[0].createdAt").value("2026-03-18T09:00:00Z"))
                .andExpect(jsonPath("$[0].updatedAt").value("2026-03-18T09:00:00Z"))
                .andExpect(jsonPath("$[0].completedAt").isEmpty())
                .andExpect(jsonPath("$[0].tenantId").doesNotExist());
    }

    @Test
    void opensATask() throws Exception {
        when(service.addTask(eq(TENANT), eq(CLAIM_ID), any(ClaimCommands.Task.class), isNull()))
                .thenReturn(new ClaimCommandResult<>(task("OPEN"), null));

        mvc.perform(post(BASE + "/tasks")
                        .header("X-EIA-Tenant", TENANT)
                        .header("X-EIA-Book", DIRECT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Obtain updated medical records",
                                  "description": "Provider records requested at FNOL",
                                  "dueDate": "2026-04-01",
                                  "priority": "HIGH",
                                  "assignedTo": "A. Adjuster"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().doesNotExist("Idempotent-Replay"))
                .andExpect(jsonPath("$.id").value(TASK_ID.toString()))
                .andExpect(jsonPath("$.status").value("OPEN"));
    }

    @Test
    void opensATaskWithAnIdempotencyKeyAndPreservesTheReplayHeader() throws Exception {
        when(service.addTask(eq(TENANT), eq(CLAIM_ID), any(ClaimCommands.Task.class),
                eq("task-key-1"))).thenReturn(new ClaimCommandResult<>(task("OPEN"), "true"));

        mvc.perform(post(BASE + "/tasks")
                        .header("X-EIA-Tenant", TENANT)
                        .header("X-EIA-Book", DIRECT)
                        .header("Idempotency-Key", "task-key-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Obtain updated medical records",
                                  "dueDate": "2026-04-01",
                                  "priority": "HIGH"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Idempotent-Replay", "true"))
                .andExpect(jsonPath("$.id").value(TASK_ID.toString()));
    }

    @Test
    void changesATaskStatusWithTheObservedVersion() throws Exception {
        when(service.changeTaskStatus(eq(TENANT), eq(CLAIM_ID), eq(TASK_ID),
                any(ClaimCommands.TaskStatusChange.class))).thenReturn(task("COMPLETE"));

        mvc.perform(patch(BASE + "/tasks/" + TASK_ID)
                        .header("X-EIA-Tenant", TENANT)
                        .header("X-EIA-Book", DIRECT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"COMPLETE\", \"version\": 1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETE"))
                .andExpect(jsonPath("$.completedAt").value("2026-03-18T09:00:00Z"));
    }

    @Test
    void taskRoutesKeepTheDirectBookWall() throws Exception {
        mvc.perform(get(BASE + "/tasks")
                        .header("X-EIA-Tenant", TENANT)
                        .header("X-EIA-Book", "ASSUMED"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
        verifyNoInteractions(service);
    }

    // ------------------------------------------------------------------ notes
    @Test
    void listsNotesWithTheExactPublicProjection() throws Exception {
        when(service.notes(TENANT, CLAIM_ID)).thenReturn(List.of(note()));

        mvc.perform(get(BASE + "/notes")
                        .header("X-EIA-Tenant", TENANT)
                        .header("X-EIA-Book", DIRECT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(NOTE_ID.toString()))
                .andExpect(jsonPath("$[0].claimId").value(CLAIM_ID.toString()))
                .andExpect(jsonPath("$[0].noteNumber").value("NTE-2026-0000001"))
                .andExpect(jsonPath("$[0].noteType").value("DIARY"))
                .andExpect(jsonPath("$[0].body").value(
                        "Called the claimant to confirm the medical authorization"))
                .andExpect(jsonPath("$[0].author").value("Dana Handler"))
                .andExpect(jsonPath("$[0].createdAt").value("2026-03-18T09:00:00Z"))
                .andExpect(jsonPath("$[0].tenantId").doesNotExist());
    }

    @Test
    void addsANote() throws Exception {
        when(service.addNote(eq(TENANT), eq(CLAIM_ID), any(ClaimCommands.Note.class), isNull()))
                .thenReturn(new ClaimCommandResult<>(note(), null));

        mvc.perform(post(BASE + "/notes")
                        .header("X-EIA-Tenant", TENANT)
                        .header("X-EIA-Book", DIRECT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "noteType": "DIARY",
                                  "body": "Called the claimant to confirm the medical authorization",
                                  "author": "Dana Handler"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().doesNotExist("Idempotent-Replay"))
                .andExpect(jsonPath("$.noteNumber").value("NTE-2026-0000001"));
    }

    @Test
    void appendsANoteWithAnIdempotencyKeyAndPreservesTheReplayHeader() throws Exception {
        when(service.addNote(eq(TENANT), eq(CLAIM_ID), any(ClaimCommands.Note.class),
                eq("note-key-1"))).thenReturn(new ClaimCommandResult<>(note(), "false"));

        mvc.perform(post(BASE + "/notes")
                        .header("X-EIA-Tenant", TENANT)
                        .header("X-EIA-Book", DIRECT)
                        .header("Idempotency-Key", "note-key-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "noteType": "DIARY",
                                  "body": "Called the claimant to confirm the medical authorization",
                                  "author": "Dana Handler"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Idempotent-Replay", "false"));
    }

    @Test
    void noteRoutesKeepTheDirectBookWall() throws Exception {
        mvc.perform(post(BASE + "/notes")
                        .header("X-EIA-Tenant", TENANT)
                        .header("X-EIA-Book", "ASSUMED")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotFound());
        verifyNoInteractions(service);
    }

    // ------------------------------------------------------------------ expert problems
    @Test
    void expertConflictOnATaskIsPreservedAs409() throws Exception {
        when(service.changeTaskStatus(eq(TENANT), eq(CLAIM_ID), eq(TASK_ID),
                any(ClaimCommands.TaskStatusChange.class)))
                .thenThrow(expertProblem(409, "CONFLICT",
                        "Claim task version 1 does not match current version 2"));

        mvc.perform(patch(BASE + "/tasks/" + TASK_ID)
                        .header("X-EIA-Tenant", TENANT)
                        .header("X-EIA-Book", DIRECT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"COMPLETE\", \"version\": 1}"))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.code").value("CONFLICT"))
                .andExpect(jsonPath("$.message").value(
                        "Claim task version 1 does not match current version 2"));
    }

    @Test
    void expertNotFoundOnTasksIsPreservedAs404() throws Exception {
        when(service.tasks(TENANT, CLAIM_ID))
                .thenThrow(expertProblem(404, "NOT_FOUND", "Claim not found: " + CLAIM_ID));

        mvc.perform(get(BASE + "/tasks")
                        .header("X-EIA-Tenant", TENANT)
                        .header("X-EIA-Book", DIRECT))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Claim not found: " + CLAIM_ID));
    }

    @Test
    void expertBadRequestOnANoteIsPreservedAs400() throws Exception {
        when(service.addNote(eq(TENANT), eq(CLAIM_ID), any(ClaimCommands.Note.class), isNull()))
                .thenThrow(expertProblem(400, "VALIDATION_ERROR", "noteType is required"));

        mvc.perform(post(BASE + "/notes")
                        .header("X-EIA-Tenant", TENANT)
                        .header("X-EIA-Book", DIRECT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"noteType\": \"GENERAL\", \"body\": \"Body\", \"author\": \"Author\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("noteType is required"));
    }

    @Test
    void expertIdempotencyKeyReuseIsPreservedAs409() throws Exception {
        when(service.addNote(eq(TENANT), eq(CLAIM_ID), any(ClaimCommands.Note.class),
                eq("used-key"))).thenThrow(expertProblem(409, "EIA-DIR-4091",
                        "Idempotency-Key was already used with a different request."));

        mvc.perform(post(BASE + "/notes")
                        .header("X-EIA-Tenant", TENANT)
                        .header("X-EIA-Book", DIRECT)
                        .header("Idempotency-Key", "used-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"noteType\": \"DIARY\", \"body\": \"Body\", \"author\": \"Author\"}"))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("EIA-DIR-4091"))
                .andExpect(jsonPath("$.category").value("IDEMPOTENCY_KEY_REUSED"))
                .andExpect(jsonPath("$.message").value(
                        "Idempotency-Key was already used with a different request."));
    }

    private static RestClientResponseException expertProblem(int status, String code, String detail) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PROBLEM_JSON);
        String body = """
                {"type": "about:blank", "title": "Expert error", "status": %d,
                 "code": "%s", "detail": "%s"}
                """.formatted(status, code, detail);
        return new RestClientResponseException("expert error", HttpStatusCode.valueOf(status),
                "Expert error", headers, body.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
    }
}
