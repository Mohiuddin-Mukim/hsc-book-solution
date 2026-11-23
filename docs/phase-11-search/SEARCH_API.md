# Phase 11 — Search API

## Student
`GET /api/v1/catalog/search`

Query parameters:
- `q` required, 1–160 chars after trimming
- `subjectId`, `paperId`, `chapterId` optional UUID filters
- `language=bn|en`
- `type=MCQ|CQ|SHORT|NUMERICAL`
- `difficulty=EASY|MEDIUM|HARD`
- `page` default 0
- `size` default 20, server-bounded to 50

Only published questions and their `published_version_id` are searchable.

## Admin
`GET /api/v1/admin/catalog/search`

Requires `PERM_CONTENT_READ`. It searches the current version so authorized staff can find drafts as well as published content.

## Ranking
1. Exact `external_id` match or exact question UUID: score 1000.
2. Otherwise PostgreSQL `ts_rank_cd` over the selected question-version stem.
3. Stable `id ASC` tie-breaker.

## Unicode
The PostgreSQL `simple` text-search configuration is used. It avoids language-specific stemming and preserves Bangla/English tokens more predictably for an educational corpus containing math symbols and question codes.

The API does not lowercase or transliterate the query. Only surrounding whitespace is normalized. Exact external IDs are compared case-insensitively.

## Fuzzy search
`pg_trgm` is intentionally not enabled in Phase 11. Add it only after the reviewed corpus shows a measurable typo/fuzzy-search gap and query-plan/latency measurements justify the extra index.

## Radix tree
No radix tree is used. PostgreSQL already handles the required bounded indexed search; prefix/radix optimization should only be introduced after benchmarks demonstrate a need.

## Query-plan checks
Use PostgreSQL `EXPLAIN (ANALYZE, BUFFERS)` against representative Bangla, English, mixed, and exact-ID queries. Confirm the GIN FTS index is used for term searches and the `lower(external_id)` index is used for exact code lookups.

Example:
```sql
EXPLAIN (ANALYZE, BUFFERS)
SELECT q.id
FROM question q
JOIN question_version qv ON qv.id = q.published_version_id
WHERE q.status = 'PUBLISHED'
  AND to_tsvector('simple', coalesce(qv.stem,'')) @@ websearch_to_tsquery('simple','গতি');
```
