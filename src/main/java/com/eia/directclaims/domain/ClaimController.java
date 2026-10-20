package com.eia.directclaims.domain;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/claims")
public class ClaimController {
    private final ClaimService service;

    public ClaimController(ClaimService service) {
        this.service = service;
    }

    @GetMapping
    public List<ClaimDto> list(@RequestHeader(value = "X-EIA-Tenant", required = false) String tenantHeader,
                               @RequestHeader(value = "X-EIA-Book", required = false) String book,
                               @RequestParam(value = "status", required = false) String status,
                               @RequestParam(value = "claimantPartyId", required = false) UUID claimantPartyId) {
        RequestGuard.requireDirectBook(book);
        return service.list(RequestGuard.requireTenant(tenantHeader), status, claimantPartyId);
    }

    @GetMapping("/search")
    public ClaimSearchResponse search(@RequestHeader(value = "X-EIA-Tenant", required = false) String tenantHeader,
                                      @RequestHeader(value = "X-EIA-Book", required = false) String book,
                                      @RequestParam(value = "q", required = false, defaultValue = "") String query,
                                      @RequestParam(value = "limit", defaultValue = "10") int limit) {
        RequestGuard.requireDirectBook(book);
        return service.search(RequestGuard.requireTenant(tenantHeader), query, limit);
    }

    @GetMapping("/{id}")
    public ClaimDto get(@RequestHeader(value = "X-EIA-Tenant", required = false) String tenantHeader,
                        @RequestHeader(value = "X-EIA-Book", required = false) String book,
                        @PathVariable UUID id) {
        RequestGuard.requireDirectBook(book);
        return service.get(RequestGuard.requireTenant(tenantHeader), id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ClaimDto create(@RequestHeader(value = "X-EIA-Tenant", required = false) String tenantHeader,
                           @RequestHeader(value = "X-EIA-Book", required = false) String book,
                           @Valid @RequestBody CreateClaimCommand command) {
        RequestGuard.requireDirectBook(book);
        return service.create(RequestGuard.requireTenant(tenantHeader), command);
    }

    @PatchMapping("/{claimId}/assignment")
    public ClaimDto assign(@RequestHeader(value = "X-EIA-Tenant", required = false) String tenantHeader,
                           @RequestHeader(value = "X-EIA-Book", required = false) String book,
                           @RequestHeader(value = "X-EIA-Actor", required = false) String actor,
                           @PathVariable UUID claimId,
                           @Valid @RequestBody ClaimCommands.Assignment command) {
        RequestGuard.requireDirectBook(book);
        return service.assign(RequestGuard.requireTenant(tenantHeader), claimId, command, actor);
    }

    @PatchMapping("/{id}/status")
    public ClaimDto changeStatus(@RequestHeader(value = "X-EIA-Tenant", required = false) String tenantHeader,
                                 @RequestHeader(value = "X-EIA-Book", required = false) String book,
                                 @PathVariable UUID id,
                                 @Valid @RequestBody StatusChangeCommand command) {
        RequestGuard.requireDirectBook(book);
        return service.changeStatus(RequestGuard.requireTenant(tenantHeader), id, command);
    }

    @PostMapping("/{id}/close")
    public ClaimDto close(@RequestHeader(value = "X-EIA-Tenant", required = false) String tenantHeader,
                          @RequestHeader(value = "X-EIA-Book", required = false) String book,
                          @PathVariable UUID id,
                          @RequestBody(required = false) CloseClaimCommand command) {
        RequestGuard.requireDirectBook(book);
        return service.close(RequestGuard.requireTenant(tenantHeader), id,
                command == null ? null : command.expectedVersion());
    }
}
