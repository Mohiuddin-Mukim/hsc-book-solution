# Phase 13 Finish — payment UX, duplicate protection, history and ACID hardening

This patch is based on the Phase 0–13 project state supplied by the user.

## Student-facing changes
- `GET /api/v1/student/plans` returns `accessStatus=PAID|AVAILABLE` and active expiry.
- Android plans refresh on `onResume`, so returning from sandbox checkout or reopening the Plans screen reflects paid access.
- Paid cards show `Paid • Active` and disable the purchase action.
- A paid plan cannot create another active subscription; the backend returns `ALREADY_PAID`.
- Transaction History shows offer/package, amount, payment status, paid time, provider transaction ID and access expiry.

## Backend safety
- Checkout amount is always read from the server-side plan record.
- Active subscription uniqueness is enforced by a PostgreSQL partial unique index.
- Subscription entitlement uniqueness is enforced by a PostgreSQL unique index.
- Checkout idempotency is unique per user and idempotency key and cannot be reused for a different plan/amount/currency.
- Webhook processing is transactional and locks the target order with `FOR UPDATE`.
- Webhook provider transaction/order/amount/currency is verified before entitlement grant.
- Duplicate provider transaction IDs are rejected by a database unique index.
- Duplicate webhook event IDs are ignored.
- Payment and entitlement are committed in the same transaction.
- Admin payment review endpoint is protected by `PERM_ENTITLEMENT_ADJUST` and paginated.

## Admin payment review
`GET /api/v1/admin/commerce/payments?page=0&size=50`

Returns purchaser, plan, amount, order/payment status, provider transaction ID, paid time and active entitlement expiry.

The supplied current project does not contain the React admin frontend, so this patch adds the protected backend contract for the admin payment/validity view rather than inventing an admin UI in a project snapshot that excludes it.

## Sandbox
The sandbox remains a demo provider. Production provider adapters must perform real provider API/signature verification server-side. No client success screen is authoritative.

## Test order
1. Flyway V12 applies.
2. Student opens Plans and sees AVAILABLE.
3. Purchase sandbox offer.
4. Complete sandbox payment.
5. Return to app; plan becomes PAID • Active.
6. Close/reopen Plans; still PAID • Active.
7. Try the same purchase again; backend returns ALREADY_PAID and no new order/subscription.
8. Open Transaction History and verify amount, paid time, transaction ID and expiry.
9. Replay identical webhook; no duplicate subscription.
10. Send same idempotency key for another plan; request is rejected.
11. Send wrong amount/currency; entitlement is not granted and reconciliation records the mismatch.
12. Verify admin payment endpoint with staff authorization.
13. Verify unauthorized users receive 403.
