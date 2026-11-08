# Phase 3 bootstrap admin + database connection fix

## Why this patch is needed

The previous bootstrap runner stopped whenever **any** `app_user` existed. That is incorrect: a normal user could prevent the SUPER_ADMIN bootstrap user from being created. The runner now checks specifically whether a SUPER_ADMIN assignment exists.

The datasource is also aligned with Docker Compose: the host port is **5433**. The application now reads `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, and `DB_PASSWORD` instead of hardcoding a different connection.

## Important

Do not commit the real `.env` file. Set your local `.env` values yourself. For the Docker PostgreSQL from this project, use:

```env
DB_HOST=localhost
DB_PORT=5433
DB_NAME=hsc_academic
DB_USERNAME=hsc_local
DB_PASSWORD=mukim
BOOTSTRAP_ADMIN_EMAIL=your-admin-email
BOOTSTRAP_ADMIN_PASSWORD=your-local-admin-password
BOOTSTRAP_ADMIN_NAME=Super Admin
```

## Expected startup log

After restart, look for:

```text
Bootstrap admin check: app_user=..., super_admin_users=0
Created bootstrap user ...
Bootstrap SUPER_ADMIN ready for ...
```

On later restarts:

```text
SUPER_ADMIN already exists; bootstrap creation skipped.
```

## Database verification

Run these queries in the same PostgreSQL connection used by the backend:

```sql
select current_database(), current_user;
select version from flyway_schema_history order by installed_rank;
select id,email,display_name,status,email_verified from app_user order by created_at;
select id,code,name from role order by code;
select u.email,r.code
from app_user u
join user_role ur on ur.user_id=u.id
join role r on r.id=ur.role_id
order by u.email,r.code;
```

You should see the bootstrap email with `SUPER_ADMIN` in the final query.

## Do not manually insert a user unless the runner still fails

If the startup log says `Bootstrap SUPER_ADMIN ready` but the IntelliJ database browser shows no such row, your IntelliJ connection is pointing at a different PostgreSQL database/port. Compare the result of `select current_database(), current_user;` and the host/port with the backend datasource.
