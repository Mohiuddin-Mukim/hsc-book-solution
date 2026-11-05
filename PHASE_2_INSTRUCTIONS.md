# PHASE 2 IMPLEMENTATION + VERIFICATION

## What this patch adds
- PostgreSQL/Flyway domain schema covering identity/RBAC/devices, academic catalog,
  versioned questions/solutions/steps, MCQ/tags, media and ordered attachments,
  import/review/audit, plans/orders/payments/subscriptions/entitlements,
  bookmarks/history/offline manifests, analytics, idempotency and webhooks.
- Explicit FK-safe media attachment tables. Multiple images are supported per question
  version AND per individual solution step. Media bytes remain outside PostgreSQL.
- API contract primitives and documentation.
- Admin TypeScript contract types.
- Safe seed data only for languages, roles and initial Science subjects.

## Apply / verify
1. Extract the project.
2. Start PostgreSQL 17.x with the Phase 1 compose setup.
3. Start backend: `cd backend && mvn spring-boot:run`.
4. Confirm Flyway reports successful migration `V1__phase2_domain`.
5. Run: `mvn test`.
6. Execute every case in `docs/phase-2/MIGRATION_TEST_PLAN.md`.
7. Inspect generated OpenAPI at the configured springdoc URL once the app is running.

## API rules
Use `/api/v1`, UUIDs, UTC timestamps, page/size pagination, stable error envelope and
`X-Correlation-Id`. Keep media bytes in object storage and DB metadata only.

## Important migration rule
After a migration has been applied anywhere, do not edit it. Make a new forward migration
(e.g. `V2__...`) for corrections or schema changes.

## Exit gate
Do not move to Phase 3 until a fresh DB migrates successfully, all critical constraints
work, and both question-level and solution-step-level multiple media attachments are verified.
