# eia-direct-claims-domain

- GitHub organization: `eia-open-insurance`
- Repository URL: https://github.com/eia-open-insurance/eia-direct-claims-domain
- Type: Spring Boot service
- Deployable: Yes
- Owner: Direct Claims
- Runtime: Java 22 and Spring Boot
- Data store: None (owns no tables; calls the expert)
- Depends on: eia-direct-claims-expert, eia-contracts, eia-platform/libs

## Purpose

Direct Claims business rules, transaction catalog, orchestration, approvals, and APIs. Direct claims: FNOL, coverage, injury, reserves, litigation, settlement, and recovery.

## Responsibilities

- Keep this repository focused on one deployable component or one platform concern.
- Follow the repository standard in `../CONVENTIONS.md` and the specification in `../../SPEC.md`.
- Publish and consume contracts through `eia-contracts`.
- Maintain README, CODEOWNERS, SECURITY, CHANGELOG, tests, and local verification.
- Contain no business logic in shared libraries or front-end remotes.

## Local run

Prerequisites: JDK 22 and the expert service on port 8087.

```powershell
$env:JAVA_HOME='C:\Program Files\Eclipse Adoptium\jdk-22.0.2.9-hotspot'
.\gradlew build
.\gradlew bootRun
```

The service listens on **8088** and calls the expert at `eia.direct-claims.expert-url`
(default `http://localhost:8087`).

Public endpoints (all require `X-EIA-Tenant` and `X-EIA-Book: DIRECT`; any other or missing
book returns 404, never 403):

- `GET /api/v1/claims?status={status}`
- `GET /api/v1/claims/search?q={term}&limit=10`
- `GET /api/v1/claims/{id}`
- `POST /api/v1/claims`
- `PATCH /api/v1/claims/{id}/status`
- `POST /api/v1/claims/{id}/close`
- `PATCH /api/v1/claims/{claimId}/assignment`
- `GET|POST /api/v1/claims/{id}/injuries`, `PATCH /api/v1/claims/{id}/injuries/{injuryId}`
- `GET|POST /api/v1/claims/{id}/exposures`, `PATCH /api/v1/claims/{id}/exposures/{exposureId}`
- `GET|POST /api/v1/claims/{id}/reserves`
- `GET|POST /api/v1/claims/{id}/coverage`
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
