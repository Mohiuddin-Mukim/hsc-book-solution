-- Phase 12: versioned offline manifest metadata and download contract.
ALTER TABLE offline_manifest
    ADD COLUMN IF NOT EXISTS language_code VARCHAR(10) NOT NULL DEFAULT 'bn',
    ADD COLUMN IF NOT EXISTS total_bytes BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS access_until TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS generated_at TIMESTAMPTZ NOT NULL DEFAULT now();

ALTER TABLE offline_manifest_item
    ADD COLUMN IF NOT EXISTS id UUID DEFAULT gen_random_uuid(),
    ADD COLUMN IF NOT EXISTS resource_version_label TEXT,
    ADD COLUMN IF NOT EXISTS download_url TEXT,
    ADD COLUMN IF NOT EXISTS byte_size BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS mime_type VARCHAR(120),
    ADD COLUMN IF NOT EXISTS relative_path VARCHAR(500) NOT NULL DEFAULT '',
    ADD COLUMN IF NOT EXISTS required BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS attachment_role VARCHAR(30),
    ADD COLUMN IF NOT EXISTS display_order INT,
    ADD COLUMN IF NOT EXISTS caption TEXT,
    ADD COLUMN IF NOT EXISTS alt_text VARCHAR(500);

CREATE INDEX IF NOT EXISTS ix_offline_manifest_scope_lang
    ON offline_manifest(scope_type, scope_id, language_code, version_no DESC);

CREATE INDEX IF NOT EXISTS ix_offline_manifest_item_manifest
    ON offline_manifest_item(manifest_id);

-- Older Phase 2 schema used resource_version BIGINT already; keep it as the
-- published question/solution version marker or media revision marker.

-- JPA uses a single UUID identifier for manifest items while Phase 2 used a
-- composite primary key. Preserve uniqueness but migrate the physical PK.
ALTER TABLE offline_manifest_item DROP CONSTRAINT IF EXISTS offline_manifest_item_pkey;
ALTER TABLE offline_manifest_item ALTER COLUMN id SET NOT NULL;
ALTER TABLE offline_manifest_item ADD CONSTRAINT pk_offline_manifest_item_id PRIMARY KEY (id);
CREATE UNIQUE INDEX IF NOT EXISTS uq_offline_manifest_item_resource
    ON offline_manifest_item(manifest_id, resource_type, resource_id);
