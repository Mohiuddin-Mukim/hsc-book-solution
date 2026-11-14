# Phase 5 — Media storage, upload, attachment and delivery

## What changed

- V5 migration extends the Phase-2 `media_asset`/attachment tables rather than creating duplicate media tables.
- Local development storage is the default: `./runtime/media`.
- Quarantine files are validated before they are copied into storage.
- Production-compatible S3 adapter is selected with `MEDIA_STORAGE_BACKEND=s3`.
- Accepted formats: JPEG, PNG, WebP.
- Validation checks extension, file signature, actual decoding, byte size and dimensions.
- Every accepted object gets a random object key and SHA-256 checksum.
- Question-version and solution-step attachments are ordered and role-labelled.
- Published question versions and published solution versions cannot have their media attachments or binary metadata silently mutated.
- Public delivery uses `/api/v1/media/{assetId}` and never returns storage credentials.
- Admin UI includes a reusable upload/progress/preview/reorder/remove component.
- Android includes a reusable `StudentMediaImageView` contract using the delivery route.

## Local settings

The defaults are suitable for local development. Do not commit production secrets.

Optional `.env` values:

MEDIA_STORAGE_BACKEND=local
MEDIA_LOCAL_ROOT=./runtime/media
MEDIA_QUARANTINE_ROOT=./runtime/media-quarantine
MEDIA_MAX_BYTES=10485760
MEDIA_MAX_WIDTH=4096
MEDIA_MAX_HEIGHT=4096

## S3-compatible production mode

Set:

MEDIA_STORAGE_BACKEND=s3
MEDIA_S3_BUCKET=your-bucket
MEDIA_S3_REGION=your-region
MEDIA_S3_ENDPOINT=https://your-s3-compatible-endpoint
MEDIA_S3_PATH_STYLE=false

The AWS SDK default credential provider chain is used. Credentials are not stored in PostgreSQL and are never returned by the API.

## Core API

POST `/api/v1/admin/media/upload` — multipart `file`, optional `caption`, `altText`.

POST `/api/v1/admin/question-versions/{versionId}/media` — JSON:
`{"assetId":"UUID","role":"GRAPH","displayOrder":0,"caption":"...","altText":"..."}`

PUT `/api/v1/admin/question-versions/{versionId}/media/reorder`

DELETE `/api/v1/admin/question-versions/{versionId}/media/{assetId}`

POST `/api/v1/admin/question-versions/{versionId}/media/{assetId}/replace` — multipart replacement.

POST `/api/v1/admin/solution-steps/{stepId}/media`

PUT `/api/v1/admin/solution-steps/{stepId}/media/reorder`

DELETE `/api/v1/admin/solution-steps/{stepId}/media/{assetId}`

GET `/api/v1/media/{assetId}` — authorized delivery route. Anonymous/student delivery is allowed only when the asset is referenced by a published question/solution version; admin delivery can preview ready assets. Storage credentials are never returned.

## Important test order

1. Run backend `mvn clean test`.
2. Start PostgreSQL.
3. Start backend and verify Flyway reports V1–V5.
4. Login as a user with `PERM_MEDIA_MANAGE`.
5. Upload valid JPEG/PNG/WebP.
6. Upload a fake extension/MIME file and confirm rejection.
7. Upload a corrupt image and confirm rejection.
8. Upload an oversized image and confirm 413/rejection.
9. Attach two images to one draft question version.
10. Reorder them and verify order in the question detail JSON.
11. Publish that question version.
12. Try to attach/detach/reorder against the published version; it must fail.
13. Open the delivery route.
14. Remove an unused asset and verify orphan cleanup can mark/delete it.
15. Verify the admin UI progress and preview.
16. Run GitHub Actions before starting Phase 6.

## Note

The Phase-4 schema already contained `media_asset`, `question_media_attachment` and `solution_step_media_attachment`. Phase 5 therefore extends those tables instead of introducing duplicate relationships.
