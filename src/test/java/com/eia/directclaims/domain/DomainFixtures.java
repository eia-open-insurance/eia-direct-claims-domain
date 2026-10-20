package com.eia.directclaims.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Shared domain fixtures for the second-slice tests. */
final class DomainFixtures {
    static final UUID TENANT = UUID.fromString("11111111-1111-7111-8111-111111111111");
    static final UUID CLAIM_ID = UUID.fromString("0192f3a4-2b7e-7d1f-a9c4-3e8b5f0c9d21");
    static final UUID POLICY_ID = UUID.fromString("22222222-2222-7222-8222-222222222222");
    static final UUID CLAIMANT_ID = UUID.fromString("33333333-3333-7333-8333-333333333333");
    static final UUID OTHER_ID = UUID.fromString("44444444-4444-7444-8444-444444444444");
    static final UUID INJURY_ID = UUID.fromString("0192f3a4-2b7e-7d1f-a9c4-3e8b5f0c9d31");
    static final UUID EXPOSURE_ID = UUID.fromString("0192f3a4-2b7e-7d1f-a9c4-3e8b5f0c9d32");
    static final UUID RESERVE_ID = UUID.fromString("0192f3a4-2b7e-7d1f-a9c4-3e8b5f0c9d33");
    static final UUID COVERAGE_ID = UUID.fromString("0192f3a4-2b7e-7d1f-a9c4-3e8b5f0c9d34");
    static final UUID LITIGATION_ID = UUID.fromString("0192f3a4-2b7e-7d1f-a9c4-3e8b5f0c9d35");
    static final UUID SETTLEMENT_ID = UUID.fromString("0192f3a4-2b7e-7d1f-a9c4-3e8b5f0c9d36");
    static final UUID RECOVERY_ID = UUID.fromString("0192f3a4-2b7e-7d1f-a9c4-3e8b5f0c9d37");
    static final UUID LIEN_ID = UUID.fromString("0192f3a4-2b7e-7d1f-a9c4-3e8b5f0c9d38");
    static final UUID TASK_ID = UUID.fromString("0192f3a4-2b7e-7d1f-a9c4-3e8b5f0c9d39");
    static final UUID NOTE_ID = UUID.fromString("0192f3a4-2b7e-7d1f-a9c4-3e8b5f0c9d3a");
    static final UUID ALLOCATION_ID = UUID.fromString("0192f3a4-2b7e-7d1f-a9c4-3e8b5f0c9d3b");
    static final UUID LIMIT_ID = UUID.fromString("66666666-6666-7666-8666-666666666666");
    static final Instant NOW = Instant.parse("2026-03-18T09:00:00Z");

    private DomainFixtures() {
    }

    static ClaimDto claim(String status, int version) {
        return new ClaimDto(CLAIM_ID, TENANT, "DIRECT", "CLM-2026-0000001", POLICY_ID, CLAIMANT_ID,
                "BODILY_INJURY", status, LocalDate.of(2026, 3, 14), LocalDate.of(2026, 3, 18), "GL",
                new BigDecimal("125000.0000"), "USD", new BigDecimal("500000.0000"), "USD", false,
                null, null, null, new BigDecimal("125000.0000"), version, NOW, NOW);
    }

    static ClaimDto assignedClaim(String status, int version) {
        return new ClaimDto(CLAIM_ID, TENANT, "DIRECT", "CLM-2026-0000001", POLICY_ID, CLAIMANT_ID,
                "BODILY_INJURY", status, LocalDate.of(2026, 3, 14), LocalDate.of(2026, 3, 18), "GL",
                new BigDecimal("125000.0000"), "USD", new BigDecimal("500000.0000"), "USD", false,
                "adjuster@example.com", "CASUALTY", NOW, new BigDecimal("125000.0000"), version,
                NOW, NOW);
    }

    static ClaimDetailDtos.Injury injury() {
        return new ClaimDetailDtos.Injury(INJURY_ID, TENANT, CLAIM_ID, CLAIMANT_ID, "MESOTHELIOMA",
                "LUNG", List.of("C45.0"), "Mesothelioma", "SEVERE", LocalDate.of(2026, 1, 10), 1,
                NOW, NOW);
    }

    static ClaimDetailDtos.Exposure exposure() {
        return new ClaimDetailDtos.Exposure(EXPOSURE_ID, TENANT, CLAIM_ID, CLAIMANT_ID, "SUBSTANCE",
                null, "Chrysotile asbestos", "Akron plant", null, "Machinist", "US-OH",
                LocalDate.of(2015, 1, 1), LocalDate.of(2020, 12, 31), "HIGH", 1, NOW, NOW);
    }

