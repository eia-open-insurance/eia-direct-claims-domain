package com.eia.directclaims.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

/**
 * Client for the Policy domain public mass-tort profile endpoint
 * {@code GET /api/v1/policies/{policyId}/mass-tort-profile}.
 *
 * <p>The profile carries the coverage block that bounds the policy years a
 * long-tail claim may be allocated to. A policy without a profile is a 404,
 * and a profile the Policy service cannot answer for is treated the same way:
 * the caller allows the allocation, so the profile read never becomes an
 * availability dependency for claim allocation.</p>
 */
@Component
public class PolicyMassTortClient {
    static final String BUSINESS_BOOK = "DIRECT";
    static final String PROFILE_PATH = "/api/v1/policies/{policyId}/mass-tort-profile";

    private final RestClient client;

    public PolicyMassTortClient(RestClient.Builder builder,
                                @Value("${eia.policy.base-url:http://localhost:8084}") String baseUrl) {
        this.client = builder
                .baseUrl(baseUrl)
                .defaultHeader("X-EIA-Book", BUSINESS_BOOK)
                .build();
    }

    /**
     * Reads the policy's mass-tort profile. An absent profile (404), an
     * unavailable Policy service (5xx or transport failure), and any other
     * Policy rejection are returned as an empty result so the caller allows
     * the allocation.
     */
    public Optional<MassTortProfile> findProfile(UUID tenantId, UUID policyId) {
        try {
            MassTortProfile profile = client.get()
                    .uri(uri -> uri.path(PROFILE_PATH).build(policyId))
                    .header("X-EIA-Tenant", tenantId.toString())
                    .retrieve()
                    .body(MassTortProfile.class);
            return Optional.ofNullable(profile);
        } catch (RestClientResponseException ex) {
            // 404 means the policy has no profile; every other response status
            // leaves the allocation allowed as well.
            return Optional.empty();
        } catch (RestClientException | HttpMessageConversionException ex) {
            // A transport failure or an unreadable profile is unavailable, not
            // a reason to block the allocation.
            return Optional.empty();
        }
    }

    /** The validation-relevant projection of the Policy mass-tort profile. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MassTortProfile(
            String status,
            LocalDate coverageBlockStart,
            LocalDate coverageBlockEnd
    ) {
        /** A COMPLETE profile is authoritative once it has both block dates. */
        boolean isComplete() {
            return status != null && "COMPLETE".equalsIgnoreCase(status.trim());
        }

        boolean hasCoverageBlock() {
            return coverageBlockStart != null && coverageBlockEnd != null;
        }
    }
}
