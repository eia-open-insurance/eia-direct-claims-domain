package com.eia.directclaims.domain;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static com.eia.directclaims.domain.DomainFixtures.CLAIM_ID;
import static com.eia.directclaims.domain.DomainFixtures.COVERAGE_ID;
import static com.eia.directclaims.domain.DomainFixtures.EXPOSURE_ID;
import static com.eia.directclaims.domain.DomainFixtures.INJURY_ID;
import static com.eia.directclaims.domain.DomainFixtures.LIEN_ID;
import static com.eia.directclaims.domain.DomainFixtures.LITIGATION_ID;
import static com.eia.directclaims.domain.DomainFixtures.RECOVERY_ID;
import static com.eia.directclaims.domain.DomainFixtures.SETTLEMENT_ID;
import static com.eia.directclaims.domain.DomainFixtures.TENANT;
import static com.eia.directclaims.domain.DomainFixtures.coverage;
import static com.eia.directclaims.domain.DomainFixtures.exposure;
import static com.eia.directclaims.domain.DomainFixtures.injury;
import static com.eia.directclaims.domain.DomainFixtures.lien;
import static com.eia.directclaims.domain.DomainFixtures.litigation;
import static com.eia.directclaims.domain.DomainFixtures.recovery;
import static com.eia.directclaims.domain.DomainFixtures.reserve;
import static com.eia.directclaims.domain.DomainFixtures.settlement;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
}
