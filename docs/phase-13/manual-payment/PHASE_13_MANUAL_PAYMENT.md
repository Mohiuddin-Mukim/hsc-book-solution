# Phase 13 — Manual Payment Launch Path

The production launch path is manual MFS verification. The sandbox/automated gateway remains in the codebase for development but is no longer exposed by the Android purchase UI.

## Flow
1. Student taps **Buy / Unlock** in Android.
2. Android calls `POST /api/v1/student/manual-payment-portal` with the selected plan ID.
3. Backend reads the plan price/duration from PostgreSQL and returns a short-lived opaque portal URL tied to the authenticated user + plan.
4. The React web app opens `/pay?token=...`.
5. The public portal displays package, exact server price, validity and active bKash/Nagad/Rocket instructions.
6. Student submits MFS method, sender number, TrxID and optional screenshot.
7. Backend creates a `PENDING` manual request and a `PENDING_MANUAL` payment/order in one transaction.
8. Admin opens **Manual Payment Queue**, verifies the payment and approves/rejects it.
9. Approval atomically updates request + order + payment + subscription + entitlements + notification.
10. Android refreshes access state and shows `Paid • Active`; transaction history contains the full record.

## Important production configuration
- `MANUAL_PAYMENT_PORTAL_BASE_URL` — public React web URL, e.g. `https://pay.example.com/pay`.
- `MANUAL_PAYMENT_SUPPORT_WHATSAPP_URL` — support WhatsApp URL shown on pending/rejected states.
- `MANUAL_PAYMENT_SCREENSHOT_DIR` — private storage location for screenshots. Move this to object storage before scaling beyond the initial launch.
- `MANUAL_PAYMENT_PORTAL_TTL_MINUTES` — default 30.
- `MANUAL_PAYMENT_MAX_SCREENSHOT_BYTES` — default 5 MiB.
- `CORS_ORIGINS` must include the web portal origin when the portal is hosted separately.

## Admin setup
After migration, open **Manual Payment Queue → Payment channels** and set the real bKash/Nagad/Rocket account numbers, payment type and instructions. These values are what students see on the public portal.

## Security model
- Price and validity are read from the backend database; the web form cannot change them.
- Portal token is random, stored only as SHA-256, scoped to user + plan and expires quickly.
- One `PENDING` request per user/package is enforced by a PostgreSQL partial unique index.
- TrxID is normalized to uppercase and unique per payment method.
- Approval/rejection locks the request row and is transactional.
- Only `PERM_ENTITLEMENT_ADJUST` can approve/reject requests.
- Student endpoints never accept a user ID from the client.
- The sandbox gateway is not linked from Android.

## Minimal test matrix
### Student
- Buy/Unlock opens the manual web portal, not the sandbox gateway.
- Exact package price and validity match the active plan.
- Sender number accepts `01XXXXXXXXX` only.
- TrxID is uppercased.
- Screenshot accepts JPG/PNG/WEBP and rejects oversized/unsupported files.
- Reopening a package with a pending request shows status instead of another form.
- Reopening after approval shows Access Active.
- Rejection shows the admin message and allows a new request.

### Admin
- Pending queue is oldest first.
- Approve creates/activates the subscription and entitlement.
- Reject does not grant access.
- Double approve/reject returns conflict and does not duplicate entitlement.
- Student cannot call admin review endpoints.
- Screenshot can be opened from the queue.

### Regression
- Existing sandbox checkout remains available only when explicitly used for development/testing.
- Existing gateway webhook idempotency and reconciliation continue to work.
- Existing catalog/content/offline features remain unchanged.
