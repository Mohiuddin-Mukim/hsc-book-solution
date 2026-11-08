# Phase 3 Authentication API

Base: `/api/v1`

## Login
`POST /auth/login`

Body: `{ "email": "admin@example.com", "password": "..." }`

For Admin Web send `X-Client-Type: ADMIN_WEB`. The refresh token is issued as an HttpOnly cookie; the access token stays in frontend memory.

For Android send `X-Client-Type: ANDROID`. The response contains an access token and a refresh token. Android stores both using its Keystore-backed token store.

## Refresh
Admin Web: `POST /auth/refresh` with credentials/cookie.
Android: `POST /auth/refresh` with `{ "refreshToken": "..." }` and `X-Client-Type: ANDROID`.

Refresh tokens are rotated. The previous token is revoked and linked to the replacement. Reuse of a revoked token returns 401.

## Logout
`POST /auth/logout`. Revokes the supplied refresh token and clears the web cookie.

## Current user
`GET /auth/me` with `Authorization: Bearer <access-token>`.

## Errors
401 = missing/invalid/expired authentication. 403 = authenticated but missing permission. 429 = login rate limit. Responses use the existing `ApiError` envelope and correlation ID.

## CORS
Set `CORS_ORIGINS` to exact trusted origins, comma-separated. Do not use `*` when credentials are enabled.
