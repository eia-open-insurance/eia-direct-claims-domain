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
