
ALTER TABLE import_job
  ADD COLUMN IF NOT EXISTS idempotency_key VARCHAR(160),
  ADD COLUMN IF NOT EXISTS mode VARCHAR(32) NOT NULL DEFAULT 'CREATE_ONLY',
  ADD COLUMN IF NOT EXISTS dry_run BOOLEAN NOT NULL DEFAULT FALSE,
  ADD COLUMN IF NOT EXISTS source_sha256 CHAR(64),
  ADD COLUMN IF NOT EXISTS staging_path TEXT,
  ADD COLUMN IF NOT EXISTS committed_at TIMESTAMPTZ;
CREATE UNIQUE INDEX IF NOT EXISTS uq_import_job_idempotency_key
  ON import_job(idempotency_key) WHERE idempotency_key IS NOT NULL;

ALTER TABLE import_row
  ADD COLUMN IF NOT EXISTS external_id VARCHAR(160),
  ADD COLUMN IF NOT EXISTS question_id UUID REFERENCES question(id) ON DELETE SET NULL;
CREATE INDEX IF NOT EXISTS ix_import_row_external_id ON import_row(external_id);

ALTER TABLE import_error
  ADD COLUMN IF NOT EXISTS severity VARCHAR(10) NOT NULL DEFAULT 'ERROR';
CREATE INDEX IF NOT EXISTS ix_import_error_row ON import_error(import_row_id);

ALTER TABLE question
  ADD COLUMN IF NOT EXISTS external_id VARCHAR(160);
CREATE UNIQUE INDEX IF NOT EXISTS uq_question_external_id
  ON question(external_id) WHERE external_id IS NOT NULL;
