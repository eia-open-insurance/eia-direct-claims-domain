package com.eia.directclaims.domain;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class ClaimService {
    private final ClaimClient client;

    public ClaimService(ClaimClient client) {
        this.client = client;
    }

    public List<ClaimDto> list(UUID tenantId, String statusFilter) {
        ClaimStatus status = statusFilter == null || statusFilter.isBlank()
                ? null
                : ClaimStatus.from(statusFilter);
        return client.findAll(tenantId, status);
    }

    public ClaimDto get(UUID tenantId, UUID id) {
        ClaimDto claim = client.findById(tenantId, id);
        if (claim == null) {
            throw new ClaimNotFoundException(id);
        }
        return claim;
    }

    public ClaimDto create(UUID tenantId, CreateClaimCommand command) {
        validate(command);
        return client.create(tenantId, command);
    }

    public ClaimDto changeStatus(UUID tenantId, UUID id, StatusChangeCommand command) {
        ClaimDto current = get(tenantId, id);
        ClaimStatus currentStatus = ClaimStatus.from(current.status());
        ClaimStatus next = ClaimStatus.from(command.status());

        if (currentStatus == next) {
            throw new DomainValidationException("Claim is already in status " + next);
        }
        if (!currentStatus.canTransitionTo(next)) {
            throw new DomainValidationException("Illegal claim status transition "
                    + currentStatus + " -> " + next);
        }
        return client.changeStatus(tenantId, id, next, current.version());
    }

    /**
     * Closure is a transaction in SPEC §11.4.10. The expert enforces the
     * closure criteria (unresolved lien, open reserve, legal transition).
     */
    public ClaimDto close(UUID tenantId, UUID id, Integer expectedVersion) {
        get(tenantId, id);
        return client.close(tenantId, id, expectedVersion);
    }

    private static void validate(CreateClaimCommand command) {
        if (command.lossDate() == null || command.reportDate() == null) {
            throw new DomainValidationException("lossDate and reportDate are required");
        }
        if (command.reportDate().isBefore(command.lossDate())) {
            throw new DomainValidationException("reportDate must be on or after lossDate");
        }
        BigDecimal reserve = command.reserveAmount();
        if (reserve != null && reserve.signum() < 0) {
            throw new DomainValidationException("reserveAmount must be greater than or equal to 0");
        }
    }
}
