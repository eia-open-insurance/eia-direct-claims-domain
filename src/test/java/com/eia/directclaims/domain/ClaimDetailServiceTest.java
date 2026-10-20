package com.eia.directclaims.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClaimDetailServiceTest {
    private static final UUID TENANT = UUID.fromString("11111111-1111-7111-8111-111111111111");
    private static final UUID CLAIM_ID = UUID.fromString("0192f3a4-2b7e-7d1f-a9c4-3e8b5f0c9d21");
    private static final UUID CLAIMANT_ID = UUID.fromString("33333333-3333-7333-8333-333333333333");
    private static final UUID OTHER_ID = UUID.fromString("44444444-4444-7444-8444-444444444444");

    @Mock
    private ClaimClient client;

    @InjectMocks
    private ClaimDetailService service;

    @Test
    void injuryRequiresAClaimant() {
        ClaimCommands.Injury command = injury(null);

        DomainValidationException error = assertThrows(DomainValidationException.class,
                () -> service.addInjury(TENANT, CLAIM_ID, command));

        assertEquals("claimantPartyId is required", error.getMessage());
        verifyNoInteractions(client);
    }

    @Test
    void injuryRequiresATypeAndSeverity() {
        assertThrows(DomainValidationException.class,
                () -> service.addInjury(TENANT, CLAIM_ID, injury(CLAIMANT_ID, null, "SEVERE")));
        assertThrows(DomainValidationException.class,
                () -> service.addInjury(TENANT, CLAIM_ID, injury(CLAIMANT_ID, "MESOTHELIOMA", null)));
        verifyNoInteractions(client);
    }

    @Test
    void injuryDelegatesToTheExpert() {
        ClaimCommands.Injury command = injury(CLAIMANT_ID);
        ClaimDetailDtos.Injury created = DomainFixtures.injury();
        when(client.addInjury(TENANT, CLAIM_ID, command)).thenReturn(created);

        assertEquals(created, service.addInjury(TENANT, CLAIM_ID, command));
    }

    @Test
    void injuryUpdateRequiresAVersion() {
        assertThrows(DomainValidationException.class,
                () -> service.updateInjury(TENANT, CLAIM_ID, DomainFixtures.INJURY_ID,
                        new ClaimCommands.Injury(null, null, null, null, null, null, null, null)));
    }

    @Test
    void exposureCannotPostDateTheReportDate() {
        when(client.findById(TENANT, CLAIM_ID)).thenReturn(DomainFixtures.claim("REGISTERED", 0));

        DomainValidationException error = assertThrows(DomainValidationException.class,
                () -> service.addExposure(TENANT, CLAIM_ID,
                        exposure(LocalDate.of(2015, 1, 1), LocalDate.of(2026, 4, 1))));

        assertTrue(error.getMessage().contains("after reportDate"));
    }

    @Test
    void exposureStartCannotPostDateTheReportDate() {
        when(client.findById(TENANT, CLAIM_ID)).thenReturn(DomainFixtures.claim("REGISTERED", 0));

        assertThrows(DomainValidationException.class, () -> service.addExposure(TENANT, CLAIM_ID,
                exposure(LocalDate.of(2026, 3, 19), null)));
    }

    @Test
    void exposureEndCannotPrecedeTheStart() {
        when(client.findById(TENANT, CLAIM_ID)).thenReturn(DomainFixtures.claim("REGISTERED", 0));

        assertThrows(DomainValidationException.class, () -> service.addExposure(TENANT, CLAIM_ID,
                exposure(LocalDate.of(2016, 1, 1), LocalDate.of(2015, 1, 1))));
    }

    @Test
    void exposureRequiresJurisdiction() {
        ClaimCommands.Exposure command = new ClaimCommands.Exposure(CLAIMANT_ID, "SUBSTANCE", null,
                null, null, null, null, null, LocalDate.of(2015, 1, 1), null, "HIGH", null);

        assertThrows(DomainValidationException.class, () -> service.addExposure(TENANT, CLAIM_ID, command));
        verifyNoInteractions(client);
    }

    @Test
    void exposureDelegatesWithinTheReportDate() {
        when(client.findById(TENANT, CLAIM_ID)).thenReturn(DomainFixtures.claim("REGISTERED", 0));
        ClaimCommands.Exposure command = exposure(LocalDate.of(2015, 1, 1), LocalDate.of(2020, 12, 31));
        ClaimDetailDtos.Exposure created = DomainFixtures.exposure();
        when(client.addExposure(TENANT, CLAIM_ID, command)).thenReturn(created);

        assertEquals(created, service.addExposure(TENANT, CLAIM_ID, command));
    }

    @Test
    void reserveRejectsNegativeAmounts() {
        ClaimCommands.ReserveChange command = reserve("-1.00", "INITIAL", 0);

        DomainValidationException error = assertThrows(DomainValidationException.class,
                () -> service.changeReserve(TENANT, CLAIM_ID, command));

        assertTrue(error.getMessage().contains("zero or greater"));
        verifyNoInteractions(client);
    }

    @Test
    void reserveRequiresAReason() {
        assertThrows(DomainValidationException.class,
                () -> service.changeReserve(TENANT, CLAIM_ID, reserve("100.00", null, 0)));
        verifyNoInteractions(client);
    }

    @Test
    void reserveRequiresAVersion() {
        assertThrows(DomainValidationException.class,
                () -> service.changeReserve(TENANT, CLAIM_ID, reserve("100.00", "INITIAL", null)));
        verifyNoInteractions(client);
    }

    @Test
    void reserveDelegatesAValidChange() {
        ClaimCommands.ReserveChange command = reserve("100.00", "INITIAL", 0);
        ClaimDetailDtos.ReserveEntry entry = DomainFixtures.reserve();
        when(client.changeReserve(TENANT, CLAIM_ID, command)).thenReturn(entry);

        assertEquals(entry, service.changeReserve(TENANT, CLAIM_ID, command));
    }

    @Test
    void coverageRejectsAnUnknownPosition() {
        ClaimCommands.CoverageDecision command = coverage("MAYBE");

        DomainValidationException error = assertThrows(DomainValidationException.class,
                () -> service.decideCoverage(TENANT, CLAIM_ID, command));

        assertTrue(error.getMessage().contains("position"));
        verifyNoInteractions(client);
    }

    @Test
    void coverageRequiresBasisReviewerAndDate() {
        assertThrows(DomainValidationException.class,
                () -> service.decideCoverage(TENANT, CLAIM_ID, coverage("COVERED", " ", OTHER_ID)));
        assertThrows(DomainValidationException.class,
                () -> service.decideCoverage(TENANT, CLAIM_ID, coverage("COVERED", "Basis", null)));
        assertThrows(DomainValidationException.class,
                () -> service.decideCoverage(TENANT, CLAIM_ID, new ClaimCommands.CoverageDecision(
                        "COVERED", "Basis", null, null, null, null, OTHER_ID)));
    }

    @Test
    void coverageRejectsAnAllocationAboveOne() {
        ClaimCommands.CoverageDecision command = new ClaimCommands.CoverageDecision("COVERED",
                "Basis", "Coverage A", "PRO_RATA_TIME", new BigDecimal("1.2"),
                LocalDate.of(2026, 3, 18), OTHER_ID);

        assertThrows(DomainValidationException.class,
                () -> service.decideCoverage(TENANT, CLAIM_ID, command));
    }

    @Test
    void coverageDelegatesAValidDecision() {
        ClaimCommands.CoverageDecision command = coverage("DEFENSE_ONLY");
        ClaimDetailDtos.CoverageDecision decision = DomainFixtures.coverage("DEFENSE_ONLY");
        when(client.decideCoverage(TENANT, CLAIM_ID, command)).thenReturn(decision);

        assertEquals(decision, service.decideCoverage(TENANT, CLAIM_ID, command));
    }

    // ------------------------------------------------------------------ allocations
    @Test
    void allocationRequiresPolicyYearAndLayer() {
        assertThrows(DomainValidationException.class, () -> service.allocate(TENANT, CLAIM_ID,
                new ClaimCommands.Allocation(null, 2024, "PRIMARY", new BigDecimal("0.25"),
                        new BigDecimal("1000"))));
        assertThrows(DomainValidationException.class, () -> service.allocate(TENANT, CLAIM_ID,
                new ClaimCommands.Allocation(DomainFixtures.POLICY_ID, null, "PRIMARY",
                        new BigDecimal("0.25"), new BigDecimal("1000"))));
        assertThrows(DomainValidationException.class, () -> service.allocate(TENANT, CLAIM_ID,
                new ClaimCommands.Allocation(DomainFixtures.POLICY_ID, 2024, " ",
                        new BigDecimal("0.25"), new BigDecimal("1000"))));
        verifyNoInteractions(client);
    }

    @Test
    void allocationRejectsAnImpossibleYearOrAnOverlongLayer() {
        assertThrows(DomainValidationException.class, () -> service.allocate(TENANT, CLAIM_ID,
                new ClaimCommands.Allocation(DomainFixtures.POLICY_ID, 1899, "PRIMARY",
                        new BigDecimal("0.25"), new BigDecimal("1000"))));
        assertThrows(DomainValidationException.class, () -> service.allocate(TENANT, CLAIM_ID,
                new ClaimCommands.Allocation(DomainFixtures.POLICY_ID, 2024,
                        "A".repeat(33), new BigDecimal("0.25"), new BigDecimal("1000"))));
        verifyNoInteractions(client);
    }

    @Test
    void allocationRejectsAPercentageOutsideTheOpenUnitInterval() {
        assertThrows(DomainValidationException.class, () -> service.allocate(TENANT, CLAIM_ID,
                new ClaimCommands.Allocation(DomainFixtures.POLICY_ID, 2024, "PRIMARY",
                        BigDecimal.ZERO, new BigDecimal("1000"))));
        assertThrows(DomainValidationException.class, () -> service.allocate(TENANT, CLAIM_ID,
                new ClaimCommands.Allocation(DomainFixtures.POLICY_ID, 2024, "PRIMARY",
                        new BigDecimal("1.000001"), new BigDecimal("1000"))));
        assertThrows(DomainValidationException.class, () -> service.allocate(TENANT, CLAIM_ID,
                new ClaimCommands.Allocation(DomainFixtures.POLICY_ID, 2024, "PRIMARY", null,
                        new BigDecimal("1000"))));
        verifyNoInteractions(client);
    }

    @Test
    void allocationRejectsANegativeAmount() {
        DomainValidationException error = assertThrows(DomainValidationException.class,
                () -> service.allocate(TENANT, CLAIM_ID,
                        new ClaimCommands.Allocation(DomainFixtures.POLICY_ID, 2024, "PRIMARY",
                                new BigDecimal("0.25"), new BigDecimal("-0.01"))));
        assertTrue(error.getMessage().contains("zero or greater"));
        verifyNoInteractions(client);
    }

    @Test
    void allocationDelegatesAValidSplit() {
        ClaimCommands.Allocation command = new ClaimCommands.Allocation(DomainFixtures.POLICY_ID,
                2024, "PRIMARY", new BigDecimal("0.25"), new BigDecimal("31250.0000"));
        ClaimDetailDtos.Allocation created = DomainFixtures.allocation();
        when(client.createAllocation(TENANT, CLAIM_ID, command)).thenReturn(created);

        assertEquals(created, service.allocate(TENANT, CLAIM_ID, command));
    }

    @Test
    void allocationSummaryDelegatesToTheExpert() {
        ClaimDetailDtos.AllocationSummary summary = DomainFixtures.allocationSummary();
        when(client.findAllocations(TENANT, CLAIM_ID)).thenReturn(summary);

        assertSame(summary, service.allocations(TENANT, CLAIM_ID));
    }

    @Test
    void litigationRejectsATrialBeforeFiling() {
        ClaimCommands.Litigation command = new ClaimCommands.Litigation("DEFENSE", "US-OH", OTHER_ID,
                LocalDate.of(2026, 2, 1), LocalDate.of(2026, 1, 1), "ACTIVE", null, null);

        DomainValidationException error = assertThrows(DomainValidationException.class,
                () -> service.addLitigation(TENANT, CLAIM_ID, command));

        assertTrue(error.getMessage().contains("trialDate"));
        verifyNoInteractions(client);
    }

    @Test
    void litigationRequiresCounselAndJurisdiction() {
        assertThrows(DomainValidationException.class,
                () -> service.addLitigation(TENANT, CLAIM_ID, new ClaimCommands.Litigation("DEFENSE",
                        "US-OH", null, LocalDate.of(2026, 2, 1), null, "ACTIVE", null, null)));
        assertThrows(DomainValidationException.class,
                () -> service.addLitigation(TENANT, CLAIM_ID, new ClaimCommands.Litigation("DEFENSE",
                        " ", OTHER_ID, LocalDate.of(2026, 2, 1), null, "ACTIVE", null, null)));
    }

    @Test
    void litigationDelegatesAValidMatter() {
        ClaimCommands.Litigation command = new ClaimCommands.Litigation("DEFENSE", "US-OH", OTHER_ID,
                LocalDate.of(2026, 2, 1), LocalDate.of(2026, 9, 1), "ACTIVE", "CV-1", null);
        ClaimDetailDtos.Litigation matter = DomainFixtures.litigation();
        when(client.addLitigation(TENANT, CLAIM_ID, command)).thenReturn(matter);

        assertEquals(matter, service.addLitigation(TENANT, CLAIM_ID, command));
    }

    @Test
    void settlementRejectsANonPositiveAmount() {
        assertThrows(DomainValidationException.class, () -> service.proposeSettlement(TENANT, CLAIM_ID,
                new ClaimCommands.Settlement(BigDecimal.ZERO, "USD", "LUMP_SUM", "FULL", null)));
    }

    @Test
    void settlementRequiresAStructure() {
        assertThrows(DomainValidationException.class, () -> service.proposeSettlement(TENANT, CLAIM_ID,
                new ClaimCommands.Settlement(new BigDecimal("1000"), "USD", null, "FULL", null)));
    }

    @Test
    void settlementApprovalRequiresAuthorityAndReviewer() {
        assertThrows(DomainValidationException.class, () -> service.approveSettlement(TENANT, CLAIM_ID,
                DomainFixtures.SETTLEMENT_ID, new ClaimCommands.SettlementApproval(null, OTHER_ID, 1)));
        assertThrows(DomainValidationException.class, () -> service.approveSettlement(TENANT, CLAIM_ID,
                DomainFixtures.SETTLEMENT_ID, new ClaimCommands.SettlementApproval("Committee", null, 1)));
    }

    @Test
    void settlementExecutionRequiresARelease() {
        assertThrows(DomainValidationException.class, () -> service.executeSettlement(TENANT, CLAIM_ID,
                DomainFixtures.SETTLEMENT_ID, new ClaimCommands.SettlementExecution(null, "Wire", 1)));
    }

    @Test
    void recoveryRequiresADateWhenReceived() {
        ClaimCommands.Recovery command = new ClaimCommands.Recovery("SUBROGATION",
                new BigDecimal("1000"), "USD", "Acme", "RECEIVED", null, null);

        DomainValidationException error = assertThrows(DomainValidationException.class,
                () -> service.recordRecovery(TENANT, CLAIM_ID, command));

        assertTrue(error.getMessage().contains("recoveryDate"));
        verifyNoInteractions(client);
    }

    @Test
    void recoveryDelegatesWhenValid() {
        ClaimCommands.Recovery command = new ClaimCommands.Recovery("SUBROGATION",
                new BigDecimal("1000"), "USD", "Acme", "RECEIVED", LocalDate.of(2026, 5, 1), null);
        ClaimDetailDtos.Recovery recovery = DomainFixtures.recovery("RECEIVED");
        when(client.recordRecovery(TENANT, CLAIM_ID, command)).thenReturn(recovery);

        assertEquals(recovery, service.recordRecovery(TENANT, CLAIM_ID, command));
    }

    @Test
    void resolvingALienRequiresAResolutionType() {
        ClaimCommands.Lien command = new ClaimCommands.Lien("MEDICARE", "CMS", new BigDecimal("1000"),
                "USD", "RESOLVED", null, null);

        DomainValidationException error = assertThrows(DomainValidationException.class,
                () -> service.recordLien(TENANT, CLAIM_ID, command));

        assertTrue(error.getMessage().contains("resolutionType"));
        verifyNoInteractions(client);
    }

    @Test
    void lienDelegatesWhenValid() {
        ClaimCommands.Lien command = new ClaimCommands.Lien("MEDICARE", "CMS",
                new BigDecimal("1000"), "USD", "IDENTIFIED", null, null);
        ClaimDetailDtos.Lien lien = DomainFixtures.lien("IDENTIFIED");
        when(client.recordLien(TENANT, CLAIM_ID, command)).thenReturn(lien);

        assertEquals(lien, service.recordLien(TENANT, CLAIM_ID, command));
    }

    @Test
    void lienUpdateRequiresAVersion() {
        assertThrows(DomainValidationException.class, () -> service.updateLien(TENANT, CLAIM_ID,
                DomainFixtures.LIEN_ID, new ClaimCommands.Lien(null, null, null, null, null, null, null)));
    }

    @Test
    void listsDelegateToTheExpert() {
        when(client.findInjuries(TENANT, CLAIM_ID)).thenReturn(List.of(DomainFixtures.injury()));
        assertEquals(1, service.injuries(TENANT, CLAIM_ID).size());
        verify(client).findInjuries(TENANT, CLAIM_ID);
    }

    @Test
    void taskRequiresTitleDueDateAndPriority() {
        assertThrows(DomainValidationException.class, () -> service.addTask(TENANT, CLAIM_ID,
                new ClaimCommands.Task(" ", null, LocalDate.of(2026, 4, 1), "HIGH", null)));
        assertThrows(DomainValidationException.class, () -> service.addTask(TENANT, CLAIM_ID,
                new ClaimCommands.Task("Title", null, null, "HIGH", null)));
        assertThrows(DomainValidationException.class, () -> service.addTask(TENANT, CLAIM_ID,
                new ClaimCommands.Task("Title", null, LocalDate.of(2026, 4, 1), " ", null)));
        verifyNoInteractions(client);
    }

    @Test
    void taskRejectsAnUnknownPriority() {
        DomainValidationException error = assertThrows(DomainValidationException.class,
                () -> service.addTask(TENANT, CLAIM_ID,
                        new ClaimCommands.Task("Title", null, LocalDate.of(2026, 4, 1),
                                "CATASTROPHE", null)));

        assertTrue(error.getMessage().contains("priority"));
        verifyNoInteractions(client);
    }

    @Test
    void taskDelegatesAValidCreate() {
        ClaimCommands.Task command = new ClaimCommands.Task("Obtain records", "Provider records",
                LocalDate.of(2026, 4, 1), "high", "A. Adjuster");
        ClaimDetailDtos.Task created = DomainFixtures.task("OPEN");
        when(client.addTask(TENANT, CLAIM_ID, command)).thenReturn(created);

        assertEquals(created, service.addTask(TENANT, CLAIM_ID, command));
    }

    @Test
    void taskForwardsTheIdempotencyKeyAndPreservesTheReplayHeader() {
        ClaimCommands.Task command = new ClaimCommands.Task("Obtain records", "Provider records",
                LocalDate.of(2026, 4, 1), "high", "A. Adjuster");
        ClaimDetailDtos.Task created = DomainFixtures.task("OPEN");
        ClaimCommandResult<ClaimDetailDtos.Task> forwarded = new ClaimCommandResult<>(created, "true");
        when(client.addTask(TENANT, CLAIM_ID, command, "task-key-1")).thenReturn(forwarded);

        assertSame(forwarded, service.addTask(TENANT, CLAIM_ID, command, "task-key-1"));
    }

    @Test
    void taskStatusRequiresStatusAndVersion() {
        assertThrows(DomainValidationException.class, () -> service.changeTaskStatus(TENANT, CLAIM_ID,
                DomainFixtures.TASK_ID, new ClaimCommands.TaskStatusChange(null, 1L)));
        assertThrows(DomainValidationException.class, () -> service.changeTaskStatus(TENANT, CLAIM_ID,
                DomainFixtures.TASK_ID, new ClaimCommands.TaskStatusChange("COMPLETE", null)));
        verifyNoInteractions(client);
    }

    @Test
    void taskStatusRejectsAnUnknownStatus() {
        DomainValidationException error = assertThrows(DomainValidationException.class,
                () -> service.changeTaskStatus(TENANT, CLAIM_ID, DomainFixtures.TASK_ID,
                        new ClaimCommands.TaskStatusChange("DONE", 1L)));

        assertTrue(error.getMessage().contains("status"));
        verifyNoInteractions(client);
    }

    @Test
    void taskStatusDelegatesWithTheObservedVersion() {
        ClaimCommands.TaskStatusChange command = new ClaimCommands.TaskStatusChange("COMPLETE", 3L);
        ClaimDetailDtos.Task completed = DomainFixtures.task("COMPLETE");
        when(client.changeTaskStatus(TENANT, CLAIM_ID, DomainFixtures.TASK_ID, command))
                .thenReturn(completed);

        assertEquals(completed, service.changeTaskStatus(TENANT, CLAIM_ID,
                DomainFixtures.TASK_ID, command));
    }

    @Test
    void noteRequiresTypeBodyAndAuthor() {
        assertThrows(DomainValidationException.class, () -> service.addNote(TENANT, CLAIM_ID,
                new ClaimCommands.Note(null, "Body", "Author")));
        assertThrows(DomainValidationException.class, () -> service.addNote(TENANT, CLAIM_ID,
                new ClaimCommands.Note("DIARY", " ", "Author")));
        assertThrows(DomainValidationException.class, () -> service.addNote(TENANT, CLAIM_ID,
                new ClaimCommands.Note("DIARY", "Body", " ")));
        verifyNoInteractions(client);
    }

    @Test
    void noteRejectsAnUnknownNoteType() {
        DomainValidationException error = assertThrows(DomainValidationException.class,
                () -> service.addNote(TENANT, CLAIM_ID,
                        new ClaimCommands.Note("RANDOM", "Body", "Author")));

        assertTrue(error.getMessage().contains("noteType"));
        verifyNoInteractions(client);
    }

    @Test
    void noteDelegatesAValidAppend() {
        ClaimCommands.Note command = new ClaimCommands.Note("DIARY", "Called the claimant", "Dana");
        ClaimDetailDtos.Note created = DomainFixtures.note();
        when(client.addNote(TENANT, CLAIM_ID, command)).thenReturn(created);

        assertEquals(created, service.addNote(TENANT, CLAIM_ID, command));
    }

    @Test
    void noteForwardsTheIdempotencyKeyAndPreservesTheReplayHeader() {
        ClaimCommands.Note command = new ClaimCommands.Note("DIARY", "Called the claimant", "Dana");
        ClaimDetailDtos.Note created = DomainFixtures.note();
        ClaimCommandResult<ClaimDetailDtos.Note> forwarded = new ClaimCommandResult<>(created, "false");
        when(client.addNote(TENANT, CLAIM_ID, command, "note-key-1")).thenReturn(forwarded);

        assertSame(forwarded, service.addNote(TENANT, CLAIM_ID, command, "note-key-1"));
    }

    @Test
    void taskAndNoteListsDelegateToTheExpert() {
        when(client.findTasks(TENANT, CLAIM_ID)).thenReturn(List.of(DomainFixtures.task("OPEN")));
        when(client.findNotes(TENANT, CLAIM_ID)).thenReturn(List.of(DomainFixtures.note()));

        assertEquals(1, service.tasks(TENANT, CLAIM_ID).size());
        assertEquals(1, service.notes(TENANT, CLAIM_ID).size());
        verify(client).findTasks(TENANT, CLAIM_ID);
        verify(client).findNotes(TENANT, CLAIM_ID);
    }

    private static ClaimCommands.Injury injury(UUID claimant) {
        return injury(claimant, "MESOTHELIOMA", "SEVERE");
    }

    private static ClaimCommands.Injury injury(UUID claimant, String type, String severity) {
        return new ClaimCommands.Injury(claimant, type, "LUNG", List.of("C45.0"), "Mesothelioma",
                severity, LocalDate.of(2026, 1, 10), null);
    }

    private static ClaimCommands.Exposure exposure(LocalDate from, LocalDate to) {
        return new ClaimCommands.Exposure(CLAIMANT_ID, "SUBSTANCE", null, "Asbestos", "Akron plant",
                null, "Machinist", "US-OH", from, to, "HIGH", null);
    }

    private static ClaimCommands.ReserveChange reserve(String amount, String reason, Integer version) {
        return new ClaimCommands.ReserveChange("INDEMNITY", new BigDecimal(amount), "USD",
                LocalDate.of(2026, 3, 18), reason, "Reserve", "Manager", version);
    }

    private static ClaimCommands.CoverageDecision coverage(String position) {
        return coverage(position, "Policy year 2024 responds", OTHER_ID);
    }

    private static ClaimCommands.CoverageDecision coverage(String position, String basis, UUID reviewer) {
        return new ClaimCommands.CoverageDecision(position, basis, "Coverage A", "PRO_RATA_TIME",
                new BigDecimal("0.25"), LocalDate.of(2026, 3, 18), reviewer);
    }
}
