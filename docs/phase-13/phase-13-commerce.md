# Phase 13 — Plans, pricing, payments and entitlements

## Pricing model
- All money is stored as integer minor units: 2500 = ৳25.00.
- `original_price_minor` is the crossed/anchor price and `sale_price_minor` is the actual payable price.
- `discount_percent` is server-calculated from those two values; clients cannot set it independently.
- `CHAPTER` plans price one chapter; `BOOK` plans price one book; `PACKAGE` plans contain multiple books.
- Free chapters are explicit `free_content_access` rows and are checked before paid access.

## Security model
The Android client never grants entitlement. A verified provider result is required. Webhook events are idempotent on `(provider, provider_event_id)`, and subscription creation is additionally guarded by `order_id`. Amount/currency mismatches are written to `commerce_reconciliation_item` and do not grant access.

## Provider adapter
Development defaults to `SANDBOX`, so Phase 13 can be tested without real money. The `PaymentProvider` interface is the production seam. A production provider must verify the transaction server-side before calling the same entitlement grant path.

## shurjoPay note
The official shurjoPay docs currently document sandbox and production API bases, token generation, checkout and `/verification`; verification response code 1000 is the successful transaction state. Live credentials are merchant-issued and must stay server-side. Current integration assumptions must be re-checked before production. See official docs: https://shurjopay.com.bd/frontend-developers/direct-integration-with-api

aamarPay is also a viable alternative provider; its official developer pages list custom API/Android integration, but current commercial pricing is quote/onboarding dependent. Do not hardcode a gateway fee into product prices.

## Admin workflow
Use `/admin/commerce/plans` to create/update offers and `/admin/commerce/free-chapters` to mark individual book chapters free. Package plans require at least one book.
