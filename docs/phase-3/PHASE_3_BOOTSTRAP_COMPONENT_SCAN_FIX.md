# Phase 3 — definitive bootstrap/component-scan fix

## Root cause

The main Spring Boot class is in package `bd.hscsolution.api`, while Phase 3 security/auth/admin classes are in sibling packages such as:

- `bd.hscsolution.security`
- `bd.hscsolution.auth`
- `bd.hscsolution.admin`

Default Spring component scanning starts from `bd.hscsolution.api` and its child packages, so the Phase 3 classes were not discovered.

This patch changes `HscApiApplication` to scan the full `bd.hscsolution` package tree.

## Files changed

- `backend/src/main/java/bd/hscsolution/api/HscApiApplication.java`

## Apply

1. Extract this patch into the project root and replace the existing file.
2. Open the `backend` folder in IntelliJ IDEA.
3. Run a clean build:

```bash
mvn clean test
```

4. Start the backend again.

## Expected startup log

You should now see one of these:

```text
Bootstrap admin check: app_user=..., super_admin_users=0
Created bootstrap user ...
Bootstrap SUPER_ADMIN ready for ...
```

or, if the administrator already exists:

```text
Bootstrap admin check: app_user=..., super_admin_users=1
SUPER_ADMIN already exists; bootstrap creation skipped.
```

## Verify database

Run:

```sql
select u.email, r.code
from app_user u
join user_role ur on ur.user_id = u.id
join role r on r.id = ur.role_id
where r.code = 'SUPER_ADMIN';
```

Then test:

- `POST /api/v1/auth/login`
- `GET /api/v1/auth/me` with the access token

Do not manually insert a user if the runner succeeds.
