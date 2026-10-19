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
