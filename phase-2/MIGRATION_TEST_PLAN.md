# Migration test plan
1. Empty PostgreSQL 17.x database -> application/Flyway starts cleanly.
2. Verify duplicate question version, MCQ option key/order, and solution step number fail.
3. Add 2 media attachments to one question version and 2 to one solution step; verify ordering.
4. Attempt deleting an attached media_asset; FK restriction must block dangling reference.
5. Delete solution_version; its steps/step attachments cascade.
6. Verify duplicate payment provider reference and webhook event ID fail.
7. Verify Flyway schema history is stable. Never edit an applied migration; use forward migrations.
