# Phase 13 — Manual MFS Payment Launch

## Migration rule

The current project already contains:
- V11__phase13_commerce.sql
- V12__phase13_payment_finish.sql

Do **not** rename, delete, replace, or rerun either file.

This patch adds exactly one new migration:
`V13__phase13_manual_payment_launch.sql`

It creates the manual payment tables, indexes, constraints and notifications. V12 remains the source of the existing payment uniqueness/ACID guardrails.

## Production-first payment flow

Android Buy/Unlock -> authenticated backend creates a short-lived random portal token -> dedicated web portal.

The token is stored only as SHA-256 in the database and expires after the configured TTL. The portal receives the user and plan only from that server-created token; price is always read from the database.

The portal collects:
- Payment method: bKash / Nagad / Rocket
- Sender mobile: 11-digit Bangladesh number
- Transaction ID / TrxID

Screenshot upload is intentionally not included. It is optional evidence, not required for verification. Admin verifies the TrxID and amount against the MFS account statement.

## Approval transaction

Approve is one database transaction:
1. lock request with FOR UPDATE
2. require PENDING
3. mark manual request APPROVED
4. mark customer order PAID
5. mark payment PAID + verified_at
6. grant subscription + entitlements
7. create user notification
8. write audit event

If any step fails, the transaction rolls back.

Reject similarly locks the request and atomically records rejection + order/payment failure + notification + audit event.

## Admin

`/payments/manual` is the pending-first verification queue.

`/payments/manual-settings` configures the receiving MFS account numbers, payment type and instructions.

## Local development

Backend default:
- API: http://localhost:8080
- Payment portal: http://localhost:5174

Run:
1. backend
2. admin (`npm run dev`)
3. payment portal (`cd payment-portal`, `npm install`, `npm run dev`)

Set:
`VITE_API_BASE_URL=http://localhost:8080/api/v1`

For production, point `MANUAL_PAYMENT_PORTAL_BASE_URL` at the deployed portal URL and configure `CORS_ORIGINS` for the deployed web origin.

## Required tests

1. Android Buy/Unlock opens the manual portal, not sandbox checkout.
2. Portal shows backend price and duration.
3. Sender number rejects invalid formats.
4. TrxID is normalized to uppercase and spaces are removed.
5. First submit creates exactly one PENDING request.
6. Re-opening the same token while PENDING shows status, reference and TrxID; form is hidden.
7. Re-submitting while PENDING is rejected.
8. Duplicate TrxID is rejected even for another user/package.
9. Reject shows admin reason and allows a new request.
10. Approve immediately grants access and creates notification.
11. Approved portal shows Access Active.
12. Android refresh shows Paid • Active and disables Buy.
13. Transaction History shows reference, TrxID, payment method, source, amount, status and validity.
14. Two simultaneous Approve requests: only one succeeds; no duplicate active subscription/entitlement.
15. Student cannot call admin queue/review/settings endpoints.
16. Unexpired portal token cannot be changed to another user/package.
17. Expired token is rejected.
