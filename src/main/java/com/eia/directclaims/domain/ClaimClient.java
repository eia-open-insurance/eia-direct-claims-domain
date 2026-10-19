package com.eia.directclaims.domain;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
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
                        command.reserveAmount(), command.reserveCurrency()))
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

    record CreateClaimRequest(
            UUID tenantId,
            UUID policyId,
            UUID claimantPartyId,
            java.time.LocalDate lossDate,
            java.time.LocalDate reportDate,
            String causeCode,
            java.math.BigDecimal reserveAmount,
            String reserveCurrency
    ) {
    }

    record StatusChangeRequest(String status, Integer expectedVersion) {
    }
}
