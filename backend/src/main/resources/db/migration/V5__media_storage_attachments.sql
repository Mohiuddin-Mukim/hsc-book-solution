ALTER TABLE media_asset
  ADD COLUMN IF NOT EXISTS lifecycle_state VARCHAR(20) NOT NULL DEFAULT 'QUARANTINED';

ALTER TABLE media_asset
  ADD COLUMN IF NOT EXISTS storage_backend VARCHAR(20) NOT NULL DEFAULT 'LOCAL';

ALTER TABLE media_asset
  ADD COLUMN IF NOT EXISTS original_extension VARCHAR(10);

ALTER TABLE media_asset
  ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT now();

ALTER TABLE media_asset
  DROP CONSTRAINT IF EXISTS media_asset_lifecycle_state_check;

ALTER TABLE media_asset
  ADD CONSTRAINT media_asset_lifecycle_state_check
  CHECK (lifecycle_state IN ('QUARANTINED','READY','REPLACED','ORPHANED','DELETED'));

ALTER TABLE media_asset
  DROP CONSTRAINT IF EXISTS media_asset_storage_backend_check;

ALTER TABLE media_asset
  ADD CONSTRAINT media_asset_storage_backend_check
  CHECK (storage_backend IN ('LOCAL','S3'));

ALTER TABLE media_asset
  DROP CONSTRAINT IF EXISTS media_asset_dimensions_check;

ALTER TABLE media_asset
  ADD CONSTRAINT media_asset_dimensions_check
  CHECK (
    width IS NULL OR (width > 0 AND width <= 10000)
  );

ALTER TABLE media_asset
  DROP CONSTRAINT IF EXISTS media_asset_height_check;

ALTER TABLE media_asset
  ADD CONSTRAINT media_asset_height_check
  CHECK (
    height IS NULL OR (height > 0 AND height <= 10000)
  );

CREATE INDEX IF NOT EXISTS ix_media_asset_state_created
  ON media_asset(lifecycle_state, created_at);

CREATE INDEX IF NOT EXISTS ix_q_media_asset
  ON question_media_attachment(media_asset_id);

CREATE INDEX IF NOT EXISTS ix_step_media_asset
  ON solution_step_media_attachment(media_asset_id);

CREATE OR REPLACE FUNCTION prevent_published_question_media_mutation()
RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE published_version UUID;
BEGIN
  IF TG_OP = 'INSERT' OR TG_OP = 'UPDATE' THEN
    SELECT q.published_version_id INTO published_version
    FROM question_version qv
    JOIN question q ON q.id = qv.question_id
    WHERE qv.id = NEW.question_version_id;
    IF published_version = NEW.question_version_id THEN
      RAISE EXCEPTION 'Published question media attachment is immutable' USING ERRCODE='23514';
    END IF;
  ELSE
    SELECT q.published_version_id INTO published_version
    FROM question_version qv
    JOIN question q ON q.id = qv.question_id
    WHERE qv.id = OLD.question_version_id;
    IF published_version = OLD.question_version_id THEN
      RAISE EXCEPTION 'Published question media attachment is immutable' USING ERRCODE='23514';
    END IF;
  END IF;
  RETURN COALESCE(NEW, OLD);
END $$;

DROP TRIGGER IF EXISTS trg_immutable_published_question_media ON question_media_attachment;
CREATE TRIGGER trg_immutable_published_question_media
BEFORE INSERT OR UPDATE OR DELETE ON question_media_attachment
FOR EACH ROW EXECUTE FUNCTION prevent_published_question_media_mutation();

CREATE OR REPLACE FUNCTION prevent_published_solution_step_media_mutation()
RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE published_version UUID;
BEGIN
  IF TG_OP = 'INSERT' OR TG_OP = 'UPDATE' THEN
    SELECT s.published_version_id INTO published_version
    FROM solution_step ss
    JOIN solution_version sv ON sv.id = ss.solution_version_id
    JOIN solution s ON s.id = sv.solution_id
    WHERE ss.id = NEW.solution_step_id;
    IF published_version = (SELECT solution_version_id FROM solution_step WHERE id = NEW.solution_step_id) THEN
      RAISE EXCEPTION 'Published solution-step media attachment is immutable' USING ERRCODE='23514';
    END IF;
  ELSE
    SELECT s.published_version_id INTO published_version
    FROM solution_step ss
    JOIN solution_version sv ON sv.id = ss.solution_version_id
    JOIN solution s ON s.id = sv.solution_id
    WHERE ss.id = OLD.solution_step_id;
    IF published_version = (SELECT solution_version_id FROM solution_step WHERE id = OLD.solution_step_id) THEN
      RAISE EXCEPTION 'Published solution-step media attachment is immutable' USING ERRCODE='23514';
    END IF;
  END IF;
  RETURN COALESCE(NEW, OLD);
END $$;

DROP TRIGGER IF EXISTS trg_immutable_published_solution_step_media ON solution_step_media_attachment;
CREATE TRIGGER trg_immutable_published_solution_step_media
BEFORE INSERT OR UPDATE OR DELETE ON solution_step_media_attachment
FOR EACH ROW EXECUTE FUNCTION prevent_published_solution_step_media_mutation();

CREATE OR REPLACE FUNCTION prevent_published_media_metadata_mutation()
RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
  IF EXISTS (
    SELECT 1 FROM question_media_attachment a
    JOIN question_version qv ON qv.id = a.question_version_id
    JOIN question q ON q.id = qv.question_id
    WHERE a.media_asset_id = OLD.id AND q.published_version_id = qv.id
  ) OR EXISTS (
    SELECT 1 FROM solution_step_media_attachment a
    JOIN solution_step ss ON ss.id = a.solution_step_id
    JOIN solution_version sv ON sv.id = ss.solution_version_id
    JOIN solution s ON s.id = sv.solution_id
    WHERE a.media_asset_id = OLD.id AND s.published_version_id = sv.id
  ) THEN
    IF OLD.checksum_sha256 IS DISTINCT FROM NEW.checksum_sha256
       OR OLD.storage_key IS DISTINCT FROM NEW.storage_key
       OR OLD.byte_size IS DISTINCT FROM NEW.byte_size
       OR OLD.mime_type IS DISTINCT FROM NEW.mime_type
       OR OLD.width IS DISTINCT FROM NEW.width
       OR OLD.height IS DISTINCT FROM NEW.height
       OR OLD.lifecycle_state IS DISTINCT FROM NEW.lifecycle_state THEN
      RAISE EXCEPTION 'Published media asset is immutable' USING ERRCODE='23514';
    END IF;
  END IF;
  RETURN NEW;
END $$;

DROP TRIGGER IF EXISTS trg_immutable_published_media_metadata ON media_asset;
CREATE TRIGGER trg_immutable_published_media_metadata
BEFORE UPDATE ON media_asset
FOR EACH ROW EXECUTE FUNCTION prevent_published_media_metadata_mutation();
