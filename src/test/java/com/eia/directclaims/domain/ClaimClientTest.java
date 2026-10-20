package com.eia.directclaims.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static com.eia.directclaims.domain.DomainFixtures.ALLOCATION_ID;
import static com.eia.directclaims.domain.DomainFixtures.CLAIM_ID;
import static com.eia.directclaims.domain.DomainFixtures.NOTE_ID;
import static com.eia.directclaims.domain.DomainFixtures.POLICY_ID;
import static com.eia.directclaims.domain.DomainFixtures.TASK_ID;
import static com.eia.directclaims.domain.DomainFixtures.TENANT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ClaimClientTest {
    private static final String EXPERT = "http://expert.test";
    private static final String TASK_URL = EXPERT + "/internal/v1/claims/" + CLAIM_ID + "/tasks";
    private static final String NOTE_URL = EXPERT + "/internal/v1/claims/" + CLAIM_ID + "/notes";
    private static final String ASSIGN_URL = EXPERT + "/internal/v1/claims/" + CLAIM_ID + "/assignment";
    private static final String ALLOCATION_URL =
            EXPERT + "/internal/v1/claims/" + CLAIM_ID + "/allocations";

    private static final String CLAIM_JSON = """
            {
              "id": "0192f3a4-2b7e-7d1f-a9c4-3e8b5f0c9d21",
              "tenantId": "11111111-1111-7111-8111-111111111111",
              "businessBook": "DIRECT",
              "claimNumber": "CLM-2026-0000001",
              "policyId": "22222222-2222-7222-8222-222222222222",
              "claimantPartyId": "33333333-3333-7333-8333-333333333333",
              "claimType": "BODILY_INJURY",
              "status": "REGISTERED",
              "lossDate": "2026-03-14",
              "reportDate": "2026-03-18",
              "causeCode": "GL",
              "reserveAmount": "125000.0000",
              "reserveCurrency": "USD",
              "settlementAuthorityAmount": "500000.0000",
              "settlementAuthorityCurrency": "USD",
              "litigationFlag": false,
              "assignedAdjuster": "adjuster@example.com",
              "assignedTeam": "CASUALTY",
              "assignedAt": "2026-03-18T09:00:00Z",
              "currentReserveAmount": "125000.0000",
              "version": 4,
              "createdAt": "2026-03-18T09:00:00Z",
              "updatedAt": "2026-03-18T10:00:00Z"
            }
            """;

    private static final String TASK_JSON = """
            {
              "id": "0192f3a4-2b7e-7d1f-a9c4-3e8b5f0c9d39",
              "tenantId": "11111111-1111-7111-8111-111111111111",
              "claimId": "0192f3a4-2b7e-7d1f-a9c4-3e8b5f0c9d21",
              "taskNumber": "TSK-2026-0000001",
              "title": "Obtain updated medical records",
              "description": "Provider records requested at FNOL",
              "dueDate": "2026-04-01",
              "priority": "HIGH",
              "status": "OPEN",
              "assignedTo": "A. Adjuster",
              "version": 3,
              "createdAt": "2026-03-18T09:00:00Z",
              "updatedAt": "2026-03-18T09:00:00Z",
              "completedAt": null
            }
            """;

    private static final String NOTE_JSON = """
            {
              "id": "0192f3a4-2b7e-7d1f-a9c4-3e8b5f0c9d3a",
              "tenantId": "11111111-1111-7111-8111-111111111111",
              "claimId": "0192f3a4-2b7e-7d1f-a9c4-3e8b5f0c9d21",
              "noteNumber": "NTE-2026-0000001",
              "noteType": "DIARY",
              "body": "Called the claimant to confirm the medical authorization",
              "author": "Dana Handler",
              "createdAt": "2026-03-18T09:00:00Z"
            }
            """;

    private static final String ALLOCATION_JSON = """
            {
              "id": "0192f3a4-2b7e-7d1f-a9c4-3e8b5f0c9d3b",
              "tenantId": "11111111-1111-7111-8111-111111111111",
              "claimId": "0192f3a4-2b7e-7d1f-a9c4-3e8b5f0c9d21",
              "policyId": "22222222-2222-7222-8222-222222222222",
              "policyYear": 2024,
              "layer": "PRIMARY",
              "allocationPct": "0.250000",
              "allocatedAmount": "31250.0000",
              "createdAt": "2026-03-18T09:00:00Z"
            }
            """;

    private static final String ALLOCATION_SUMMARY_JSON = """
            {
              "items": [%s],
              "totalPct": "0.250000",
              "totalAmount": "31250.0000"
            }
            """.formatted(ALLOCATION_JSON);

    private MockRestServiceServer server;
    private ClaimClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder()
                .messageConverters(converters -> {
                    ObjectMapper mapper = Jackson2ObjectMapperBuilder.json()
                            .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                            .build();
                    converters.removeIf(MappingJackson2HttpMessageConverter.class::isInstance);
                    converters.add(0, new MappingJackson2HttpMessageConverter(mapper));
                });
        server = MockRestServiceServer.bindTo(builder).build();
        client = new ClaimClient(builder, EXPERT);
    }

    @Test
    void findTasksSendsTenantAndBookHeadersAndParsesTheList() {
        server.expect(requestTo(TASK_URL))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-EIA-Tenant", TENANT.toString()))
                .andExpect(header("X-EIA-Book", "DIRECT"))
                .andRespond(withSuccess("[" + TASK_JSON + "]", MediaType.APPLICATION_JSON));

        List<ClaimDetailDtos.Task> tasks = client.findTasks(TENANT, CLAIM_ID);

        assertEquals(1, tasks.size());
        ClaimDetailDtos.Task task = tasks.getFirst();
        assertEquals(TASK_ID, task.id());
        assertEquals(CLAIM_ID, task.claimId());
        assertEquals("TSK-2026-0000001", task.taskNumber());
        assertEquals("HIGH", task.priority());
        assertEquals("OPEN", task.status());
        assertEquals("A. Adjuster", task.assignedTo());
        assertEquals(3L, task.version());
        assertNull(task.completedAt());
        server.verify();
    }

    @Test
    void addTaskPostsThePublicPayloadWithHeaders() {
        server.expect(requestTo(TASK_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-EIA-Tenant", TENANT.toString()))
                .andExpect(header("X-EIA-Book", "DIRECT"))
                .andExpect(content().json("""
                        {
                          "title": "Obtain updated medical records",
                          "description": "Provider records requested at FNOL",
                          "dueDate": "2026-04-01",
                          "priority": "HIGH",
                          "assignedTo": "A. Adjuster"
                        }
                        """, JsonCompareMode.STRICT))
                .andRespond(withStatus(HttpStatus.CREATED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(TASK_JSON));

        ClaimDetailDtos.Task task = client.addTask(TENANT, CLAIM_ID, new ClaimCommands.Task(
                "Obtain updated medical records", "Provider records requested at FNOL",
                LocalDate.of(2026, 4, 1), "HIGH", "A. Adjuster"));

        assertEquals(TASK_ID, task.id());
        assertEquals("OPEN", task.status());
        assertEquals(3L, task.version());
        server.verify();
    }

    @Test
    void addTaskForwardsTheIdempotencyKeyAndReadsTheReplayHeader() {
        server.expect(requestTo(TASK_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Idempotency-Key", "task-key-1"))
                .andRespond(withStatus(HttpStatus.CREATED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotent-Replay", "true")
                        .body(TASK_JSON));

        ClaimCommandResult<ClaimDetailDtos.Task> result = client.addTask(TENANT, CLAIM_ID,
                new ClaimCommands.Task("Obtain updated medical records",
                        "Provider records requested at FNOL", LocalDate.of(2026, 4, 1), "HIGH",
                        "A. Adjuster"), "task-key-1");

        assertEquals(TASK_ID, result.value().id());
        assertEquals("true", result.replayHeader());
        server.verify();
    }

    @Test
    void changeTaskStatusPatchesStatusAndVersion() {
        server.expect(requestTo(TASK_URL + "/" + TASK_ID))
                .andExpect(method(HttpMethod.PATCH))
                .andExpect(header("X-EIA-Tenant", TENANT.toString()))
                .andExpect(header("X-EIA-Book", "DIRECT"))
                .andExpect(content().json("""
                        {"status": "COMPLETE", "version": 3}
                        """, JsonCompareMode.STRICT))
                .andRespond(withSuccess(TASK_JSON
                        .replace("\"status\": \"OPEN\"", "\"status\": \"COMPLETE\"")
                        .replace("\"completedAt\": null",
                                "\"completedAt\": \"2026-03-18T10:00:00Z\""),
                        MediaType.APPLICATION_JSON));

        ClaimDetailDtos.Task task = client.changeTaskStatus(TENANT, CLAIM_ID, TASK_ID,
                new ClaimCommands.TaskStatusChange("COMPLETE", 3L));

        assertEquals("COMPLETE", task.status());
        assertNotNull(task.completedAt());
        server.verify();
    }

    @Test
    void findNotesSendsHeadersAndParsesTheList() {
        server.expect(requestTo(NOTE_URL))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-EIA-Tenant", TENANT.toString()))
                .andExpect(header("X-EIA-Book", "DIRECT"))
                .andRespond(withSuccess("[" + NOTE_JSON + "]", MediaType.APPLICATION_JSON));

        List<ClaimDetailDtos.Note> notes = client.findNotes(TENANT, CLAIM_ID);

        assertEquals(1, notes.size());
        assertEquals(NOTE_ID, notes.getFirst().id());
        assertEquals("NTE-2026-0000001", notes.getFirst().noteNumber());
        assertEquals("DIARY", notes.getFirst().noteType());
        assertEquals("Dana Handler", notes.getFirst().author());
        server.verify();
    }

    @Test
    void addNotePostsThePublicPayloadWithHeaders() {
        server.expect(requestTo(NOTE_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-EIA-Tenant", TENANT.toString()))
                .andExpect(header("X-EIA-Book", "DIRECT"))
                .andExpect(content().json("""
                        {
                          "noteType": "DIARY",
                          "body": "Called the claimant to confirm the medical authorization",
                          "author": "Dana Handler"
                        }
                        """, JsonCompareMode.STRICT))
                .andRespond(withStatus(HttpStatus.CREATED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(NOTE_JSON));

        ClaimDetailDtos.Note note = client.addNote(TENANT, CLAIM_ID, new ClaimCommands.Note(
                "DIARY", "Called the claimant to confirm the medical authorization",
                "Dana Handler"));

        assertEquals(NOTE_ID, note.id());
        assertEquals("DIARY", note.noteType());
        server.verify();
    }

    @Test
    void addNoteForwardsTheIdempotencyKeyAndReadsTheReplayHeader() {
        server.expect(requestTo(NOTE_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Idempotency-Key", "note-key-1"))
                .andRespond(withStatus(HttpStatus.CREATED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotent-Replay", "false")
                        .body(NOTE_JSON));

        ClaimCommandResult<ClaimDetailDtos.Note> result = client.addNote(TENANT, CLAIM_ID,
                new ClaimCommands.Note("DIARY",
                        "Called the claimant to confirm the medical authorization", "Dana Handler"),
                "note-key-1");

        assertEquals(NOTE_ID, result.value().id());
        assertEquals("false", result.replayHeader());
        server.verify();
    }

    @Test
    void assignPatchesThePayloadAndForwardsTheActor() {
        server.expect(requestTo(ASSIGN_URL))
                .andExpect(method(HttpMethod.PATCH))
                .andExpect(header("X-EIA-Tenant", TENANT.toString()))
                .andExpect(header("X-EIA-Book", "DIRECT"))
                .andExpect(header("X-EIA-Actor", "supervisor@example.com"))
                .andExpect(content().json("""
                        {
                          "assignedAdjuster": "adjuster@example.com",
                          "assignedTeam": "CASUALTY",
                          "expectedVersion": 3
                        }
                        """, JsonCompareMode.STRICT))
                .andRespond(withSuccess(CLAIM_JSON, MediaType.APPLICATION_JSON));

        ClaimDto claim = client.assign(TENANT, CLAIM_ID, new ClaimCommands.Assignment(
                "adjuster@example.com", "CASUALTY", 3), "supervisor@example.com");

        assertEquals(CLAIM_ID, claim.id());
        assertEquals("adjuster@example.com", claim.assignedAdjuster());
        assertEquals("CASUALTY", claim.assignedTeam());
        assertEquals("2026-03-18T09:00:00Z", claim.assignedAt().toString());
        assertEquals(4, claim.version());
        server.verify();
    }

    @Test
    void assignOmitsTheActorHeaderWhenNoneIsSupplied() {
        server.expect(requestTo(ASSIGN_URL))
                .andExpect(method(HttpMethod.PATCH))
                .andExpect(request -> assertNull(request.getHeaders().getFirst("X-EIA-Actor")))
                .andRespond(withSuccess(CLAIM_JSON, MediaType.APPLICATION_JSON));

        ClaimDto claim = client.assign(TENANT, CLAIM_ID, new ClaimCommands.Assignment(
                "adjuster@example.com", null, 3), null);

        assertEquals("adjuster@example.com", claim.assignedAdjuster());
        server.verify();
    }

    @Test
    void propagatesAnExpertConflictForAssignment() {
        server.expect(requestTo(ASSIGN_URL))
                .andRespond(withStatus(HttpStatus.CONFLICT)
                        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                        .body("""
                                {
                                  "type": "about:blank",
                                  "title": "Conflict",
                                  "status": 409,
                                  "detail": "Claim version 2 does not match current version 3",
                                  "code": "CONFLICT"
                                }
                                """));

        RestClientResponseException error = assertThrows(RestClientResponseException.class,
                () -> client.assign(TENANT, CLAIM_ID, new ClaimCommands.Assignment(
                        "adjuster@example.com", "CASUALTY", 2), null));

        assertEquals(409, error.getStatusCode().value());
        assertTrue(error.getResponseBodyAsString().contains("does not match current version 3"));
        server.verify();
    }

    @Test
    void propagatesAnExpertValidationProblemForAssignment() {
        server.expect(requestTo(ASSIGN_URL))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                        .body("""
                                {"title": "Validation failed", "status": 400,
                                 "detail": "assignedAdjuster must be at most 128 characters",
                                 "code": "VALIDATION_ERROR"}
                                """));

        RestClientResponseException error = assertThrows(RestClientResponseException.class,
                () -> client.assign(TENANT, CLAIM_ID, new ClaimCommands.Assignment(
                        "adjuster@example.com", null, 3), null));

        assertEquals(400, error.getStatusCode().value());
        assertTrue(error.getResponseBodyAsString().contains("at most 128 characters"));
        server.verify();
    }

    @Test
    void propagatesAnExpertConflictProblemWithStatusAndBody() {
        server.expect(requestTo(TASK_URL + "/" + TASK_ID))
                .andRespond(withStatus(HttpStatus.CONFLICT)
                        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                        .body("""
                                {
                                  "type": "https://errors.eia.example/eia-rns-7006",
                                  "title": "Conflict",
                                  "status": 409,
                                  "detail": "Claim task version 2 does not match current version 3",
                                  "instance": "/internal/v1/claims/0192f3a4-2b7e-7d1f-a9c4-3e8b5f0c9d21/tasks/0192f3a4-2b7e-7d1f-a9c4-3e8b5f0c9d39",
                                  "code": "CONFLICT"
                                }
                                """));

        RestClientResponseException error = assertThrows(RestClientResponseException.class,
                () -> client.changeTaskStatus(TENANT, CLAIM_ID, TASK_ID,
                        new ClaimCommands.TaskStatusChange("COMPLETE", 2L)));

        assertEquals(409, error.getStatusCode().value());
        assertTrue(error.getResponseBodyAsString().contains("\"code\": \"CONFLICT\""));
        assertTrue(error.getResponseBodyAsString().contains("does not match current version 3"));
        server.verify();
    }

    @Test
    void propagatesAnExpertNotFoundProblemForTasks() {
        server.expect(requestTo(TASK_URL))
                .andRespond(withStatus(HttpStatus.NOT_FOUND)
                        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                        .body("""
                                {"title": "Not found", "status": 404, "detail": "Claim not found",
                                 "code": "NOT_FOUND"}
                                """));

        RestClientResponseException error = assertThrows(RestClientResponseException.class,
                () -> client.findTasks(TENANT, CLAIM_ID));

        assertEquals(404, error.getStatusCode().value());
        assertTrue(error.getResponseBodyAsString().contains("Claim not found"));
        server.verify();
    }

    @Test
    void propagatesAnExpertBadRequestProblemForNotes() {
        server.expect(requestTo(NOTE_URL))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                        .body("""
                                {"title": "Validation failed", "status": 400,
                                 "detail": "noteType is required", "code": "VALIDATION_ERROR"}
                                """));

        RestClientResponseException error = assertThrows(RestClientResponseException.class,
                () -> client.addNote(TENANT, CLAIM_ID,
                        new ClaimCommands.Note("GENERAL", "body", "author")));

        assertEquals(400, error.getStatusCode().value());
        assertTrue(error.getResponseBodyAsString().contains("noteType is required"));
        server.verify();
    }

    // ------------------------------------------------------------------ allocations
    @Test
    void findAllocationsSendsHeadersAndMapsTheSummary() {
        server.expect(requestTo(ALLOCATION_URL + "?tenantId=" + TENANT))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-EIA-Tenant", TENANT.toString()))
                .andExpect(header("X-EIA-Book", "DIRECT"))
                .andRespond(withSuccess(ALLOCATION_SUMMARY_JSON, MediaType.APPLICATION_JSON));

        ClaimDetailDtos.AllocationSummary summary = client.findAllocations(TENANT, CLAIM_ID);

        assertEquals(1, summary.items().size());
        ClaimDetailDtos.Allocation allocation = summary.items().getFirst();
        assertEquals(ALLOCATION_ID, allocation.allocationId());
        assertEquals(CLAIM_ID, allocation.claimId());
        assertEquals(POLICY_ID, allocation.policyId());
        assertEquals(2024, allocation.policyYear());
        assertEquals("PRIMARY", allocation.layer());
        assertEquals(0, new BigDecimal("0.250000").compareTo(allocation.allocationPct()));
        assertEquals(0, new BigDecimal("31250.0000").compareTo(allocation.allocatedAmount()));
        assertEquals("2026-03-18T09:00:00Z", allocation.createdAt().toString());
        assertEquals(0, new BigDecimal("0.250000").compareTo(summary.totalPct()));
        assertEquals(0, new BigDecimal("31250.0000").compareTo(summary.totalAmount()));
        server.verify();
    }

    @Test
    void createAllocationPostsThePublicPayloadWithHeaders() {
        server.expect(requestTo(ALLOCATION_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-EIA-Tenant", TENANT.toString()))
                .andExpect(header("X-EIA-Book", "DIRECT"))
                .andExpect(content().json("""
                        {
                          "policyId": "22222222-2222-7222-8222-222222222222",
                          "policyYear": 2024,
                          "layer": "PRIMARY",
                          "allocationPct": 0.25,
                          "allocatedAmount": 31250.0000
                        }
                        """, JsonCompareMode.STRICT))
                .andRespond(withStatus(HttpStatus.CREATED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(ALLOCATION_JSON));

        ClaimDetailDtos.Allocation allocation = client.createAllocation(TENANT, CLAIM_ID,
                new ClaimCommands.Allocation(POLICY_ID, 2024, "PRIMARY",
                        new BigDecimal("0.25"), new BigDecimal("31250.0000")));

        assertEquals(ALLOCATION_ID, allocation.allocationId());
        assertEquals(CLAIM_ID, allocation.claimId());
        assertEquals(POLICY_ID, allocation.policyId());
        assertEquals(2024, allocation.policyYear());
        assertEquals("PRIMARY", allocation.layer());
        server.verify();
    }

    @Test
    void propagatesAnExpertConflictForAllocations() {
        server.expect(requestTo(ALLOCATION_URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.CONFLICT)
                        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                        .body("""
                                {
                                  "title": "Conflict",
                                  "status": 409,
                                  "detail": "Allocations for claim CLM-2026-0000001 would total 1.100000, which exceeds 1",
                                  "code": "ALLOCATION_EXCEEDS_100"
                                }
                                """));

        RestClientResponseException error = assertThrows(RestClientResponseException.class,
                () -> client.createAllocation(TENANT, CLAIM_ID,
                        new ClaimCommands.Allocation(POLICY_ID, 2024, "PRIMARY",
                                new BigDecimal("0.85"), new BigDecimal("106250.0000"))));

        assertEquals(409, error.getStatusCode().value());
        assertTrue(error.getResponseBodyAsString().contains("ALLOCATION_EXCEEDS_100"));
        server.verify();
    }

    @Test
    void propagatesAnExpertBadRequestForAllocations() {
        server.expect(requestTo(ALLOCATION_URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                        .body("""
                                {
                                  "title": "Policy mismatch",
                                  "status": 400,
                                  "detail": "policyId does not match the claim policy",
                                  "code": "POLICY_MISMATCH"
                                }
                                """));

        RestClientResponseException error = assertThrows(RestClientResponseException.class,
                () -> client.createAllocation(TENANT, CLAIM_ID,
                        new ClaimCommands.Allocation(DomainFixtures.OTHER_ID, 2024, "PRIMARY",
                                new BigDecimal("0.25"), new BigDecimal("31250.0000"))));

        assertEquals(400, error.getStatusCode().value());
        assertTrue(error.getResponseBodyAsString().contains("POLICY_MISMATCH"));
        server.verify();
    }

    @Test
    void propagatesAnExpertNotFoundProblemForAllocations() {
        server.expect(requestTo(ALLOCATION_URL + "?tenantId=" + TENANT))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.NOT_FOUND)
                        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                        .body("""
                                {"title": "Not found", "status": 404,
                                 "detail": "Claim not found", "code": "NOT_FOUND"}
                                """));

        RestClientResponseException error = assertThrows(RestClientResponseException.class,
                () -> client.findAllocations(TENANT, CLAIM_ID));

        assertEquals(404, error.getStatusCode().value());
        assertTrue(error.getResponseBodyAsString().contains("Claim not found"));
        server.verify();
    }
}
