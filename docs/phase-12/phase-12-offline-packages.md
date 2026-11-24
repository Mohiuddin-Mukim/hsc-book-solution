# Phase 12 — Offline packages and sync

## Backend

Authenticated student endpoints:

- `GET /api/v1/student/offline/manifests/chapters/{chapterId}?language=bn`
- `GET /api/v1/student/offline/manifests/books/{bookId}?language=bn`

A manifest contains published question JSON resources, every question-level and solution-step media asset, SHA-256 checksums, byte sizes, relative offline paths, attachment order/caption/alt text, and a required bundled MathJax renderer asset.

Only `STUDENT` role can generate manifests.

### Book-scope limitation

The current academic schema links chapters to papers and books to subjects, but does not directly link a chapter to a book edition. Until that relationship is introduced, a `BOOK` offline package means all published chapters under that book's subject. This avoids silently inventing textbook/chapter mappings.

### Entitlement policy

`offline.enforce-entitlement=false` is the development/default policy, so free/public content can be downloaded without subscription tables being fully modeled in this phase.

When enabled, the service checks `entitlement` + `subscription` for:
- matching `resource_type` + `resource_id`, or
- a global entitlement (`resource_id IS NULL`).

The manifest exposes `accessUntil`; Android refuses to open expired offline content and tells the user to reconnect.

## Android

- WorkManager package download with retry.
- HTTP Range resume using `.part` files when the server returns `206`.
- Atomic temp-file -> final-file rename.
- SHA-256 + exact byte-size validation.
- Room stores manifest/resource/content metadata.
- Media files live in app-private `files/offline`.
- Shared media is reused across packages when checksum matches.
- Bundled MathJax: `app/src/main/assets/mathjax/tex-chtml.js`; formulas do not require CDN access offline.
- Question reader falls back to Room when network is unavailable.
- Home → Offline downloads shows storage usage and allows deletion.
- Chapter and book screens provide offline download buttons.

## Important integrity rule

The UI must only show `Downloaded` after all required question JSON, media and the bundled math renderer have been verified. A failed checksum, incomplete response, corrupt/missing file, or insufficient storage causes the WorkManager job to fail.

## Test checklist

1. Generate a chapter manifest while logged in as STUDENT.
2. Download a chapter and wait for 100%.
3. Open a downloaded question in airplane mode.
4. Verify Bangla text, LaTeX, every figure, caption and order.
5. Kill/reopen the app and open the same question offline.
6. Interrupt a download; reconnect and verify resume/verification.
7. Corrupt a media file and verify checksum failure.
8. Use low-storage conditions and verify a clear failure.
9. Change/publish a new question version; generate a new manifest and verify unchanged media is reused.
10. Enable entitlement enforcement in a controlled test database and verify expiry messaging.
11. Delete a package from Home → Offline downloads; verify it can be redownloaded.
