-- Phase 13: pricing/catalog, packages, verified payment lifecycle and reconciliation.
ALTER TABLE plan
 ADD COLUMN IF NOT EXISTS plan_type VARCHAR(20) NOT NULL DEFAULT 'CHAPTER',
 ADD COLUMN IF NOT EXISTS resource_type VARCHAR(40),
 ADD COLUMN IF NOT EXISTS resource_id UUID,
 ADD COLUMN IF NOT EXISTS original_price_minor BIGINT NOT NULL DEFAULT 0,
 ADD COLUMN IF NOT EXISTS sale_price_minor BIGINT NOT NULL DEFAULT 0,
 ADD COLUMN IF NOT EXISTS discount_percent NUMERIC(5,2) NOT NULL DEFAULT 0,
 ADD COLUMN IF NOT EXISTS description TEXT,
 ADD COLUMN IF NOT EXISTS starts_at TIMESTAMPTZ,
 ADD COLUMN IF NOT EXISTS ends_at TIMESTAMPTZ,
 ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT now();
UPDATE plan SET original_price_minor=price_minor, sale_price_minor=price_minor WHERE original_price_minor=0 AND sale_price_minor=0;
ALTER TABLE plan ADD CONSTRAINT ck_plan_type CHECK (plan_type IN ('CHAPTER','BOOK','PACKAGE'));
ALTER TABLE plan ADD CONSTRAINT ck_plan_prices CHECK (original_price_minor >= 0 AND sale_price_minor >= 0 AND sale_price_minor <= original_price_minor);
ALTER TABLE plan ADD CONSTRAINT ck_plan_discount CHECK (discount_percent >= 0 AND discount_percent <= 100);
ALTER TABLE plan ADD CONSTRAINT ck_plan_resource_type CHECK (resource_type IS NULL OR resource_type IN ('CHAPTER','BOOK','PACKAGE'));
CREATE INDEX IF NOT EXISTS ix_plan_active_catalog ON plan(is_active,plan_type,resource_type,resource_id);
CREATE TABLE IF NOT EXISTS plan_package_item(id UUID PRIMARY KEY DEFAULT gen_random_uuid(),plan_id UUID NOT NULL REFERENCES plan(id) ON DELETE CASCADE,book_id UUID NOT NULL REFERENCES book(id) ON DELETE RESTRICT,UNIQUE(plan_id,book_id));
CREATE INDEX IF NOT EXISTS ix_plan_package_item_plan ON plan_package_item(plan_id);
CREATE TABLE IF NOT EXISTS free_content_access(id UUID PRIMARY KEY DEFAULT gen_random_uuid(),book_id UUID NOT NULL REFERENCES book(id) ON DELETE CASCADE,chapter_id UUID NOT NULL REFERENCES chapter(id) ON DELETE CASCADE,created_by UUID REFERENCES app_user(id) ON DELETE SET NULL,created_at TIMESTAMPTZ NOT NULL DEFAULT now(),UNIQUE(book_id,chapter_id));
CREATE INDEX IF NOT EXISTS ix_free_content_book ON free_content_access(book_id,chapter_id);
CREATE TABLE IF NOT EXISTS commerce_reconciliation_item(id UUID PRIMARY KEY DEFAULT gen_random_uuid(),provider VARCHAR(40) NOT NULL,provider_transaction_id VARCHAR(160),order_id UUID REFERENCES customer_order(id) ON DELETE SET NULL,expected_amount_minor BIGINT,actual_amount_minor BIGINT,expected_status VARCHAR(30),actual_status VARCHAR(30),mismatch_type VARCHAR(60) NOT NULL,details JSONB NOT NULL DEFAULT '{}'::jsonb,resolved BOOLEAN NOT NULL DEFAULT FALSE,created_at TIMESTAMPTZ NOT NULL DEFAULT now());
CREATE INDEX IF NOT EXISTS ix_recon_unresolved ON commerce_reconciliation_item(resolved,created_at DESC);
ALTER TABLE customer_order ADD COLUMN IF NOT EXISTS idempotency_key VARCHAR(160);
CREATE UNIQUE INDEX IF NOT EXISTS uq_customer_order_idempotency ON customer_order(user_id,idempotency_key) WHERE idempotency_key IS NOT NULL;
ALTER TABLE payment ADD COLUMN IF NOT EXISTS verified_at TIMESTAMPTZ;
ALTER TABLE payment ADD COLUMN IF NOT EXISTS provider_transaction_id VARCHAR(160);
CREATE UNIQUE INDEX IF NOT EXISTS uq_payment_provider_tx ON payment(provider,provider_transaction_id) WHERE provider_transaction_id IS NOT NULL;
