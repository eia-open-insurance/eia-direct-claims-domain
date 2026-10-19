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
- `GET /api/v1/claims/{id}`
- `POST /api/v1/claims`
- `PATCH /api/v1/claims/{id}/status`

## Verification

- Lint and format
- Unit tests with 100% business-logic coverage where applicable
- Contract tests
- Integration or Playwright tests where applicable
- Security scan and SBOM
- Build and publish

## Notes

Owns the business logic for this vertical.

## License

Apache License 2.0. See [LICENSE](LICENSE) and [NOTICE](NOTICE).
