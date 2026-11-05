# Phase 2 ERD
subject -> paper -> chapter -> topic.
question -> question_version and solution -> solution_version -> solution_step.
media_asset stores object-storage metadata only. question_media_attachment and
solution_step_media_attachment are explicit FK-safe junction tables with unique
(parent, display_order), allowing multiple ordered images per question version and
per individual solution step without dangling attachment rows.
plan -> customer_order -> payment -> subscription -> entitlement.
import_job -> import_row -> import_error; review_task and audit_log cover workflow/audit.
offline_manifest -> offline_manifest_item; analytics_event; idempotency_record; webhook_event.
