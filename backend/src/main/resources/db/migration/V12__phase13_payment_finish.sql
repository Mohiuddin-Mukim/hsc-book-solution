-- Phase 13 finish: purchase uniqueness, payment history metadata and ACID guardrails.
ALTER TABLE payment ADD COLUMN IF NOT EXISTS verified_at TIMESTAMPTZ;
ALTER TABLE payment ADD COLUMN IF NOT EXISTS provider_transaction_id VARCHAR(160);

-- Normalize stale ACTIVE rows first.
UPDATE subscription SET status='EXPIRED' WHERE status='ACTIVE' AND ends_at <= now();

-- Repair duplicate active subscriptions created by the pre-finish demo flow.
-- Keep the newest active subscription per user/plan and expire older duplicates.
WITH ranked AS (
    SELECT id, ROW_NUMBER() OVER (PARTITION BY user_id, plan_id ORDER BY ends_at DESC, created_at DESC) AS rn
    FROM subscription
    WHERE status = 'ACTIVE'
)
UPDATE entitlement e
SET ends_at = now()
WHERE e.subscription_id IN (SELECT id FROM ranked WHERE rn > 1)
  AND e.ends_at > now();

WITH ranked AS (
    SELECT id, ROW_NUMBER() OVER (PARTITION BY user_id, plan_id ORDER BY ends_at DESC, created_at DESC) AS rn
    FROM subscription
    WHERE status = 'ACTIVE'
)
UPDATE subscription s
SET status = 'EXPIRED'
WHERE s.id IN (SELECT id FROM ranked WHERE rn > 1);

CREATE UNIQUE INDEX IF NOT EXISTS uq_active_subscription_user_plan
    ON subscription(user_id, plan_id)
    WHERE status = 'ACTIVE';

CREATE UNIQUE INDEX IF NOT EXISTS uq_subscription_entitlement_resource
    ON entitlement(subscription_id, resource_type, resource_id);

CREATE INDEX IF NOT EXISTS ix_payment_order_paid_at
    ON payment(order_id, paid_at DESC);

CREATE INDEX IF NOT EXISTS ix_subscription_user_active_expiry
    ON subscription(user_id, status, ends_at DESC);

CREATE INDEX IF NOT EXISTS ix_entitlement_user_expiry
    ON entitlement(subscription_id, ends_at DESC);
