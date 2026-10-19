package com.eia.directclaims.domain;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
              "reserveAmount": 125000.00,
              "reserveCurrency": "USD"
            }
            """;

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ClaimService service;

    @Test
    void listsClaimsForTheDirectBook() throws Exception {
        when(service.list(TENANT, null)).thenReturn(List.of(claim("REGISTERED", 0)));

        mvc.perform(get(URL)
                        .header("X-EIA-Tenant", TENANT)
                        .header("X-EIA-Book", "DIRECT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].claimNumber").value("CLM-2026-0000001"))
                .andExpect(jsonPath("$[0].businessBook").value("DIRECT"));
    }

    @Test
    void reserveAmountIsSerializedAsDecimalString() throws Exception {
        when(service.list(TENANT, null)).thenReturn(List.of(claim("REGISTERED", 0)));

        mvc.perform(get(URL)
                        .header("X-EIA-Tenant", TENANT)
                        .header("X-EIA-Book", "DIRECT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].reserveAmount").isString())
                .andExpect(jsonPath("$[0].reserveAmount").value("125000.0000"));
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

    private static ClaimDto claim(String status, int version) {
        return new ClaimDto(
                CLAIM_ID,
                TENANT,
                "DIRECT",
                "CLM-2026-0000001",
                POLICY_ID,
                CLAIMANT_ID,
                status,
                LocalDate.of(2026, 3, 14),
                LocalDate.of(2026, 3, 18),
                "GL",
                new BigDecimal("125000.0000"),
                "USD",
                version,
                Instant.parse("2026-03-18T09:00:00Z"),
                Instant.parse("2026-03-18T09:00:00Z"));
    }
}
