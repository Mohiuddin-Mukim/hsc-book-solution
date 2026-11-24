# Phase 12 Offline Package Patch

This ZIP contains only files changed or added for the Phase 12 offline-package repair. Apply it over the matching `HSC_Academic_Solution_Phase_1` project root, preserving the directory structure.

## What this patch changes

- Adds a dedicated Home → Offline downloads screen with package title, scope, language, version, size, Open offline and Delete actions.
- Opens a downloaded chapter/book package directly from Room, including the question list and question detail, without calling the online subject/chapter/question-list endpoints.
- Persists downloaded question versions in Room using `(questionId, languageCode, versionId)` so a new content version cannot overwrite the old package's data during an interrupted update.
- Stores source checksums separately from the locally rewritten JSON checksum, preventing stale JSON reuse when media references change without a question version change.
- Keeps images/figures in app-private `files/offline`, rewrites the media references in Room JSON to verified local `file://` URIs, and passes those URIs to Glide correctly.
- Verifies byte size and SHA-256 for every required question JSON and media asset before marking a package complete. Opening a package revalidates local content and media.
- Preserves completed resources across interrupted retries, handles fully downloaded `.part` files, validates HTTP Range offsets, and reuses unchanged media across package versions.
- Cleans up old versions only after a new package has passed integrity verification.
- Fixes Room manifest duplicate-row behavior and adds migrations through database version 5.
- Adds the backend scope title to manifests and fixes the persisted manifest item's question-version label.
- Applies the configured entitlement grace window. `OFFLINE_DEFAULT_GRACE_MINUTES` defaults to 180; `OFFLINE_ENFORCE_ENTITLEMENT` defaults to false.

## Apply and run

1. Unzip this patch into the project root, allowing the included files to overwrite their matching files.
2. Rebuild and install the Android app. Room will migrate existing local offline metadata to schema version 5.
3. Rebuild/restart the Spring Boot backend so the updated manifest response and grace policy are active.
4. Sign in while online, download a chapter, wait until the button shows Downloaded.
5. Open Home → Offline downloads → Open offline.
6. Turn on Airplane mode and open a question and its solution. Verify every image and formula.
7. Return online, update/publish content, download again, and verify only changed question/media resources are transferred.

## Important verification limitation

The uploaded project contains only `android/app/src/main/assets/mathjax/tex-chtml.js`. It does not contain the MathJax CHTML WOFF font directory. The JavaScript renderer is local, but full offline font coverage (especially uncommon math symbols) cannot be certified from the supplied source. This patch does not fabricate or substitute font files. Bundle the matching MathJax font assets (or switch to a self-contained offline SVG renderer) and test formulas in Airplane mode before treating the Phase 12 exit criteria as fully passed.

## Build verification

The patch was statically reviewed and the changed XML files parse. A full Android build could not be run in this environment because the Gradle wrapper could not download Gradle (`services.gradle.org` was unreachable). Backend compilation could not be run because Maven is not installed in this environment. Run `bash android/gradlew :app:assembleDebug` and `mvn -f backend/pom.xml test` on the development machine after applying the patch.
