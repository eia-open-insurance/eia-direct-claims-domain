# eia-direct-domain

- GitHub organization: `eia-platform`
- Repository URL: https://github.com/eia-platform/eia-direct-domain
- Type: Spring Boot service
- Deployable: Yes
- Owner: Direct
- Runtime: Java 22 and Spring Boot
- Data store: eia_direct (tx schema)
- Depends on: eia-direct-expert, eia-contracts, eia-platform-libs

## Purpose

Direct business rules, transaction catalog, orchestration, approvals, and APIs. Direct claims: FNOL, coverage, injury, reserves, litigation, settlement, and recovery.

## Responsibilities

- Keep this repository focused on one deployable component or one platform concern.
- Follow the repository standard in `../CONVENTIONS.md` and the specification in `../../SPEC.md`.
- Publish and consume contracts through `eia-contracts`.
- Maintain README, CODEOWNERS, SECURITY, CHANGELOG, tests, and CI.
- Contain no business logic in shared libraries or front-end remotes.

## Required checks

- Lint and format
- Unit tests with 100% business-logic coverage where applicable
- Contract tests
- Integration or Playwright tests where applicable
- Security scan and SBOM
- Build and publish

## Notes

Owns the business logic for this vertical.

