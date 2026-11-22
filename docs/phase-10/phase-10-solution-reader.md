# Phase 10 — Solution reader

## Reader contract
The Android reader consumes the existing published `GET /api/v1/catalog/questions/{id}?language=bn|en` contract. The backend response is version-bound: question content uses `publishedVersionId`; solution content uses `publishedVersionId`; question and solution media are returned ordered by `displayOrder`.

## LaTeX policy
The reader uses a restricted WebView only for text blocks that contain supported LaTeX delimiters. MathJax 3 is loaded from jsDelivr and renders TeX formulas. Server text is HTML-escaped before insertion; raw HTML and user navigation are not allowed. WebView file/content access and universal file access are disabled. External navigation is intercepted.

Supported delimiters:
- inline: `\(...\)`
- display: `$$...$$` and `\[...\]`

If MathJax cannot load, the text remains visible rather than crashing the reader. A future release can vendor the renderer into app assets for fully offline-first math.

## Images
Images use Glide with a 1600x1600 decode bound and disk/memory caching. `ZoomImageView` supports pinch zoom up to 4x and panning while zoomed. Captions and alt text are preserved. The API's attachment order is rendered in list order.

## Position restoration
Reader scroll position is stored per question in app-private SharedPreferences and restored after the reader content is loaded.

## Bookmarks
Student bookmarks use the existing Phase 2 `bookmark` table. Phase 10 adds authenticated student endpoints:
- `GET /api/v1/student/bookmarks/{questionId}`
- `PUT /api/v1/student/bookmarks/{questionId}`
- `DELETE /api/v1/student/bookmarks/{questionId}`
- `GET /api/v1/student/bookmarks`

Only published questions can be bookmarked.

## Theme
The existing Material `DayNight` theme is retained and Phase 10 adds night palette resources so the same reader works in both system/light and dark appearance.
