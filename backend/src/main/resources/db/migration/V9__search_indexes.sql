-- Phase 11 search indexes.
CREATE INDEX IF NOT EXISTS ix_question_external_id_lower
    ON question (lower(external_id));
CREATE INDEX IF NOT EXISTS ix_question_version_stem_fts_simple
    ON question_version
    USING GIN (to_tsvector('simple', coalesce(stem, '')));
CREATE INDEX IF NOT EXISTS ix_question_version_language
    ON question_version (question_id, language_code, id);
