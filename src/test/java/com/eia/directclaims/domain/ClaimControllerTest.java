package com.eia.directclaims.domain;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ClaimController.class)
class ClaimControllerTest {
    private static final UUID TENANT = UUID.fromString("11111111-1111-7111-8111-111111111111");
    private static final UUID CLAIM_ID = UUID.fromString("0192f3a4-2b7e-7d1f-a9c4-3e8b5f0c9d21");
    private static final UUID POLICY_ID = UUID.fromString("22222222-2222-7222-8222-222222222222");
    private static final UUID CLAIMANT_ID = UUID.fromString("33333333-3333-7333-8333-333333333333");
    private static final String URL = "/api/v1/claims";

    private static final String VALID_CREATE = """
            {
              "policyId": "22222222-2222-7222-8222-222222222222",
              "claimantPartyId": "33333333-3333-7333-8333-333333333333",
              "lossDate": "2026-03-14",
              "reportDate": "2026-03-18",
              "causeCode": "GL",
              "claimType": "MASS_TORT",
              "reserveAmount": 125000.00,
              "reserveCurrency": "USD",
              "settlementAuthorityAmount": 500000.00
            }
            """;

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ClaimService service;

    @Test
    void listsClaimsForTheDirectBook() throws Exception {
        when(service.list(TENANT, null, null)).thenReturn(List.of(claim("REGISTERED", 0)));

        mvc.perform(get(URL)
                        .header("X-EIA-Tenant", TENANT)
                        .header("X-EIA-Book", "DIRECT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].claimNumber").value("CLM-2026-0000001"))
                .andExpect(jsonPath("$[0].businessBook").value("DIRECT"))
                .andExpect(jsonPath("$[0].claimType").value("MASS_TORT"));
    }

    @Test
    void reserveAmountIsSerializedAsDecimalString() throws Exception {
        when(service.list(TENANT, null, null)).thenReturn(List.of(claim("REGISTERED", 0)));

        mvc.perform(get(URL)
                        .header("X-EIA-Tenant", TENANT)
                        .header("X-EIA-Book", "DIRECT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].reserveAmount").isString())
                .andExpect(jsonPath("$[0].reserveAmount").value("125000.0000"))
                .andExpect(jsonPath("$[0].currentReserveAmount").isString())
                .andExpect(jsonPath("$[0].settlementAuthorityAmount").isString());
    }

