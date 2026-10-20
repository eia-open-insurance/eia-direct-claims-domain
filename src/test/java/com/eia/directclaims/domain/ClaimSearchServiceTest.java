package com.eia.directclaims.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClaimSearchServiceTest {
    private static final UUID TENANT = UUID.fromString("11111111-1111-7111-8111-111111111111");
    private static final UUID OTHER_TENANT = UUID.fromString("99999999-9999-7999-8999-999999999999");
    private static final UUID CLAIMANT = UUID.fromString("33333333-3333-7333-8333-333333333333");
    private static final UUID POLICY = UUID.fromString("aaaaaaaa-aaaa-7aaa-8aaa-aaaaaaaaaaaa");
    private static final Instant BASE = Instant.parse("2026-03-18T09:00:00Z");

    @Mock
    private ClaimClient client;

    @InjectMocks
    private ClaimService service;

    @Test
    void matchesClaimNumberCaseInsensitively() {
        ClaimDto claim = claim(1, "CLM-2026-0000042", "BODILY_INJURY", "GL", POLICY, "REGISTERED",
                "DIRECT", TENANT, BASE);
        when(client.findAll(TENANT, null)).thenReturn(List.of(claim));

        ClaimSearchResponse result = service.search(TENANT, "clm-2026-0000042", 10);

        assertEquals(1, result.items().size());
        assertEquals(claim.id(), result.items().get(0).id());
    }

    @Test
    void matchesClaimType() {
        ClaimDto claim = claim(1, "CLM-2026-0000042", "PROPERTY_DAMAGE", "GL", POLICY, "REGISTERED",
                "DIRECT", TENANT, BASE);
        when(client.findAll(TENANT, null)).thenReturn(List.of(claim));

        ClaimSearchResponse result = service.search(TENANT, "property_damage", 10);

        assertEquals(1, result.items().size());
        assertEquals("CLM-2026-0000042", result.items().get(0).label());
    }

    @Test
    void matchesCauseCode() {
        ClaimDto claim = claim(1, "CLM-2026-0000042", "BODILY_INJURY", "AUTO", POLICY, "REGISTERED",
                "DIRECT", TENANT, BASE);
        when(client.findAll(TENANT, null)).thenReturn(List.of(claim));

        ClaimSearchResponse result = service.search(TENANT, "aut", 10);

        assertEquals(1, result.items().size());
    }

    @Test
    void matchesPolicyId() {
        ClaimDto claim = claim(1, "CLM-2026-0000042", "BODILY_INJURY", "GL", POLICY, "REGISTERED",
                "DIRECT", TENANT, BASE);
        when(client.findAll(TENANT, null)).thenReturn(List.of(claim));

        ClaimSearchResponse result = service.search(TENANT, "AAAAAAAA-AAAA-7AAA", 10);

        assertEquals(1, result.items().size());
        assertEquals("CLM-2026-0000042", result.items().get(0).label());
    }

    @Test
    void blankQueryReturnsTheLatestTenByUpdatedAt() {
        List<ClaimDto> claims = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            claims.add(claim(i, "CLM-2026-" + String.format("%07d", i), "BODILY_INJURY", "GL",
                    POLICY, "REGISTERED", "DIRECT", TENANT, BASE.plusSeconds(i)));
        }
        when(client.findAll(TENANT, null)).thenReturn(claims.reversed());

        ClaimSearchResponse result = service.search(TENANT, "   ", 10);

        assertEquals("", result.query());
        assertEquals(ClaimService.DEFAULT_SEARCH_LIMIT, result.limit());
        assertEquals(10, result.items().size());
        assertEquals(claims.get(11).id(), result.items().get(0).id());
        assertEquals(claims.get(2).id(), result.items().get(9).id());
    }

    @Test
    void honorsRequestedLimit() {
        when(client.findAll(TENANT, null)).thenReturn(claims(5));

        ClaimSearchResponse result = service.search(TENANT, "", 2);

        assertEquals(2, result.limit());
        assertEquals(2, result.items().size());
    }

    @Test
    void capsLimitAtTwentyFive() {
        when(client.findAll(TENANT, null)).thenReturn(claims(30));

        ClaimSearchResponse result = service.search(TENANT, "", 100);

        assertEquals(ClaimService.MAX_SEARCH_LIMIT, result.limit());
        assertEquals(25, result.items().size());
    }

    @Test
    void defaultsNonPositiveLimitToTen() {
        when(client.findAll(TENANT, null)).thenReturn(claims(12));

        ClaimSearchResponse result = service.search(TENANT, "", 0);

        assertEquals(ClaimService.DEFAULT_SEARCH_LIMIT, result.limit());
        assertEquals(10, result.items().size());
    }

    @Test
    void excludesAnotherTenantsClaims() {
        ClaimDto mine = claim(1, "CLM-2026-0000001", "BODILY_INJURY", "GL", POLICY, "REGISTERED",
                "DIRECT", TENANT, BASE);
        ClaimDto other = claim(2, "CLM-2026-0000002", "BODILY_INJURY", "GL", POLICY, "REGISTERED",
                "DIRECT", OTHER_TENANT, BASE);
        when(client.findAll(TENANT, null)).thenReturn(List.of(mine, other));

        ClaimSearchResponse result = service.search(TENANT, "", 10);

        assertEquals(1, result.items().size());
        assertEquals(mine.id(), result.items().get(0).id());
    }

    @Test
    void bookWallExcludesRowsOutsideTheDirectBook() {
        ClaimDto direct = claim(1, "CLM-2026-0000001", "BODILY_INJURY", "GL", POLICY, "REGISTERED",
                "DIRECT", TENANT, BASE);
        ClaimDto assumed = claim(2, "CLM-2026-0000002", "BODILY_INJURY", "GL", POLICY, "REGISTERED",
                "ASSUMED", TENANT, BASE);
        when(client.findAll(TENANT, null)).thenReturn(List.of(direct, assumed));

        ClaimSearchResponse result = service.search(TENANT, "CLM", 10);

        assertEquals(1, result.items().size());
        assertEquals(direct.id(), result.items().get(0).id());
    }

    @Test
    void projectsTheCommonSearchItemShape() {
        ClaimDto claim = claim(1, "CLM-2026-0000001", "BODILY_INJURY", "GL", POLICY, "REGISTERED",
                "DIRECT", TENANT, BASE);
        when(client.findAll(TENANT, null)).thenReturn(List.of(claim));

        ClaimSearchResponse.Item item = service.search(TENANT, "CLM", 10).items().get(0);

        assertEquals(claim.id(), item.id());
        assertEquals("DIRECT_CLAIM", item.type());
        assertEquals("CLM-2026-0000001", item.label());
        assertEquals("BODILY_INJURY · REGISTERED", item.secondary());
        assertEquals("REGISTERED", item.status());
        assertEquals("/direct-claims?record=" + claim.id(), item.href());
    }

    @Test
    void noMatchReturnsAnEmptyEnvelope() {
        when(client.findAll(TENANT, null)).thenReturn(claims(3));

        ClaimSearchResponse result = service.search(TENANT, "ZZZ-NO-SUCH-CLAIM", 10);

        assertTrue(result.items().isEmpty());
        assertEquals("ZZZ-NO-SUCH-CLAIM", result.query());
        assertEquals(10, result.limit());
    }

    private static List<ClaimDto> claims(int count) {
        List<ClaimDto> claims = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            claims.add(claim(i, "CLM-2026-" + String.format("%07d", i), "BODILY_INJURY", "GL",
                    POLICY, "REGISTERED", "DIRECT", TENANT, BASE.plusSeconds(i)));
        }
        return claims;
    }

    private static ClaimDto claim(int sequence, String number, String type, String cause, UUID policy,
                                  String status, String book, UUID tenant, Instant updatedAt) {
        return new ClaimDto(
                UUID.fromString(String.format("0192f3a4-2b7e-7d1f-a9c4-%012d", sequence)),
                tenant,
                book,
                number,
                policy,
                CLAIMANT,
                type,
                status,
                LocalDate.of(2026, 3, 14),
                LocalDate.of(2026, 3, 18),
                cause,
                new BigDecimal("125000.0000"),
                "USD",
                null,
                null,
                false,
                new BigDecimal("125000.0000"),
                0,
                updatedAt,
                updatedAt);
    }
}
