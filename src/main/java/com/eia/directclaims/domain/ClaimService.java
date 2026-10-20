package com.eia.directclaims.domain;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
public class ClaimService {
    public static final int DEFAULT_SEARCH_LIMIT = 10;
    public static final int MAX_SEARCH_LIMIT = 25;

    private static final String BOOK = "DIRECT";
    private static final String SEARCH_TYPE = "DIRECT_CLAIM";

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

    /**
     * Common search over the claims the direct-claims expert exposes for a tenant.
     * The domain owns no tables, so the projection is built from the existing
     * tenant-scoped list and still applies the book wall defensively.
     */
    public ClaimSearchResponse search(UUID tenantId, String query, int limit) {
        String term = query == null ? "" : query.trim();
        int effectiveLimit = normalizeLimit(limit);
        String needle = term.toLowerCase(Locale.ROOT);

        List<ClaimSearchResponse.Item> items = Optional.ofNullable(client.findAll(tenantId, null))
                .orElseGet(List::of)
                .stream()
                .filter(claim -> tenantId.equals(claim.tenantId()))
                .filter(claim -> BOOK.equals(claim.businessBook()))
                .filter(claim -> needle.isEmpty() || matches(needle,
                        claim.claimNumber(), claim.claimType(), claim.causeCode(), claim.policyId()))
                .sorted(Comparator.comparing(ClaimDto::updatedAt,
                                Comparator.nullsLast(Comparator.<Instant>reverseOrder()))
                        .thenComparing(ClaimDto::id))
                .limit(effectiveLimit)
                .map(ClaimService::searchItem)
                .toList();

        return new ClaimSearchResponse(term, effectiveLimit, items);
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
     * Assigns or reassigns the claim at the version the caller observed. The
     * expert owns the optimistic concurrency check, so a stale version surfaces
     * as the expert's 409 problem instead of overwriting a concurrent change.
     */
    public ClaimDto assign(UUID tenantId, UUID claimId, ClaimCommands.Assignment command, String actor) {
        return client.assign(tenantId, claimId, command, actor);
    }

    /**
     * Closure is a transaction in SPEC §11.4.10. The expert enforces the
     * closure criteria (unresolved lien, open reserve, legal transition).
     */
    public ClaimDto close(UUID tenantId, UUID id, Integer expectedVersion) {
        get(tenantId, id);
        return client.close(tenantId, id, expectedVersion);
    }

    private static int normalizeLimit(int limit) {
        if (limit <= 0) {
            return DEFAULT_SEARCH_LIMIT;
        }
        return Math.min(limit, MAX_SEARCH_LIMIT);
    }

    private static boolean matches(String needle, Object... values) {
        for (Object value : values) {
            if (value != null && value.toString().toLowerCase(Locale.ROOT).contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private static ClaimSearchResponse.Item searchItem(ClaimDto claim) {
        return new ClaimSearchResponse.Item(
                claim.id(),
                SEARCH_TYPE,
                claim.claimNumber(),
                secondary(claim.claimType(), claim.status()),
                claim.status(),
                "/direct-claims?record=" + claim.id()
        );
    }

    private static String secondary(String first, String status) {
        return (first == null ? "" : first) + " · " + (status == null ? "" : status);
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
