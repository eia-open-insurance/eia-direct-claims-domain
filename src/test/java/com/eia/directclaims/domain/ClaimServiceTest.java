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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClaimServiceTest {
    private static final UUID TENANT = UUID.fromString("11111111-1111-7111-8111-111111111111");
    private static final UUID CLAIM_ID = UUID.fromString("0192f3a4-2b7e-7d1f-a9c4-3e8b5f0c9d21");
    private static final UUID POLICY_ID = UUID.fromString("22222222-2222-7222-8222-222222222222");
    private static final UUID CLAIMANT_ID = UUID.fromString("33333333-3333-7333-8333-333333333333");
    private static final LocalDate LOSS_DATE = LocalDate.of(2026, 3, 14);
    private static final LocalDate REPORT_DATE = LocalDate.of(2026, 3, 18);

    @Mock
    private ClaimClient client;

    @InjectMocks
    private ClaimService service;

    @Test
    void rejectsReportDateBeforeLossDateWithoutCallingTheExpert() {
        CreateClaimCommand command = command(LOSS_DATE.minusDays(1), new BigDecimal("100.00"));

        DomainValidationException error = assertThrows(DomainValidationException.class,
                () -> service.create(TENANT, command));

        assertEquals("reportDate must be on or after lossDate", error.getMessage());
        verifyNoInteractions(client);
    }

    @Test
    void rejectsNegativeReserveWithoutCallingTheExpert() {
        CreateClaimCommand command = command(REPORT_DATE, new BigDecimal("-0.01"));

        assertThrows(DomainValidationException.class, () -> service.create(TENANT, command));
        verifyNoInteractions(client);
    }

    @Test
    void delegatesValidCreateToTheExpert() {
        CreateClaimCommand command = command(REPORT_DATE, new BigDecimal("125000.00"));
        ClaimDto created = dto("REGISTERED", 0);
        when(client.create(TENANT, command)).thenReturn(created);

        ClaimDto result = service.create(TENANT, command);

        assertSame(created, result);
        verify(client).create(TENANT, command);
    }

    @Test
    void allowsLifecycleTransitionAndCarriesVersion() {
        when(client.findById(TENANT, CLAIM_ID)).thenReturn(dto("REGISTERED", 3));
        when(client.changeStatus(TENANT, CLAIM_ID, ClaimStatus.OPEN, 3)).thenReturn(dto("OPEN", 4));

        ClaimDto updated = service.changeStatus(TENANT, CLAIM_ID, new StatusChangeCommand("OPEN"));

        assertEquals("OPEN", updated.status());
        verify(client).changeStatus(TENANT, CLAIM_ID, ClaimStatus.OPEN, 3);
    }

    @Test
    void rejectsIllegalTransition() {
        when(client.findById(TENANT, CLAIM_ID)).thenReturn(dto("REGISTERED", 0));

        DomainValidationException error = assertThrows(DomainValidationException.class,
                () -> service.changeStatus(TENANT, CLAIM_ID, new StatusChangeCommand("PAID")));

        assertTrue(error.getMessage().contains("REGISTERED -> PAID"));
        verify(client, never()).changeStatus(any(), any(), any(), anyInt());
    }

    @Test
    void rejectsUnknownStatus() {
        when(client.findById(TENANT, CLAIM_ID)).thenReturn(dto("REGISTERED", 0));

        assertThrows(DomainValidationException.class,
                () -> service.changeStatus(TENANT, CLAIM_ID, new StatusChangeCommand("BOGUS")));
    }

    @Test
    void rejectsUnknownStatusFilter() {
        assertThrows(DomainValidationException.class, () -> service.list(TENANT, "BOGUS"));
        verifyNoInteractions(client);
    }

    @Test
    void missingExpertClaimBecomesNotFound() {
        when(client.findById(TENANT, CLAIM_ID)).thenReturn(null);

        assertThrows(ClaimNotFoundException.class, () -> service.get(TENANT, CLAIM_ID));
    }

    @Test
    void listNormalizesStatusForTheExpert() {
        when(client.findAll(TENANT, ClaimStatus.OPEN, null)).thenReturn(List.of(dto("OPEN", 2)));

        List<ClaimDto> result = service.list(TENANT, "open", null);

        assertEquals(1, result.size());
        verify(client).findAll(TENANT, ClaimStatus.OPEN, null);
    }

    @Test
    void listForwardsTheClaimantPartyFilter() {
        when(client.findAll(TENANT, null, CLAIMANT_ID)).thenReturn(List.of(dto("REGISTERED", 0)));

        List<ClaimDto> result = service.list(TENANT, null, CLAIMANT_ID);

        assertEquals(1, result.size());
        verify(client).findAll(TENANT, null, CLAIMANT_ID);
    }

    @Test
    void listForwardsStatusAndClaimantPartyTogether() {
        when(client.findAll(TENANT, ClaimStatus.OPEN, CLAIMANT_ID))
                .thenReturn(List.of(dto("OPEN", 2)));

        List<ClaimDto> result = service.list(TENANT, "open", CLAIMANT_ID);

        assertEquals(1, result.size());
        verify(client).findAll(TENANT, ClaimStatus.OPEN, CLAIMANT_ID);
    }

    @Test
    void closeChecksTheClaimBeforeCallingTheExpert() {
        when(client.findById(TENANT, CLAIM_ID)).thenReturn(dto("PAID", 5));
        when(client.close(TENANT, CLAIM_ID, 5)).thenReturn(dto("CLOSED", 6));

        ClaimDto closed = service.close(TENANT, CLAIM_ID, 5);

        assertEquals("CLOSED", closed.status());
        verify(client).close(TENANT, CLAIM_ID, 5);
    }

    @Test
    void assignmentDelegatesToTheExpertWithTheActor() {
        ClaimCommands.Assignment command = new ClaimCommands.Assignment(
                "adjuster@example.com", "CASUALTY", 3);
        ClaimDto assigned = assignedDto("REGISTERED", 4);
        when(client.assign(TENANT, CLAIM_ID, command, "supervisor@example.com")).thenReturn(assigned);

        ClaimDto result = service.assign(TENANT, CLAIM_ID, command, "supervisor@example.com");

        assertSame(assigned, result);
        assertEquals("adjuster@example.com", result.assignedAdjuster());
        assertEquals("CASUALTY", result.assignedTeam());
        verify(client).assign(TENANT, CLAIM_ID, command, "supervisor@example.com");
    }

    @Test
    void assignmentWithoutAnActorStillDelegates() {
        ClaimCommands.Assignment command = new ClaimCommands.Assignment(
                "adjuster@example.com", null, 0);
        when(client.assign(TENANT, CLAIM_ID, command, null)).thenReturn(assignedDto("REGISTERED", 1));

        service.assign(TENANT, CLAIM_ID, command, null);

        verify(client).assign(TENANT, CLAIM_ID, command, null);
    }

    private static CreateClaimCommand command(LocalDate reportDate, BigDecimal reserve) {
        return new CreateClaimCommand(POLICY_ID, CLAIMANT_ID, LOSS_DATE, reportDate,
                "GL", null, reserve, "USD", null, null);
    }

    private static ClaimDto dto(String status, int version) {
        return new ClaimDto(
                CLAIM_ID,
                TENANT,
                "DIRECT",
                "CLM-2026-0000001",
                POLICY_ID,
                CLAIMANT_ID,
                "BODILY_INJURY",
                status,
                LOSS_DATE,
                REPORT_DATE,
                "GL",
                new BigDecimal("125000.0000"),
                "USD",
                new BigDecimal("500000.0000"),
                "USD",
                false,
                null,
                null,
                null,
                new BigDecimal("125000.0000"),
                version,
                Instant.parse("2026-03-18T09:00:00Z"),
                Instant.parse("2026-03-18T09:00:00Z"));
    }

    private static ClaimDto assignedDto(String status, int version) {
        return new ClaimDto(
                CLAIM_ID,
                TENANT,
                "DIRECT",
                "CLM-2026-0000001",
                POLICY_ID,
                CLAIMANT_ID,
                "BODILY_INJURY",
                status,
                LOSS_DATE,
                REPORT_DATE,
                "GL",
                new BigDecimal("125000.0000"),
                "USD",
                new BigDecimal("500000.0000"),
                "USD",
                false,
                "adjuster@example.com",
                "CASUALTY",
                Instant.parse("2026-03-18T09:00:00Z"),
                new BigDecimal("125000.0000"),
                version,
                Instant.parse("2026-03-18T09:00:00Z"),
                Instant.parse("2026-03-18T09:00:00Z"));
    }
}