    static ClaimDetailDtos.ReserveEntry reserve() {
        return new ClaimDetailDtos.ReserveEntry(RESERVE_ID, TENANT, CLAIM_ID, "INDEMNITY",
                new BigDecimal("100000.0000"), "USD", LocalDate.of(2026, 3, 18), "INITIAL",
                "Initial reserve", "Manager", 1, true, 1, NOW);
    }

    static ClaimDetailDtos.CoverageDecision coverage(String position) {
        return new ClaimDetailDtos.CoverageDecision(COVERAGE_ID, TENANT, CLAIM_ID, position,
                "Policy year 2024 responds", "Coverage A", "PRO_RATA_TIME",
                new BigDecimal("0.250000"), LocalDate.of(2026, 3, 18), OTHER_ID, "FINAL", 1, NOW, NOW);
    }

    static ClaimDetailDtos.Litigation litigation() {
        return new ClaimDetailDtos.Litigation(LITIGATION_ID, TENANT, CLAIM_ID, "DEFENSE", "US-OH",
                OTHER_ID, LocalDate.of(2026, 2, 1), LocalDate.of(2026, 9, 1), "ACTIVE", "CV-1", 1,
                NOW, NOW);
    }

    static ClaimDetailDtos.Settlement settlement(String status) {
        return new ClaimDetailDtos.Settlement(SETTLEMENT_ID, TENANT, CLAIM_ID, "SET-2026-0000001",
                new BigDecimal("60000.0000"), "USD", "LUMP_SUM", "FULL", "Claims Committee",
                "Wire to claimant trust account", OTHER_ID, status, OTHER_ID, NOW, NOW, 1, NOW, NOW);
    }

    static ClaimDetailDtos.Recovery recovery(String status) {
        return new ClaimDetailDtos.Recovery(RECOVERY_ID, TENANT, CLAIM_ID, "SUBROGATION",
                new BigDecimal("20000.0000"), "USD", "Acme", status, LocalDate.of(2026, 5, 1), 1,
                NOW, NOW);
    }

    static ClaimDetailDtos.Lien lien(String status) {
        return new ClaimDetailDtos.Lien(LIEN_ID, TENANT, CLAIM_ID, "MEDICARE", "CMS",
                new BigDecimal("15000.0000"), "USD", status, null, null, 1, NOW, NOW);
    }

    static ClaimDetailDtos.Task task(String status) {
        return new ClaimDetailDtos.Task(TASK_ID, CLAIM_ID, "TSK-2026-0000001",
                "Obtain updated medical records", "Provider records requested at FNOL",
                LocalDate.of(2026, 4, 1), "HIGH", status, "A. Adjuster", 1, NOW, NOW,
                "COMPLETE".equals(status) ? NOW : null);
    }

    static ClaimDetailDtos.Note note() {
        return new ClaimDetailDtos.Note(NOTE_ID, CLAIM_ID, "NTE-2026-0000001", "DIARY",
                "Called the claimant to confirm the medical authorization", "Dana Handler", NOW);
    }

    static ClaimDetailDtos.Allocation allocation() {
        return new ClaimDetailDtos.Allocation(ALLOCATION_ID, CLAIM_ID, POLICY_ID, null, 2024,
                "PRIMARY", new BigDecimal("0.250000"), new BigDecimal("31250.0000"),
                "NOT_POSTED", null, null, NOW);
    }

    static ClaimDetailDtos.Allocation allocationWithLimit() {
        return new ClaimDetailDtos.Allocation(ALLOCATION_ID, CLAIM_ID, POLICY_ID, LIMIT_ID, 2024,
                "PRIMARY", new BigDecimal("0.250000"), new BigDecimal("31250.0000"),
                "NOT_POSTED", null, null, NOW);
    }

    static ClaimDetailDtos.Allocation postedAllocation(String message) {
        return new ClaimDetailDtos.Allocation(ALLOCATION_ID, CLAIM_ID, POLICY_ID, LIMIT_ID, 2024,
                "PRIMARY", new BigDecimal("0.250000"), new BigDecimal("31250.0000"),
                "POSTED", message, NOW, NOW);
    }

    static ClaimDetailDtos.Allocation failedAllocation(String message) {
        return new ClaimDetailDtos.Allocation(ALLOCATION_ID, CLAIM_ID, POLICY_ID, LIMIT_ID, 2024,
                "PRIMARY", new BigDecimal("0.250000"), new BigDecimal("31250.0000"),
                "FAILED", message, null, NOW);
    }

    static ClaimDetailDtos.AllocationSummary allocationSummary() {
        return new ClaimDetailDtos.AllocationSummary(List.of(allocation()),
                new BigDecimal("0.250000"), new BigDecimal("31250.0000"));
    }
}
