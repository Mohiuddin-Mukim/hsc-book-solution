# Phase 3 Fix — Login / Unauthorized popup

This is a corrective patch for Phase 3 only. It does not change V1, V2, or add Phase 4.

Changes:
1. Prevents Spring Security 401 responses from carrying a browser Basic-Auth challenge.
   This stops the native username/password popup when the API returns 401.
2. Makes the PostgreSQL connection configurable through DB_URL/DB_USERNAME/DB_PASSWORD.
3. Removes bootstrap-admin credentials from application.yml defaults.
4. Adds complete local `.env.example` values for the Phase 3 security setup.
5. Adds the Admin Vite API URL example.

Important:
- Put the real local values in the untracked root `.env`.
- If the database currently has zero app_user rows, restart the backend with
  BOOTSTRAP_ADMIN_EMAIL/BOOTSTRAP_ADMIN_PASSWORD/BOOTSTRAP_ADMIN_NAME set.
- Then verify app_user and the SUPER_ADMIN role before testing the Admin login.
