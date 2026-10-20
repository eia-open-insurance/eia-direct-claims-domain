package com.eia.directclaims.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.net.SocketTimeoutException;
import java.time.LocalDate;
import java.util.Optional;

import static com.eia.directclaims.domain.DomainFixtures.POLICY_ID;
import static com.eia.directclaims.domain.DomainFixtures.TENANT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

/**
 * Proves the exact Policy mass-tort profile call: the public path, the DIRECT
 * book and tenant headers, the parsed coverage block, and the fail-open mapping
 * for an absent profile and an unavailable Policy service.
 */
class PolicyMassTortClientTest {
    private static final String POLICY_BASE = "http://policy.test";
    private static final String PROFILE_URL =
            POLICY_BASE + "/api/v1/policies/" + POLICY_ID + "/mass-tort-profile";

    private MockRestServiceServer server;
    private PolicyMassTortClient client;

    @BeforeEach
    void setUp() {
        ObjectMapper mapper = Jackson2ObjectMapperBuilder.json()
                .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .build();
        RestClient.Builder builder = RestClient.builder()
                .messageConverters(converters -> {
                    converters.removeIf(MappingJackson2HttpMessageConverter.class::isInstance);
                    converters.add(0, new MappingJackson2HttpMessageConverter(mapper));
                });
        server = MockRestServiceServer.bindTo(builder).build();
        client = new PolicyMassTortClient(builder, POLICY_BASE);
    }

    @Test
    void readsTheProfileWithTenantAndDirectBook() {
        server.expect(requestTo(PROFILE_URL))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-EIA-Tenant", TENANT.toString()))
                .andExpect(header("X-EIA-Book", "DIRECT"))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                                {
                                  "profileId": "55555555-5555-7555-8555-555555555555",
                                  "policyId": "22222222-2222-7222-8222-222222222222",
                                  "tortType": "ASBESTOS",
                                  "triggerType": "EXPOSURE",
                                  "allocationMethod": "PRO_RATA_TIME",
                                  "allocationNotes": "Allocate across the block",
                                  "coverageBlockStart": "2024-01-01",
                                  "coverageBlockEnd": "2028-12-31",
                                  "status": "COMPLETE",
                                  "updatedAt": "2026-03-18T09:00:00Z"
                                }
                                """));

        Optional<PolicyMassTortClient.MassTortProfile> profile =
                client.findProfile(TENANT, POLICY_ID);

        assertTrue(profile.isPresent());
        assertEquals("COMPLETE", profile.get().status());
        assertEquals(LocalDate.of(2024, 1, 1), profile.get().coverageBlockStart());
        assertEquals(LocalDate.of(2028, 12, 31), profile.get().coverageBlockEnd());
        server.verify();
    }

    @Test
    void aPolicyWithoutAProfileIsEmpty() {
        server.expect(requestTo(PROFILE_URL))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.NOT_FOUND)
                        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                        .body("""
                                {
                                  "type": "about:blank",
                                  "title": "Not found",
                                  "status": 404,
                                  "code": "MASS_TORT_PROFILE_NOT_FOUND",
                                  "detail": "Policy has no mass tort profile"
                                }
                                """));

        assertTrue(client.findProfile(TENANT, POLICY_ID).isEmpty());
        server.verify();
    }

    @Test
    void anUnavailablePolicyIsTreatedAsNoProfile() {
        server.expect(requestTo(PROFILE_URL))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE)
                        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                        .body("{\"title\": \"Unavailable\", \"status\": 503}"));

        assertTrue(client.findProfile(TENANT, POLICY_ID).isEmpty());
        server.verify();
    }

    @Test
    void aTransportFailureIsTreatedAsNoProfile() {
        server.expect(requestTo(PROFILE_URL))
                .andExpect(method(HttpMethod.GET))
                .andRespond(request -> {
                    throw new SocketTimeoutException("policy timed out");
                });

        assertTrue(client.findProfile(TENANT, POLICY_ID).isEmpty());
        server.verify();
    }

    @Test
    void aDraftProfileMayOmitTheCoverageBlock() {
        server.expect(requestTo(PROFILE_URL))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                                {
                                  "tortType": "ASBESTOS",
                                  "triggerType": "EXPOSURE",
                                  "allocationMethod": "PRO_RATA_TIME",
                                  "allocationNotes": null,
                                  "coverageBlockStart": null,
                                  "coverageBlockEnd": null,
                                  "status": "DRAFT",
                                  "updatedAt": "2026-03-18T09:00:00Z"
                                }
                                """));

        Optional<PolicyMassTortClient.MassTortProfile> profile =
                client.findProfile(TENANT, POLICY_ID);

        assertTrue(profile.isPresent());
        assertEquals("DRAFT", profile.get().status());
        assertNull(profile.get().coverageBlockStart());
        assertNull(profile.get().coverageBlockEnd());
        server.verify();
    }
}
