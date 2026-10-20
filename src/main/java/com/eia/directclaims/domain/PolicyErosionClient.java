package com.eia.directclaims.domain;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Client for the Policy domain public erosion endpoint
 * {@code POST /api/v1/policies/{policyId}/erosion}.
 *
 * <p>The allocation id is the idempotency key, so retrying the same allocation
 * can never double-count its erosion fact. The policy service owns the limit
 * check and the aggregate ledger; this client normalises both a rejection and
 * an unavailable service into a {@link PolicyErosionResult} so allocation
 * creation can record the outcome without failing the create.</p>
 */
@Component
public class PolicyErosionClient {
    static final String BUSINESS_BOOK = "DIRECT";
    static final long SOURCE_VERSION = 1L;
    static final int MAX_MESSAGE_LENGTH = 4000;
    static final String UNAVAILABLE_MESSAGE = "Policy erosion service is unavailable";

    private static final Logger log = LoggerFactory.getLogger(PolicyErosionClient.class);

    private final RestClient client;
    private final ObjectMapper objectMapper;

    public PolicyErosionClient(RestClient.Builder builder,
                               ObjectMapper objectMapper,
                               @Value("${eia.policy.base-url:http://localhost:8084}") String baseUrl) {
        this.client = builder
                .baseUrl(baseUrl)
                .defaultHeader("X-EIA-Book", BUSINESS_BOOK)
                .build();
        this.objectMapper = objectMapper;
    }

    /**
     * Posts one erosion fact for an allocation. A 2xx response is a successful
     * posting; a policy rejection or transport failure is returned as a failed
     * result carrying a short message for the allocation's erosion state.
     */
    public PolicyErosionResult postErosion(UUID tenantId,
                                           UUID policyId,
                                           UUID limitId,
                                           UUID claimId,
                                           UUID allocationId,
                                           BigDecimal reservedAmount,
                                           Instant asOf) {
        try {
            client.post()
                    .uri(uri -> uri.path("/api/v1/policies/{policyId}/erosion").build(policyId))
                    .header("X-EIA-Tenant", tenantId.toString())
                    .header("Idempotency-Key", allocationId.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new ErosionRequest(limitId, claimId, allocationId, SOURCE_VERSION,
                            BigDecimal.ZERO, reservedAmount, BigDecimal.ZERO, asOf))
                    .retrieve()
                    .toBodilessEntity();
            return PolicyErosionResult.success();
        } catch (RestClientResponseException ex) {
            String message = failureMessage(ex);
            log.warn("Policy erosion for allocation {} was rejected with {}: {}", allocationId,
                    ex.getStatusCode().value(), message);
            return PolicyErosionResult.failure(message);
        } catch (RestClientException ex) {
            log.warn("Policy erosion for allocation {} could not be posted: {}", allocationId,
                    ex.getMessage());
            return PolicyErosionResult.failure(UNAVAILABLE_MESSAGE);
        }
    }

    /** Prefers the policy problem's detail, then message, then a status fallback. */
    private String failureMessage(RestClientResponseException ex) {
        String body = ex.getResponseBodyAsString();
        String detail = problemField(body, "detail");
        if (detail == null) {
            detail = problemField(body, "message");
        }
        if (detail == null) {
            detail = "Policy erosion failed with status " + ex.getStatusCode().value();
        }
        return detail.length() > MAX_MESSAGE_LENGTH
                ? detail.substring(0, MAX_MESSAGE_LENGTH)
                : detail;
    }

    private String problemField(String body, String field) {
        if (body == null || body.isBlank()) {
            return null;
        }
        try {
            JsonNode problem = objectMapper.readTree(body);
            JsonNode value = problem.get(field);
            if (value != null && value.isTextual() && !value.asText().isBlank()) {
                return value.asText().trim();
            }
        } catch (Exception ignored) {
            // A non-JSON policy error keeps the generic status message.
        }
        return null;
    }

    /** The allocation erosion fact posted to the Policy domain. */
    record ErosionRequest(
            UUID limitId,
            UUID claimId,
            UUID allocationId,
            long sourceVersion,
            BigDecimal paidAmount,
            BigDecimal reservedAmount,
            BigDecimal recoveredAmount,
            Instant asOf
    ) {
    }

    /**
     * The outcome of one policy erosion attempt. {@code message} is null after
     * a successful posting and carries the policy rejection or transport
     * failure reason otherwise.
     */
    public record PolicyErosionResult(boolean posted, String message) {
        static PolicyErosionResult success() {
            return new PolicyErosionResult(true, null);
        }

        static PolicyErosionResult failure(String message) {
            return new PolicyErosionResult(false, message);
        }
    }
}