    @Test
    void filtersClaimsByClaimantParty() throws Exception {
        when(service.list(TENANT, null, CLAIMANT_ID)).thenReturn(List.of(claim("REGISTERED", 0)));

        mvc.perform(get(URL)
                        .header("X-EIA-Tenant", TENANT)
                        .header("X-EIA-Book", "DIRECT")
                        .param("claimantPartyId", CLAIMANT_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].claimNumber").value("CLM-2026-0000001"))
                .andExpect(jsonPath("$[0].claimantPartyId").value(CLAIMANT_ID.toString()));

        verify(service).list(TENANT, null, CLAIMANT_ID);
    }

    @Test
    void statusAndClaimantPartyFiltersAreCombined() throws Exception {
        when(service.list(TENANT, "OPEN", CLAIMANT_ID)).thenReturn(List.of(claim("OPEN", 1)));

        mvc.perform(get(URL)
                        .header("X-EIA-Tenant", TENANT)
                        .header("X-EIA-Book", "DIRECT")
                        .param("status", "OPEN")
                        .param("claimantPartyId", CLAIMANT_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("OPEN"));

        verify(service).list(TENANT, "OPEN", CLAIMANT_ID);
    }

    @Test
    void claimantPartyFilterStaysInsideTheBookWall() throws Exception {
        mvc.perform(get(URL)
                        .header("X-EIA-Tenant", TENANT)
                        .header("X-EIA-Book", "ASSUMED")
                        .param("claimantPartyId", CLAIMANT_ID.toString()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));

        verifyNoInteractions(service);
    }

    @Test
    void malformedClaimantPartyReturns400WithoutTouchingTheService() throws Exception {
        mvc.perform(get(URL)
                        .header("X-EIA-Tenant", TENANT)
                        .header("X-EIA-Book", "DIRECT")
                        .param("claimantPartyId", "not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_ARGUMENT"));

        verifyNoInteractions(service);
    }

    @Test
    void assumedBookReturns404AndNeverReachesTheService() throws Exception {
        mvc.perform(get(URL)
                        .header("X-EIA-Tenant", TENANT)
                        .header("X-EIA-Book", "ASSUMED"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Not found"));

        verifyNoInteractions(service);
    }

    @Test
    void missingBookReturns404() throws Exception {
        mvc.perform(get(URL).header("X-EIA-Tenant", TENANT))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));

        verifyNoInteractions(service);
    }

    @Test
    void lowercaseBookIsRejectedAs404() throws Exception {
        mvc.perform(get(URL)
                        .header("X-EIA-Tenant", TENANT)
                        .header("X-EIA-Book", "direct"))
                .andExpect(status().isNotFound());

        verifyNoInteractions(service);
    }

    @Test
    void missingTenantReturnsStructured400() throws Exception {
        mvc.perform(get(URL).header("X-EIA-Book", "DIRECT"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("X-EIA-Tenant header is required"));

        verifyNoInteractions(service);
    }

    @Test
    void malformedTenantReturnsStructured400() throws Exception {
        mvc.perform(get(URL)
                        .header("X-EIA-Tenant", "not-a-uuid")
                        .header("X-EIA-Book", "DIRECT"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void createsClaimForTheDirectBook() throws Exception {
        when(service.create(any(UUID.class), any(CreateClaimCommand.class))).thenReturn(claim("REGISTERED", 0));

        mvc.perform(post(URL)
                        .header("X-EIA-Tenant", TENANT)
                        .header("X-EIA-Book", "DIRECT")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_CREATE))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.claimNumber").value("CLM-2026-0000001"))
                .andExpect(jsonPath("$.status").value("REGISTERED"));
    }

    @Test
    void reportDateBeforeLossDateReturns400() throws Exception {
        when(service.create(any(UUID.class), any(CreateClaimCommand.class)))
                .thenThrow(new DomainValidationException("reportDate must be on or after lossDate"));

        mvc.perform(post(URL)
                        .header("X-EIA-Tenant", TENANT)
                        .header("X-EIA-Book", "DIRECT")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_CREATE.replace("2026-03-18", "2026-03-01")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("reportDate must be on or after lossDate"));
    }

    @Test
    void negativeReserveReturnsStructured400() throws Exception {
        mvc.perform(post(URL)
                        .header("X-EIA-Tenant", TENANT)
                        .header("X-EIA-Book", "DIRECT")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_CREATE.replace("125000.00", "-1.00")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details[0].field").value("reserveAmount"));

        verifyNoInteractions(service);
    }

    @Test
    void unknownClaimReturns404() throws Exception {
        when(service.get(TENANT, CLAIM_ID)).thenThrow(new ClaimNotFoundException(CLAIM_ID));

        mvc.perform(get(URL + "/" + CLAIM_ID)
                        .header("X-EIA-Tenant", TENANT)
                        .header("X-EIA-Book", "DIRECT"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.path").value(URL + "/" + CLAIM_ID));
    }

    @Test
    void statusTransitionUsesTheLifecycle() throws Exception {
        when(service.changeStatus(any(UUID.class), any(UUID.class), any(StatusChangeCommand.class)))
                .thenReturn(claim("OPEN", 1));

        mvc.perform(patch(URL + "/" + CLAIM_ID + "/status")
                        .header("X-EIA-Tenant", TENANT)
                        .header("X-EIA-Book", "DIRECT")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": "OPEN"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OPEN"));
    }

    @Test
    void illegalStatusTransitionReturns400() throws Exception {
        when(service.changeStatus(any(UUID.class), any(UUID.class), any(StatusChangeCommand.class)))
                .thenThrow(new DomainValidationException("Illegal claim status transition REGISTERED -> PAID"));

        mvc.perform(patch(URL + "/" + CLAIM_ID + "/status")
                        .header("X-EIA-Tenant", TENANT)
                        .header("X-EIA-Book", "DIRECT")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": "PAID"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void closesAClaimThroughTheTransactionRoute() throws Exception {
        when(service.close(TENANT, CLAIM_ID, 5)).thenReturn(claim("CLOSED", 6));

        mvc.perform(post(URL + "/" + CLAIM_ID + "/close")
                        .header("X-EIA-Tenant", TENANT)
                        .header("X-EIA-Book", "DIRECT")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expectedVersion\": 5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));
    }

    @Test
    void closeOnAnotherBookReturns404() throws Exception {
        mvc.perform(post(URL + "/" + CLAIM_ID + "/close")
                        .header("X-EIA-Tenant", TENANT)
                        .header("X-EIA-Book", "ASSUMED")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotFound());

        verifyNoInteractions(service);
    }

    // ------------------------------------------------------------------ assignment
    @Test
    void assignsAClaimWithThePayloadActorAndAssignedResponse() throws Exception {
        when(service.assign(eq(TENANT), eq(CLAIM_ID), any(ClaimCommands.Assignment.class),
                eq("supervisor@example.com"))).thenReturn(assignedClaim("REGISTERED", 4));

        mvc.perform(patch(URL + "/" + CLAIM_ID + "/assignment")
                        .header("X-EIA-Tenant", TENANT)
                        .header("X-EIA-Book", "DIRECT")
                        .header("X-EIA-Actor", "supervisor@example.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "assignedAdjuster": "adjuster@example.com",
                                  "assignedTeam": "CASUALTY",
                                  "expectedVersion": 3
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignedAdjuster").value("adjuster@example.com"))
                .andExpect(jsonPath("$.assignedTeam").value("CASUALTY"))
                .andExpect(jsonPath("$.assignedAt").value("2026-03-18T09:00:00Z"))
                .andExpect(jsonPath("$.version").value(4));

        ArgumentCaptor<ClaimCommands.Assignment> captured =
                ArgumentCaptor.forClass(ClaimCommands.Assignment.class);
        verify(service).assign(eq(TENANT), eq(CLAIM_ID), captured.capture(),
                eq("supervisor@example.com"));
        assertEquals("adjuster@example.com", captured.getValue().assignedAdjuster());
        assertEquals("CASUALTY", captured.getValue().assignedTeam());
        assertEquals(3, captured.getValue().expectedVersion());
    }

    @Test
    void assignmentWithoutAnActorForwardsNull() throws Exception {
        when(service.assign(eq(TENANT), eq(CLAIM_ID), any(ClaimCommands.Assignment.class), isNull()))
                .thenReturn(assignedClaim("REGISTERED", 4));

        mvc.perform(patch(URL + "/" + CLAIM_ID + "/assignment")
                        .header("X-EIA-Tenant", TENANT)
                        .header("X-EIA-Book", "DIRECT")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"assignedAdjuster": "adjuster@example.com", "expectedVersion": 3}
                                """))
                .andExpect(status().isOk());

        verify(service).assign(eq(TENANT), eq(CLAIM_ID), any(ClaimCommands.Assignment.class),
                isNull());
    }

    @Test
    void blankAssignedAdjusterReturnsStructured400() throws Exception {
        mvc.perform(patch(URL + "/" + CLAIM_ID + "/assignment")
                        .header("X-EIA-Tenant", TENANT)
                        .header("X-EIA-Book", "DIRECT")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"assignedAdjuster": "  ", "assignedTeam": "CASUALTY", "expectedVersion": 3}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details[0].field").value("assignedAdjuster"));

        verifyNoInteractions(service);
    }

    @Test
    void missingExpectedVersionReturnsStructured400() throws Exception {
        mvc.perform(patch(URL + "/" + CLAIM_ID + "/assignment")
                        .header("X-EIA-Tenant", TENANT)
                        .header("X-EIA-Book", "DIRECT")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"assignedAdjuster": "adjuster@example.com"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details[0].field").value("expectedVersion"));

        verifyNoInteractions(service);
    }

    @Test
    void overlongAssignedTeamReturnsStructured400() throws Exception {
        String body = "{\"assignedAdjuster\":\"adjuster@example.com\",\"assignedTeam\":\""
                + "t".repeat(65) + "\",\"expectedVersion\":3}";

        mvc.perform(patch(URL + "/" + CLAIM_ID + "/assignment")
                        .header("X-EIA-Tenant", TENANT)
                        .header("X-EIA-Book", "DIRECT")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0].field").value("assignedTeam"));

        verifyNoInteractions(service);
    }

    @Test
    void assignmentOnAnotherBookReturns404WithoutCallingTheService() throws Exception {
        mvc.perform(patch(URL + "/" + CLAIM_ID + "/assignment")
                        .header("X-EIA-Tenant", TENANT)
                        .header("X-EIA-Book", "ASSUMED")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"assignedAdjuster": "adjuster@example.com", "expectedVersion": 3}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));

        verifyNoInteractions(service);
    }

    @Test
    void staleAssignmentVersionIsPreservedAs409() throws Exception {
        when(service.assign(eq(TENANT), eq(CLAIM_ID), any(ClaimCommands.Assignment.class), any()))
                .thenThrow(expertProblem(409, "CONFLICT",
                        "Claim version 2 does not match current version 3"));

        mvc.perform(patch(URL + "/" + CLAIM_ID + "/assignment")
                        .header("X-EIA-Tenant", TENANT)
                        .header("X-EIA-Book", "DIRECT")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"assignedAdjuster": "adjuster@example.com", "expectedVersion": 2}
                                """))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.code").value("CONFLICT"))
                .andExpect(jsonPath("$.message").value(
                        "Claim version 2 does not match current version 3"));
    }

    @Test
    void unknownClaimOnAssignmentIsPreservedAs404() throws Exception {
        when(service.assign(eq(TENANT), eq(CLAIM_ID), any(ClaimCommands.Assignment.class), any()))
                .thenThrow(expertProblem(404, "NOT_FOUND", "Claim not found: " + CLAIM_ID));

        mvc.perform(patch(URL + "/" + CLAIM_ID + "/assignment")
                        .header("X-EIA-Tenant", TENANT)
                        .header("X-EIA-Book", "DIRECT")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"assignedAdjuster": "adjuster@example.com", "expectedVersion": 3}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Claim not found: " + CLAIM_ID));
    }

    @Test
    void expertValidationProblemOnAssignmentIsPreservedAs400() throws Exception {
        when(service.assign(eq(TENANT), eq(CLAIM_ID), any(ClaimCommands.Assignment.class), any()))
                .thenThrow(expertProblem(400, "VALIDATION_ERROR",
                        "assignedAdjuster must be at most 128 characters"));

        mvc.perform(patch(URL + "/" + CLAIM_ID + "/assignment")
                        .header("X-EIA-Tenant", TENANT)
                        .header("X-EIA-Book", "DIRECT")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"assignedAdjuster": "adjuster@example.com", "expectedVersion": 3}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value(
                        "assignedAdjuster must be at most 128 characters"));
    }

    private static RestClientResponseException expertProblem(int status, String code, String detail) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PROBLEM_JSON);
        String body = """
                {"type": "about:blank", "title": "Expert error", "status": %d,
                 "code": "%s", "detail": "%s"}
                """.formatted(status, code, detail);
        return new RestClientResponseException("expert error", HttpStatusCode.valueOf(status),
                "Expert error", headers, body.getBytes(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8);
    }

    private static ClaimDto assignedClaim(String status, int version) {
        return claim(status, version, "adjuster@example.com", "CASUALTY",
                Instant.parse("2026-03-18T09:00:00Z"));
    }

    private static ClaimDto claim(String status, int version) {
        return claim(status, version, null, null, null);
    }

    private static ClaimDto claim(String status, int version, String assignedAdjuster,
                                  String assignedTeam, Instant assignedAt) {
        return new ClaimDto(
                CLAIM_ID,
                TENANT,
                "DIRECT",
                "CLM-2026-0000001",
                POLICY_ID,
                CLAIMANT_ID,
                "MASS_TORT",
                status,
                LocalDate.of(2026, 3, 14),
                LocalDate.of(2026, 3, 18),
                "GL",
                new BigDecimal("125000.0000"),
                "USD",
                new BigDecimal("500000.0000"),
                "USD",
                false,
                assignedAdjuster,
                assignedTeam,
                assignedAt,
                new BigDecimal("125000.0000"),
                version,
                Instant.parse("2026-03-18T09:00:00Z"),
                Instant.parse("2026-03-18T09:00:00Z"));
    }
}
