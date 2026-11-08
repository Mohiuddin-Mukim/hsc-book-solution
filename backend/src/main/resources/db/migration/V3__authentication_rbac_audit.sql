-- Phase 3: authentication/RBAC lifecycle. Never edit V1/V2 after they have been applied.
CREATE TABLE refresh_token (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 user_id UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
 token_hash CHAR(64) NOT NULL UNIQUE,
 expires_at TIMESTAMPTZ NOT NULL,
 revoked_at TIMESTAMPTZ,
 replaced_by UUID REFERENCES refresh_token(id) ON DELETE SET NULL,
 created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 last_used_at TIMESTAMPTZ,
 device_id UUID REFERENCES device(id) ON DELETE SET NULL
);
CREATE INDEX ix_refresh_token_user_active ON refresh_token(user_id,expires_at) WHERE revoked_at IS NULL;
CREATE INDEX ix_refresh_token_device ON refresh_token(device_id);

-- Align the Phase 2 seed with the Phase 3 role contract.
UPDATE role SET code='SUPPORT_AGENT', name='Support Agent' WHERE code='SUPPORT';
INSERT INTO role(code,name) VALUES ('SUPPORT_AGENT','Support Agent') ON CONFLICT(code) DO NOTHING;

INSERT INTO permission(code,name) VALUES
('USER_READ','Read users'),('USER_MANAGE','Manage users'),('ROLE_MANAGE','Manage user roles'),
('CONTENT_READ','Read content'),('CONTENT_CREATE','Create content'),('CONTENT_EDIT','Edit content'),
('CONTENT_REVIEW','Review content'),('CONTENT_PUBLISH','Publish content'),('MEDIA_MANAGE','Manage media'),
('ENTITLEMENT_ADJUST','Adjust entitlements'),('DEVICE_RESET','Reset devices'),('AUDIT_READ','Read audit log'),
('ANALYTICS_READ','Read analytics'),('SUPPORT_MANAGE','Manage support actions')
ON CONFLICT(code) DO NOTHING;

INSERT INTO role_permission(role_id,permission_id)
SELECT r.id,p.id FROM role r CROSS JOIN permission p
WHERE r.code='SUPER_ADMIN' ON CONFLICT DO NOTHING;
INSERT INTO role_permission(role_id,permission_id)
SELECT r.id,p.id FROM role r JOIN permission p ON p.code IN
('USER_READ','USER_MANAGE','ROLE_MANAGE','CONTENT_READ','CONTENT_CREATE','CONTENT_EDIT','CONTENT_REVIEW','CONTENT_PUBLISH','MEDIA_MANAGE','ENTITLEMENT_ADJUST','DEVICE_RESET','AUDIT_READ','ANALYTICS_READ','SUPPORT_MANAGE')
WHERE r.code='ADMIN' ON CONFLICT DO NOTHING;
INSERT INTO role_permission(role_id,permission_id)
SELECT r.id,p.id FROM role r JOIN permission p ON p.code IN ('CONTENT_READ','CONTENT_CREATE','CONTENT_EDIT','MEDIA_MANAGE')
WHERE r.code='CONTENT_EDITOR' ON CONFLICT DO NOTHING;
INSERT INTO role_permission(role_id,permission_id)
SELECT r.id,p.id FROM role r JOIN permission p ON p.code IN ('CONTENT_READ','CONTENT_REVIEW')
WHERE r.code='MODERATOR' ON CONFLICT DO NOTHING;
INSERT INTO role_permission(role_id,permission_id)
SELECT r.id,p.id FROM role r JOIN permission p ON p.code IN ('CONTENT_READ','CONTENT_REVIEW')
WHERE r.code='TEACHER_REVIEWER' ON CONFLICT DO NOTHING;
INSERT INTO role_permission(role_id,permission_id)
SELECT r.id,p.id FROM role r JOIN permission p ON p.code IN ('USER_READ','SUPPORT_MANAGE','DEVICE_RESET','CONTENT_READ')
WHERE r.code='SUPPORT_AGENT' ON CONFLICT DO NOTHING;
INSERT INTO role_permission(role_id,permission_id)
SELECT r.id,p.id FROM role r JOIN permission p ON p.code IN ('CONTENT_READ','ANALYTICS_READ')
WHERE r.code='ANALYST' ON CONFLICT DO NOTHING;
