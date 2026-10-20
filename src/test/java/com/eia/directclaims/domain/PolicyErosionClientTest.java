package com.eia.directclaims.domain;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Instant;

import static com.eia.directclaims.domain.DomainFixtures.ALLOCATION_ID;
import static com.eia.directclaims.domain.DomainFixtures.CLAIM_ID;
import static com.eia.directclaims.domain.DomainFixtures.LIMIT_ID;
import static com.eia.directclaims.domain.DomainFixtures.POLICY_ID;
import static com.eia.directclaims.domain.DomainFixtures.TENANT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

/**
 * Proves the exact Policy erosion call: the public path, the DIRECT book and
 * tenant headers, the allocation id as the idempotency key, and the full
 * {@code reservedAmount}-based fact body.
 */
class PolicyErosionClientTest {
    private static final String POLICY_BASE = "http://policy.test";
    private static final String EROSION_URL =
            POLICY_BASE + "/api/v1/policies/" + POLICY_ID + "/erosion";
    private static final ObjectMapper BODY_READER = new ObjectMapper();
    private static final BigDecimal ALLOCATED_AMOUNT = new BigDecimal("31250.0000");
    private static final Instant AS_OF = Instant.parse("2026-03-18T09:00:00Z");

    private MockRestServiceServer server;
    private PolicyErosionClient client;

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
        client = new PolicyErosionClient(builder, mapper, POLICY_BASE);
    }

    @Test
    void postsTheErosionFactWithTenantDirectBookAndAllocationIdempotency() {
        server.expect(requestTo(EROSION_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-EIA-Tenant", TENANT.toString()))
                .andExpect(header("X-EIA-Book", "DIRECT"))
                .andExpect(header("Idempotency-Key", ALLOCATION_ID.toString()))
                .andExpect(request -> {
                    String raw = ((MockClientHttpRequest) request).getBodyAsString();
                    JsonNode body = BODY_READER.readTree(raw);
                    assertEquals(LIMIT_ID.toString(), body.path("limitId").asText());
                    assertEquals(CLAIM_ID.toString(), body.path("claimId").asText());
                    assertEquals(ALLOCATION_ID.toString(), body.path("allocationId").asText());
                    assertEquals(1, body.path("sourceVersion").asInt());
                    assertEquals(0, body.path("paidAmount").decimalValue()
                            .compareTo(BigDecimal.ZERO));
                    assertEquals(0, body.path("reservedAmount").decimalValue()
                            .compareTo(ALLOCATED_AMOUNT));
                    assertEquals(0, body.path("recoveredAmount").decimalValue()
                            .compareTo(BigDecimal.ZERO));
                    assertEquals(AS_OF.toString(), body.path("asOf").asText());
                })
                .andRespond(withStatus(HttpStatus.CREATED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{}"));

        PolicyErosionClient.PolicyErosionResult result = postErosion();

        assertTrue(result.posted());
        assertNull(result.message());
        server.verify();
    }

    @Test
    void aPolicyRejectionBecomesAFailedResultWithTheProblemDetail() {
        server.expect(requestTo(EROSION_URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                        .body("""
                                {
                                  "title": "Limit exceeded",
                                  "status": 400,
                                  "detail": "The erosion fact would exceed the limit.",
                                  "code": "LIMIT_EXCEEDED"
                                }
                                """));

        PolicyErosionClient.PolicyErosionResult result = postErosion();

        assertFalse(result.posted());
        assertEquals("The erosion fact would exceed the limit.", result.message());
        server.verify();
    }

    @Test
    void aPolicyProblemWithoutADetailFallsBackToItsMessage() {
        server.expect(requestTo(EROSION_URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.NOT_FOUND)
                        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                        .body("""
                                {
                                  "title": "Not found",
                                  "status": 404,
                                  "message": "Policy limit not found",
                                  "code": "NOT_FOUND"
                                }
                                """));

        PolicyErosionClient.PolicyErosionResult result = postErosion();

        assertFalse(result.posted());
        assertEquals("Policy limit not found", result.message());
        server.verify();
    }

    @Test
    void aPolicyProblemWithoutAMessageUsesTheStatusFallback() {
        server.expect(requestTo(EROSION_URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE)
                        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                        .body("{\"title\": \"Unavailable\", \"status\": 503}"));

        PolicyErosionClient.PolicyErosionResult result = postErosion();

        assertFalse(result.posted());
        assertEquals("Policy erosion failed with status 503", result.message());
        server.verify();
    }

    private PolicyErosionClient.PolicyErosionResult postErosion() {
        return client.postErosion(TENANT, POLICY_ID, LIMIT_ID, CLAIM_ID, ALLOCATION_ID,
                ALLOCATED_AMOUNT, AS_OF);
    }
}
