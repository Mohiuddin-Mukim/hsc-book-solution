# Phase 4 setup

1. Apply the patch over the Phase 3 working project. Do not copy `admin/node_modules`.
2. Start PostgreSQL with the existing Phase 2/3 Docker compose setup.
3. Run backend from IntelliJ IDEA:
   `cd backend`
   `mvn clean test`
4. Start the Spring Boot application. Flyway should report migration `V4__academic_catalog_versioned_content.sql` as current.
5. Open Swagger at `/swagger-ui.html` and authenticate with the Phase 3 admin login. Public student endpoints do not need an admin permission.
6. Create a Subject -> Paper -> Chapter -> Topic -> Question -> Question Version -> Solution Version -> Steps.
7. Move the question through `SUBMITTED -> APPROVED -> PUBLISHED` and verify the public catalog endpoint returns it.
8. Create a new version after publication. The old `published_version_id` remains the public snapshot while the new version becomes the working version.
9. Do not proceed to Phase 5 until the Phase 4 tests and manual exit checklist pass.
