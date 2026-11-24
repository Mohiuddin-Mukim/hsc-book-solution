# Phase 12 — Offline packages and sync

## Backend

Authenticated student endpoints:

- `GET /api/v1/student/offline/manifests/chapters/{chapterId}?language=bn`
- `GET /api/v1/student/offline/manifests/books/{bookId}?language=bn`

A manifest contains published question JSON resources, every question-level and solution-step media asset, SHA-256 checksums, byte sizes, relative offline paths, attachment order/caption/alt text, and a required bundled MathJax renderer asset with its expected byte size and SHA-256 checksum.

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
- Question reader falls back to the exact downloaded question version in Room when network is unavailable.
- Home → Offline downloads opens a package manager screen with package title, scope, language, version, size, Open offline and Delete actions.
- Open offline loads the package's question list from Room; it does not request the subject/chapter/question catalog endpoints.
- Chapter and book screens check Room before showing Downloaded, preventing a second download for a completed package. Active downloads can be cancelled from the same button; failed/cancelled downloads can be retried, continuing from a retained `.part` file when the server supports HTTP Range.
- Question JSON is kept in Room; binary media remains in app-private `files/offline`.
- Local media `file://` URIs are passed to Glide without being rewritten as API URLs.
- Required resource checksums and byte sizes are verified, and reused media/question versions are preserved across retries and incremental updates.
- The Room schema is migrated to version 5; question content is keyed by question, language and content version so an update does not overwrite an older version before the new package is verified.
- The configured entitlement grace is added to the offline access cutoff. Configure it with `OFFLINE_DEFAULT_GRACE_MINUTES` (default 180); turn enforcement on with `OFFLINE_ENFORCE_ENTITLEMENT=true`.

## Important integrity rule

The UI must only show `Downloaded` after all required question JSON, media and the bundled math renderer have been verified. A failed checksum, incomplete response, corrupt/missing file, or insufficient storage causes the WorkManager job to fail.

## Test checklist

1. Generate a chapter manifest while logged in as STUDENT.
2. Download a chapter and wait for 100%.
3. Open a downloaded question in airplane mode.
4. Verify Bangla text, LaTeX, every figure, caption and order. Note: this archive currently bundles only `mathjax/tex-chtml.js`; it does not include MathJax's external CHTML WOFF font files. Formula output may fall back to system fonts, so full symbol/font parity is not yet verified.
5. Kill/reopen the app and open the same question offline.
6. Interrupt a download; reconnect and verify resume/verification.
7. Corrupt a media file and verify checksum failure.
8. Use low-storage conditions and verify a clear failure.
9. Change/publish a new question version; generate a new manifest and verify unchanged media is reused.
10. Enable entitlement enforcement in a controlled test database and verify expiry messaging.
11. Delete a package from Home → Offline downloads; verify it can be redownloaded.

## Offline package manager behavior

The Offline downloads screen is the supported offline entry point. Subject, paper, chapter catalog browsing, search and other not-downloaded pages still depend on the backend. A downloaded package is opened from the Offline downloads screen and its question list/detail is read from Room and app-private media.

A package is marked complete only after every required resource is present and checksum/size validation passes. Opening a package rechecks its local resources; missing/corrupt content produces a recoverable error rather than silently using the network.

## Remaining release gate

Bundle and verify the complete MathJax CHTML font asset set (or replace the renderer with a self-contained offline SVG bundle), then test formulas and uncommon symbols in airplane mode on a clean install. The build/test commands must also be run in an environment with the Gradle distribution and Maven available.
