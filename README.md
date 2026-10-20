# eia-direct-claims-domain

- GitHub organization: `eia-open-insurance`
- Repository URL: https://github.com/eia-open-insurance/eia-direct-claims-domain
- Type: Spring Boot service
- Deployable: Yes
- Owner: Direct Claims
- Runtime: Java 22 and Spring Boot
- Data store: None (owns no tables; calls the expert)
- Depends on: eia-direct-claims-expert, eia-policy-domain, eia-contracts, eia-platform/libs

## Purpose

Direct Claims business rules, transaction catalog, orchestration, approvals, and APIs. Direct claims: FNOL, coverage, injury, reserves, litigation, settlement, and recovery.

## Responsibilities

- Keep this repository focused on one deployable component or one platform concern.
- Follow the repository standard in `../CONVENTIONS.md` and the specification in `../../SPEC.md`.
- Publish and consume contracts through `eia-contracts`.
- Maintain README, CODEOWNERS, SECURITY, CHANGELOG, tests, and local verification.
- Contain no business logic in shared libraries or front-end remotes.

## Local run

Prerequisites: JDK 22, the expert service on port 8087, and the Policy domain on port 8084
for claim-allocation erosion postings.

```powershell
$env:JAVA_HOME='C:\Program Files\Eclipse Adoptium\jdk-22.0.2.9-hotspot'
.\gradlew build
.\gradlew bootRun
```

The service listens on **8088**, calls the expert at `eia.direct-claims.expert-url`
(default `http://localhost:8087`), and calls the Policy domain at `eia.policy.base-url`
(default `http://localhost:8084`, overridable with `EIA_POLICY_BASE_URL`).

Public endpoints (all require `X-EIA-Tenant` and `X-EIA-Book: DIRECT`; any other or missing
book returns 404, never 403):

- `GET /api/v1/claims?status={status}&claimantPartyId={uuid}`
- `GET /api/v1/claims/search?q={term}&limit=10`
- `GET /api/v1/claims/{id}`
- `POST /api/v1/claims`
- `PATCH /api/v1/claims/{id}/status`
- `POST /api/v1/claims/{id}/close`
- `PATCH /api/v1/claims/{claimId}/assignment`
- `GET|POST /api/v1/claims/{id}/injuries`, `PATCH /api/v1/claims/{id}/injuries/{injuryId}`
- `GET|POST /api/v1/claims/{id}/exposures`, `PATCH /api/v1/claims/{id}/exposures/{exposureId}`
- `GET|POST /api/v1/claims/{id}/reserves`
- `GET /api/v1/claims/{id}/coverage`, `POST /api/v1/claims/{id}/coverage`
- `GET|POST /api/v1/claims/{id}/allocations`, `POST .../allocations/{allocationId}/erosion`
- `GET|POST /api/v1/claims/{id}/litigation`, `PATCH .../litigation/{litigationId}`
- `GET|POST /api/v1/claims/{id}/settlements`, `POST .../settlements/{settlementId}/approve`,
  `POST .../settlements/{settlementId}/execute`
- `GET|POST /api/v1/claims/{id}/recoveries`, `PATCH .../recoveries/{recoveryId}`
- `GET|POST /api/v1/claims/{id}/liens`, `PATCH .../liens/{lienId}`
- `GET|POST /api/v1/claims/{id}/tasks`, `PATCH .../tasks/{taskId}`
- `GET|POST /api/v1/claims/{id}/notes`

`GET /{id}/coverage` returns 404 until a decision is recorded, so the workbench can show an
empty tab. Money is a decimal string on every route. Task writes carry the task `version`
observed by the caller and return 409 when it is stale; notes are append-only. Expert
problems keep their 400/404/409 status and detail, rendered as `application/problem+json`.
The task and note create routes also accept an optional `Idempotency-Key`, forward it to the
expert, and preserve the expert's `Idempotent-Replay` response header. A same-body replay is
201 with `Idempotent-Replay: true`; a different body under the same key is 409 with code
`EIA-DIR-4091` and category `IDEMPOTENCY_KEY_REUSED`.

`PATCH /api/v1/claims/{claimId}/assignment` assigns or reassigns a claim to an adjuster and,
optionally, a team at the claim `version` the caller observed. The domain forwards the tenant,
the DIRECT book, the optional `X-EIA-Actor` header, and the exact
`{assignedAdjuster, assignedTeam, expectedVersion}` payload to the expert; a stale version is
preserved as the expert's 409 problem+json. Claim responses expose `assignedAdjuster`,
`assignedTeam`, and `assignedAt`.

`GET /api/v1/claims/{claimId}/allocations` returns the policy-year and layer split of a claim
as `{items, totalPct, totalAmount}` oldest first. `POST` takes
`{policyId, policyYear, layer, allocationPct, allocatedAmount, limitId?}` and returns 201 with
the created allocation. The domain requires the policy, year, layer, percentage, and amount;
`allocationPct` must be greater than 0 and at most 1, `allocatedAmount` must be zero or
greater, `layer` is at most 32 characters, and `policyYear` is 1900-2200. The expert remains
authoritative for the claim's own policy, the 1.0 percentage total across the claim, and
every other invariant; expert 400/404/409 problem+json responses keep their status and
detail. Allocation projections use `allocationId` and omit the expert's `tenantId`; money
and percentages are decimal strings.

Allocations expose `limitId`, `erosionStatus` (`NOT_POSTED`, `POSTED`, `FAILED`),
`erosionMessage`, and `erosionPostedAt`. When a create carries a `limitId`, the domain posts
an erosion fact to the Policy domain's `POST /api/v1/policies/{policyId}/erosion` with
`X-EIA-Tenant`, `X-EIA-Book: DIRECT`, and the allocation id as `Idempotency-Key`
(`{limitId, claimId, allocationId, sourceVersion: 1, paidAmount: 0,
reservedAmount: allocatedAmount, recoveredAmount: 0, asOf}`), then records `POSTED` or
`FAILED` (with the policy rejection detail) on the allocation. A policy failure never fails
the 201 create. `POST /api/v1/claims/{claimId}/allocations/{allocationId}/erosion` reloads
the allocation from the expert list, posts the same fact again, and returns the allocation
with its updated erosion state; an unknown allocation is a 404.

`GET /api/v1/claims/search` matches `claimNumber`, `claimType`, `causeCode`, and `policyId`
case-insensitively for the caller's tenant and the DIRECT book. A blank `q` returns the latest
records by `updatedAt`; `limit` defaults to 10 and is capped at 25. The domain owns no tables, so
the projection is built from the existing tenant-scoped expert list.

## Verification

- `.\gradlew build` - domain unit and controller tests.
- The expert's `scripts/live-smoke-domain.ps1` exercises this API end to end against real
  PostgreSQL with both services running.

## Notes

Owns the business logic for this vertical.

## License

Apache License 2.0. See [LICENSE](LICENSE) and [NOTICE](NOTICE).
