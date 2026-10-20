package com.eia.directclaims.domain;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ClaimController.class)
class ClaimSearchControllerTest {
    private static final UUID TENANT = UUID.fromString("11111111-1111-7111-8111-111111111111");
    private static final UUID CLAIM_ID = UUID.fromString("0192f3a4-2b7e-7d1f-a9c4-3e8b5f0c9d21");
    private static final String URL = "/api/v1/claims/search";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ClaimService service;

    @Test
    void returnsTheSearchEnvelopeForTheDirectBook() throws Exception {
        when(service.search(TENANT, "CLM", 10)).thenReturn(response("CLM", 10));

        mvc.perform(get(URL)
                        .queryParam("q", "CLM")
                        .header("X-EIA-Tenant", TENANT)
                        .header("X-EIA-Book", "DIRECT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.query").value("CLM"))
                .andExpect(jsonPath("$.limit").value(10))
                .andExpect(jsonPath("$.items[0].id").value(CLAIM_ID.toString()))
                .andExpect(jsonPath("$.items[0].type").value("DIRECT_CLAIM"))
                .andExpect(jsonPath("$.items[0].label").value("CLM-2026-0000001"))
                .andExpect(jsonPath("$.items[0].secondary").value("BODILY_INJURY · REGISTERED"))
                .andExpect(jsonPath("$.items[0].status").value("REGISTERED"))
                .andExpect(jsonPath("$.items[0].href").value("/direct-claims?record=" + CLAIM_ID));

        verify(service).search(TENANT, "CLM", 10);
    }

    @Test
    void passesTheRequestedLimitThrough() throws Exception {
        when(service.search(TENANT, "CLM", 2)).thenReturn(response("CLM", 2));

        mvc.perform(get(URL)
                        .queryParam("q", "CLM")
                        .queryParam("limit", "2")
                        .header("X-EIA-Tenant", TENANT)
                        .header("X-EIA-Book", "DIRECT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.limit").value(2));

        verify(service).search(TENANT, "CLM", 2);
    }

    @Test
    void missingQueryIsTreatedAsBlank() throws Exception {
        when(service.search(TENANT, "", 10)).thenReturn(response("", 10));

        mvc.perform(get(URL)
                        .header("X-EIA-Tenant", TENANT)
                        .header("X-EIA-Book", "DIRECT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.query").value(""));

        verify(service).search(TENANT, "", 10);
    }

    @Test
    void assumedBookReturns404AndNeverReachesTheService() throws Exception {
        mvc.perform(get(URL)
                        .queryParam("q", "CLM")
                        .header("X-EIA-Tenant", TENANT)
                        .header("X-EIA-Book", "ASSUMED"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));

        verifyNoInteractions(service);
    }

    @Test
    void missingBookReturns404AndNeverReachesTheService() throws Exception {
        mvc.perform(get(URL)
                        .queryParam("q", "CLM")
                        .header("X-EIA-Tenant", TENANT))
                .andExpect(status().isNotFound());

        verifyNoInteractions(service);
    }

    @Test
    void missingTenantReturnsStructured400() throws Exception {
        mvc.perform(get(URL)
                        .queryParam("q", "CLM")
                        .header("X-EIA-Book", "DIRECT"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("X-EIA-Tenant header is required"));

        verifyNoInteractions(service);
    }

    private static ClaimSearchResponse response(String query, int limit) {
        return new ClaimSearchResponse(query, limit, List.of(new ClaimSearchResponse.Item(
                CLAIM_ID,
                "DIRECT_CLAIM",
                "CLM-2026-0000001",
                "BODILY_INJURY · REGISTERED",
                "REGISTERED",
                "/direct-claims?record=" + CLAIM_ID
        )));
    }
}
