# Phase 7 — AI-assisted bulk import and media-pack import

## Canonical formats
- JSON: `{ "schemaVersion": "1.0", "items": [...] }`
- CSV/XLSX: first row is canonical flat headers. Complex `tags`, `mcqOptions`, `solution`, and `media` columns contain JSON.
- ZIP: `content.json` plus image files. Media `path` values are relative to the ZIP root.

## API
- `POST /api/v1/admin/imports/json` — paste canonical JSON.
- `POST /api/v1/admin/imports/upload` — multipart JSON/CSV/XLSX/ZIP.
- `GET /api/v1/admin/imports/{jobId}` — staged preview and row/field errors.
- `POST /api/v1/admin/imports/{jobId}/commit?submitForReview=false` — create drafts or submit to review.
- `POST /api/v1/admin/imports/{jobId}/retry` — retry only failed rows.
- `GET /api/v1/admin/imports/{jobId}/errors.csv` — downloadable error report.

## Safety
- ZIP path traversal rejected.
- Entry count, per-entry and total uncompressed limits enforced.
- Images are passed through the existing Phase 5 signature/decoding/checksum validation.
- External IDs provide idempotent duplicate detection.
- Similarity is not used for automatic merging.
- Import never publishes automatically.

## Modes
- `CREATE_ONLY`: existing external ID is an error.
- `UPDATE_AS_NEW_VERSION`: existing external ID creates a new version.
- `SKIP_DUPLICATES`: existing external ID is skipped.

## Canonical example
See `backend/src/main/resources/import/hsc-academic-import.schema.json`.
