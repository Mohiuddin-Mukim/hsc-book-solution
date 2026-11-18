
package bd.hscsolution.imports;

import jakarta.validation.constraints.NotBlank;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public final class ImportDtos {
    private ImportDtos() {}

    public enum ImportMode { CREATE_ONLY, UPDATE_AS_NEW_VERSION, SKIP_DUPLICATES }
    public enum ImportStatus { STAGED, VALIDATING, READY, COMMITTED, PARTIAL, FAILED }

    public record JsonPasteRequest(
            @NotBlank String content,
            @NotBlank String mode,
            boolean dryRun,
            String idempotencyKey,
            String fileName) {}

    public record ImportJobResponse(
            UUID id, String sourceType, String fileName, String status, String mode,
            boolean dryRun, int totalRows, int successRows, int errorRows,
            OffsetDateTime createdAt, OffsetDateTime updatedAt, OffsetDateTime committedAt) {}

    public record ImportRowResponse(
            UUID id, int rowNo, String externalId, String status,
            UUID questionId, Object payload, List<ImportErrorResponse> errors) {}

    public record ImportErrorResponse(
            String fieldName, String errorCode, String severity, String message) {}

    public record ImportPreviewResponse(
            ImportJobResponse job, List<ImportRowResponse> rows, int warningCount) {}

    public record CommitResponse(
            ImportJobResponse job, int createdCount, int versionCreatedCount,
            int skippedCount, int failedCount) {}

    public record ErrorReportRow(
            int rowNo, String externalId, String fieldName, String errorCode,
            String severity, String message) {}
}
