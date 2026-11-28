# Phase 13 patch manifest
- V11 commerce schema: chapter/book/package offers, original/sale prices, auto discount, free chapters, reconciliation and idempotency.
- PaymentProvider seam + sandbox provider; no production secrets.
- Student plans/checkout/order status API and Android Plans screen.
- Admin Plans & Payments screen for offer creation/editing and discount preview.
- Package contains multiple books; chapter/book purchases grant corresponding book/chapter entitlements.
- Free chapter rows bypass paid access.
- Paid catalog access is enforced only after an active price exists, so existing unpriced content remains usable during development.
- Server never trusts client success or client price.
