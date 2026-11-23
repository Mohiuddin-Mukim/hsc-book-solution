# Phase 11 reviewed search corpus

Record actual published questions before release. Keep expected results manually reviewed.

| Query | Language | Expected behavior |
|---|---|---|
| `গতি` | bn | Finds Bangla questions containing/relevant to the term |
| `বল` | bn | Finds physics questions using the Bangla term |
| `Newton` | en | Finds English questions containing Newton |
| `নিউটন` | bn | Finds Bangla spelling |
| `Newton বল` | mixed | Finds questions matching both/related terms |
| exact external question code | mixed | Exact code ranks first |
| exact UUID | neutral | Exact question ranks first |
| `superscript` | en | Finds relevant English content |
| `x²` | mixed/math | Does not corrupt the query; exact/term behavior is observed |
| punctuation-heavy question code | neutral | Exact code lookup remains reliable |

Before changing the index strategy, capture:
- result relevance review,
- p50/p95 latency,
- `EXPLAIN (ANALYZE, BUFFERS)` output,
- dataset size,
- page size.
