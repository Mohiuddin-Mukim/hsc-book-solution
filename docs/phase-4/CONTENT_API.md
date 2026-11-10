# Phase 4 Content API

Base URL: `/api/v1`

## Public student contract
- `GET /catalog/questions?chapterId={uuid}&page=0&size=20` — published questions only. `size` is bounded to 100.
- `GET /catalog/questions/{id}?language=bn|en` — published question snapshot, MCQ options/explanations and published solution version with ordered steps.
- Detail responses expose `version.id` and `version.versionNo`; solution exposes `version.id`, `version.versionNo`, and ordered `steps[].stepNo`.
- Published detail responses use `ETag` based on the published question/version IDs. Public list/detail responses have a short 60-second cache; publication changes update the version ID, so a new representation gets a new ETag.

## Admin contract
Admin routes require Phase 3 permissions and bearer access token.
- `GET/POST/PUT /admin/catalog/subjects`
- `GET/POST /admin/catalog/papers`
- `GET/POST /admin/catalog/books`
- `POST /admin/catalog/books/{bookId}/editions`
- `GET/POST /admin/catalog/chapters`
- `GET/POST /admin/catalog/topics`
- `GET /admin/catalog/questions`
- `POST /admin/catalog/questions`
- `POST /admin/catalog/questions/{id}/versions`
- `POST /admin/catalog/questions/{id}/solution/versions`
- `POST /admin/catalog/questions/{id}/workflow`
- `POST /admin/catalog/questions/{id}/solution/workflow`

### Workflow
`DRAFT -> SUBMITTED -> APPROVED -> PUBLISHED -> ARCHIVED`

Review can send `SUBMITTED -> CHANGES_REQUESTED -> SUBMITTED`; approved content can return to `CHANGES_REQUESTED` before publishing. Published snapshots are retained through `published_version_id`; later edits create a new working version instead of mutating the published snapshot.

### Example: create question
```json
{
  "chapterId":"00000000-0000-0000-0000-000000000000",
  "questionType":"MCQ",
  "difficulty":"MEDIUM",
  "languageCode":"bn",
  "stem":"নিচের কোনটি... ?",
  "marks":1
}
```

### Example: publish
```json
{"targetStatus":"PUBLISHED","note":"Teacher review passed"}
```

## Versioning rule
A published version is immutable at the PostgreSQL trigger level. Never edit its stem, marks, metadata, solution steps or MCQ data. Create a new version for changes and move it through review again.

## Tags / MCQ
- `GET/POST /admin/catalog/tags`
- `POST /admin/catalog/tags/{tagId}/questions/{questionId}/versions/{versionId}`
- `POST /admin/catalog/questions/{id}/versions/{versionId}/mcq-options`
- `POST /admin/catalog/questions/{id}/versions/{versionId}/mcq-explanation`
