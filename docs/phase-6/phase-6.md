# Phase 6 — Admin shell + authoring, review and publish

This patch extends the Phase 5 baseline. It does not recreate Phase 1–5 modules.

## Backend
- `V6__phase6_authoring_review.sql`
  - optimistic lock columns for question/solution versions
  - review events and reviewer comments
- `AuthoringService` + `AdminAuthoringController`
  - draft version editing with lock-version conflict protection
  - question/solution history and basic diff
  - comments
  - submit / request changes / approve / publish / unpublish
  - rollback creates a new draft version and copies MCQ/media references
  - publish checks referenced media is READY and object storage contains it
  - audit events
- Existing Phase 4/5 contracts are reused.

## Admin
- role-aware editorial shell/navigation
- content library
- question editor
- local draft autosave/recovery
- LaTeX delimiter warning + student-style preview
- MCQ options/explanation
- reusable tag creation/attachment
- question and exact solution-step media attachment
- review queue/history/comments/diff
- placeholders for imports, users/devices, payments, analytics and settings

## Main authoring endpoints
- `GET /api/v1/admin/authoring/questions/{questionId}`
- `PUT /api/v1/admin/authoring/questions/{questionId}/versions/{versionId}`
- `PUT /api/v1/admin/authoring/questions/{questionId}/solution/versions/{versionId}`
- `GET /api/v1/admin/authoring/questions/{questionId}/history`
- `GET /api/v1/admin/authoring/questions/{questionId}/diff`
- `POST /api/v1/admin/authoring/questions/{questionId}/comments`
- `POST /api/v1/admin/authoring/questions/{questionId}/submit`
- `POST /api/v1/admin/authoring/questions/{questionId}/review`
- `POST /api/v1/admin/authoring/questions/{questionId}/publish`
- `POST /api/v1/admin/authoring/questions/{questionId}/unpublish`
- `POST /api/v1/admin/authoring/questions/{questionId}/rollback`
- `GET /api/v1/admin/authoring/review-queue`

## Important
Run Flyway through normal backend startup. Do not manually run V6 twice.

The publish endpoint requires `PERM_CONTENT_PUBLISH`; content editors do not receive that permission by default.
