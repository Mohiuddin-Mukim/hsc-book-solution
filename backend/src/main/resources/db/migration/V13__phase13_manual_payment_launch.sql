-- Phase 13 manual payment launch.
-- IMPORTANT: V12__phase13_payment_finish is intentionally preserved.
-- This migration only adds the manual MFS payment workflow; it does not replace or edit V12.

CREATE TABLE IF NOT EXISTS manual_payment_channel (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    method VARCHAR(16) NOT NULL,
    account_number VARCHAR(32) NOT NULL,
    payment_type VARCHAR(20) NOT NULL,
    instruction TEXT NOT NULL DEFAULT '',
    is_active BOOLEAN NOT NULL DEFAULT FALSE,
    sort_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_manual_channel_method CHECK (method IN ('BKASH','NAGAD','ROCKET')),
    CONSTRAINT ck_manual_channel_payment_type CHECK (payment_type IN ('Send Money','Payment')),
    CONSTRAINT ck_manual_channel_account CHECK (length(trim(account_number)) >= 5)
);

CREATE INDEX IF NOT EXISTS ix_manual_channel_active_order
    ON manual_payment_channel(is_active, sort_order, method);

CREATE TABLE IF NOT EXISTS manual_payment_portal_session (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    token_hash CHAR(64) NOT NULL UNIQUE,
    user_id UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    plan_id UUID NOT NULL REFERENCES plan(id) ON DELETE RESTRICT,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS ix_manual_portal_session_expiry
    ON manual_payment_portal_session(expires_at);

CREATE TABLE IF NOT EXISTS manual_payment_request (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reference_code VARCHAR(40) NOT NULL UNIQUE,
    user_id UUID NOT NULL REFERENCES app_user(id) ON DELETE RESTRICT,
    plan_id UUID NOT NULL REFERENCES plan(id) ON DELETE RESTRICT,
    order_id UUID NOT NULL UNIQUE REFERENCES customer_order(id) ON DELETE RESTRICT,
    expected_amount_minor BIGINT NOT NULL,
    currency CHAR(3) NOT NULL DEFAULT 'BDT',
    duration_days INT NOT NULL,
    payment_method VARCHAR(16) NOT NULL,
    sender_mobile VARCHAR(11) NOT NULL,
    transaction_id VARCHAR(64) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    reviewed_by UUID REFERENCES app_user(id) ON DELETE SET NULL,
    reviewed_at TIMESTAMPTZ,
    rejection_reason VARCHAR(500),
    admin_message VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_manual_request_method CHECK (payment_method IN ('BKASH','NAGAD','ROCKET')),
    CONSTRAINT ck_manual_request_status CHECK (status IN ('PENDING','APPROVED','REJECTED')),
    CONSTRAINT ck_manual_request_amount CHECK (expected_amount_minor >= 0),
    CONSTRAINT ck_manual_request_duration CHECK (duration_days > 0),
    CONSTRAINT ck_manual_sender_mobile CHECK (sender_mobile ~ '^01[3-9][0-9]{8}$')
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_manual_request_trxid
    ON manual_payment_request(transaction_id);

CREATE UNIQUE INDEX IF NOT EXISTS uq_manual_request_pending_user_plan
    ON manual_payment_request(user_id, plan_id)
    WHERE status = 'PENDING';

CREATE INDEX IF NOT EXISTS ix_manual_request_queue
    ON manual_payment_request(status, created_at ASC);

CREATE INDEX IF NOT EXISTS ix_manual_request_user_created
    ON manual_payment_request(user_id, created_at DESC);

CREATE INDEX IF NOT EXISTS ix_manual_request_plan_created
    ON manual_payment_request(plan_id, created_at DESC);

CREATE TABLE IF NOT EXISTS user_notification (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    title VARCHAR(160) NOT NULL,
    message TEXT NOT NULL,
    type VARCHAR(40) NOT NULL DEFAULT 'GENERAL',
    entity_type VARCHAR(60),
    entity_id UUID,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS ix_user_notification_unread
    ON user_notification(user_id, is_read, created_at DESC);

-- Initial channel rows are inactive until an admin configures the real production MFS numbers.
INSERT INTO manual_payment_channel(method, account_number, payment_type, instruction, is_active, sort_order)
VALUES
    ('BKASH', '00000000000', 'Send Money', 'Configure the production bKash number in Admin → Plans & Payments.', FALSE, 10),
    ('NAGAD', '00000000000', 'Send Money', 'Configure the production Nagad number in Admin → Plans & Payments.', FALSE, 20),
    ('ROCKET', '00000000000', 'Send Money', 'Configure the production Rocket number in Admin → Plans & Payments.', FALSE, 30)
ON CONFLICT DO NOTHING;
