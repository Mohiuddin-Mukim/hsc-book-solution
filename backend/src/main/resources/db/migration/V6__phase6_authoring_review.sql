ALTER TABLE question_version
  ADD COLUMN IF NOT EXISTS lock_version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE solution_version
  ADD COLUMN IF NOT EXISTS lock_version BIGINT NOT NULL DEFAULT 0;

CREATE TABLE IF NOT EXISTS content_review_event (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  question_id UUID NOT NULL REFERENCES question(id) ON DELETE CASCADE,
  actor_user_id UUID REFERENCES app_user(id) ON DELETE SET NULL,
  from_status VARCHAR(30),
  to_status VARCHAR(30) NOT NULL,
  note TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS ix_content_review_event_question_created
  ON content_review_event(question_id, created_at DESC);

CREATE TABLE IF NOT EXISTS content_review_comment (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  question_id UUID NOT NULL REFERENCES question(id) ON DELETE CASCADE,
  version_id UUID REFERENCES question_version(id) ON DELETE SET NULL,
  actor_user_id UUID REFERENCES app_user(id) ON DELETE SET NULL,
  body TEXT NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS ix_content_review_comment_question_created
  ON content_review_comment(question_id, created_at ASC);

ALTER TABLE question_version
  ADD CONSTRAINT question_version_lock_version_nonnegative CHECK(lock_version >= 0);
ALTER TABLE solution_version
  ADD CONSTRAINT solution_version_lock_version_nonnegative CHECK(lock_version >= 0);
