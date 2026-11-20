# Phase 8 + Phase 9 — Student registration and catalog integration

## What this patch adds

### Authentication
- `POST /api/v1/auth/register` public endpoint.
- Public registration can create **STUDENT only**; the client cannot select a role.
- BCrypt password hashing.
- Email uniqueness check.
- Preferred language `bn`/`en`.
- Registration audit event.
- `STUDENT` role migration.
- Android Create Account UI connected to the real API.
- Login UI polished with Material outlined fields and password visibility toggle.

### Student catalog
- Public paginated subject list.
- Subject → paper list.
- Subject → books list.
- Book → edition list.
- Paper → chapter list.
- Chapter → topic list.
- Chapter → published question list with type/difficulty/marks filters.
- Stable ordering.
- Public question listing uses `publishedVersionId`, not a newer draft `currentVersionId`.
- Public question detail no longer leaks an unpublished language version.

## Important data-model note

The current schema relates `chapter -> paper`, while `book/edition` is a separate subject-level catalog entity. This patch therefore exposes books/editions as subject-level catalog metadata but does not falsely pretend that a chapter belongs to a particular book edition.

## Registration test

1. Start PostgreSQL.
2. Start Spring Boot backend.
3. Let Flyway apply `V8__student_registration.sql`.
4. Android app → Create account.
5. Enter name, valid email, password >= 8 chars, confirmation, preferred language.
6. Submit.
7. Expected HTTP `201 Created`.
8. Expected response role: `STUDENT`.
9. Return to sign-in.
10. Sign in with the new credentials.
11. Expected: successful login and Home.

### Duplicate email
Register the same email again.
Expected HTTP `409` and a clear duplicate-account message.

### Security checks
- Never accept a `role` field from registration.
- Password is never stored as plaintext.
- Do not log password or tokens.
- Existing SUPER_ADMIN/ADMIN login behavior is unchanged: login does not require the STUDENT role.

## Catalog test

After at least one subject/paper/chapter/question has been published:

`GET /api/v1/catalog/subjects`

Then:

`GET /api/v1/catalog/subjects/{subjectId}/papers`

Then:

`GET /api/v1/catalog/papers/{paperId}/chapters`

Then:

`GET /api/v1/catalog/chapters/{chapterId}/questions`

Verify only published questions are returned.

For filters, try:
- `type`
- `difficulty`
- `minMarks`
- `maxMarks`
- `page`
- `size`

Expected ordering is deterministic.

## Android Phase 8 + 9 smoke flow

Create account → Sign in → Home → Subject → Paper → Chapter → Questions.

Also test:
- empty list
- API failure + retry
- offline state
- pagination
- filters
- back navigation
- rotation
- app restart/session restoration
- no credentials/tokens in Logcat

## Phase completion

Do not move to the next phase until registration/login and the subject → paper → chapter → question flow work with real backend data.
