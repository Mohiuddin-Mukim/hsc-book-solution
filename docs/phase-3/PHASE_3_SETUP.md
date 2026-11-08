# Phase 3 setup

1. Keep V1 and V2 unchanged. Phase 3 adds V3.
2. Copy only the files in the Phase 3 patch into the matching paths of the current repository.
3. Copy `.env.example` changes manually only if your repository already has a customized env file; never commit `.env`.
4. Start PostgreSQL with `docker compose --env-file .env -f infra/docker-compose.yml up -d postgres`.
5. Backend: open `backend/` in IntelliJ IDEA and run `mvn spring-boot:run`, or use the Maven wrapper if your repository later adds one.
6. Optional first local admin bootstrap: set BOOTSTRAP_ADMIN_EMAIL, BOOTSTRAP_ADMIN_PASSWORD and BOOTSTRAP_ADMIN_NAME in local `.env`/environment. These are local secrets and must not be committed.
7. Admin: run `npm ci` then `npm run dev` inside `admin/`. Set `VITE_API_BASE_URL=http://localhost:8080/api/v1` in an uncommitted local environment file if needed.
8. Android Studio is only needed for the Android module. Phase 3 adds the auth foundation; it does not replace the app UI.
9. Run backend tests and admin tests. Then push and wait for GitHub Actions to become green.

### Local smoke test
- `POST /api/v1/auth/login` with the bootstrap admin credentials.
- `GET /api/v1/auth/me` using the returned access token.
- Call a protected endpoint without a token and confirm 401.
- Give a user a role using the role-management endpoint and confirm the audit row is created.
- Reset a device with the device-reset endpoint and confirm the refresh tokens for that device are revoked.
