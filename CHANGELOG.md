# Changelog

All notable changes to this repository are documented here.

The format is based on Keep a Changelog and the project uses Semantic Versioning.

## [Unreleased]

### Added

- Repository baseline.
- Direct Claims domain slice: `/api/v1/claims` create/list/get and status transition APIs,
  `X-EIA-Tenant`/`X-EIA-Book: DIRECT` enforcement with 404 for other books, SPEC §11.4.6
  lifecycle, date/reserve invariants, RestClient expert integration, structured errors, and
  JUnit tests.
- Second Direct Claims slice: public routes under `/api/v1/claims/{id}/` for injuries,
  exposures, reserve history, coverage, litigation, settlements (propose/approve/execute),
  recoveries, liens, and claim closure. Domain-side validation covers required fields,
  money, exposure dates against the report date, coverage positions, trial dates, and lien
  resolutions; the expert remains authoritative for the invariants.
- Common search at `GET /api/v1/claims/search?q={term}&limit=10`: tenant- and DIRECT-book-scoped
  matching over `claimNumber`, `claimType`, `causeCode`, and `policyId`, latest-by-`updatedAt`
  results for a blank query, and a limit capped at 25.
- Claim tasks and notes: `GET|POST /api/v1/claims/{id}/tasks` with
  `PATCH .../tasks/{taskId}` for version-checked status changes, and
  `GET|POST /api/v1/claims/{id}/notes` for append-only notes. The domain forwards
  `X-EIA-Tenant` and the DIRECT book, validates contract-level fields, and preserves expert
  problem+json 400/404/409 responses, including the expert detail message.
- Optional `Idempotency-Key` forwarding on the task and note create routes. The domain passes
  the header to the expert and preserves `Idempotent-Replay` on the public response; expert
  409 `EIA-DIR-4091` / `IDEMPOTENCY_KEY_REUSED` problems keep their code and category.
- Claim assignment: `PATCH /api/v1/claims/{claimId}/assignment` forwards
  `{assignedAdjuster, assignedTeam, expectedVersion}` with the tenant, DIRECT book, and the
  optional `X-EIA-Actor` header. `ClaimDto` exposes `assignedAdjuster`, `assignedTeam`, and
  `assignedAt`; the expert's 400/404/409 problem+json responses are preserved, including a
  stale-version 409.
- Claim allocations: `GET /api/v1/claims/{claimId}/allocations` returns
  `{items, totalPct, totalAmount}` oldest first, and `POST` accepts
  `{policyId, policyYear, layer, allocationPct, allocatedAmount}` and returns 201. The public
  `Allocation` projection uses `allocationId`, omits `tenantId`, and serializes money and
  percentages as decimal strings. The domain validates the required fields, the policy year
  range, the 32-character layer, the (0, 1] percentage, and the non-negative amount, then
  forwards the tenant and DIRECT book to the expert; expert 400/404/409 problem+json
  responses keep their status and detail.
- Claim list claimant filter: `GET /api/v1/claims` accepts an optional `claimantPartyId`
  UUID alongside `status` and forwards both, with the tenant and DIRECT book, to the
  expert's internal claim list. MockRestServiceServer tests cover the query parameters and
  headers, and the public `ClaimDto` response shape is unchanged.
- Claim allocation policy-erosion orchestration: `POST /api/v1/claims/{claimId}/allocations`
  accepts an optional `limitId` and the public `Allocation` adds
  `limitId`/`erosionStatus`/`erosionMessage`/`erosionPostedAt`. After a create with a limit
  the domain posts the erosion fact to the Policy domain's public
  `POST /api/v1/policies/{policyId}/erosion` using the allocation id as `Idempotency-Key`,
  then records `POSTED` or `FAILED` (with the policy detail) on the expert allocation; a
  policy failure never fails the 201 create. `POST .../allocations/{allocationId}/erosion`
  retries the posting from the expert allocation list and returns the updated allocation,
  with a 404 for an unknown allocation. The Policy base URL is `eia.policy.base-url`
  (default `http://localhost:8084`, `EIA_POLICY_BASE_URL`), and tests cover the no-limit,
  posted, failed, retry, exact header/body/idempotency, and 404 paths.
- Claim allocation mass-tort coverage block: before the expert create the domain reads
  `GET /api/v1/policies/{policyId}/mass-tort-profile` with the tenant and DIRECT book from
  `eia.policy.base-url`. A `COMPLETE` profile with both block dates rejects an allocation
  whose `policyYear` falls outside the inclusive start/end years with 400
  `MASS_TORT_YEAR_OUTSIDE_BLOCK`, whose message names the year and block. `DRAFT`, 404,
  and 5xx/unavailable profiles allow the create. Tests cover inside/outside years, both
  boundary years, DRAFT, 404, 5xx, and the always-fetch behavior without a limit.
- Settlement payment link: `PATCH /api/v1/claims/{claimId}/settlements/{settlementId}/payment`
  takes `{paymentId}` and forwards it to the expert with `X-EIA-Tenant` and the DIRECT book;
  a null `paymentId` clears the link. The public `Settlement` projection adds `paymentId` and
  `paymentLinkedAt`; the expert owns the allowed status transitions and duplicate-link checks,
  and its 400/404/409 problem+json responses keep their status and detail.
  MockRestServiceServer tests cover the controller, client route/headers/body, DTO parsing,
  link, and unlink.
