# API conventions
Prefix: /api/v1. UUID IDs. UTC ISO-8601 timestamps. Pagination: page=0,size=20,max=100.
List shape: {items,page,size,totalItems,totalPages}. Filtering via allowlisted query params.
Every response carries X-Correlation-Id. Error envelope:
{code,message,correlationId,timestamp,details}. Never expose raw SQL/payment secrets.
Media bytes live in object storage; DB stores metadata/storage_key. Published versions are immutable;
new edits create a new version. Idempotent writes use Idempotency-Key.
