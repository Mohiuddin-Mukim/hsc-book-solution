package bd.hscsolution.catalog.dto;

import bd.hscsolution.catalog.entity.AcademicEnums.ContentStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;

public final class AuthoringDtos {
    private AuthoringDtos() {}

    public record VersionEditRequest(
            @NotBlank String languageCode,
            @NotBlank String stem,
            @DecimalMin("0.0") BigDecimal marks,
            String finalAnswer,
            @NotNull Long expectedLockVersion) {}

    public record SolutionEditRequest(
            @NotBlank String languageCode,
            String summary,
            @NotNull Long expectedLockVersion,
            @Valid List<bd.hscsolution.catalog.dto.AcademicDtos.SolutionStepRequest> steps) {}

    public record CommentRequest(@NotBlank @Size(max=5000) String body, UUID versionId) {}

    public record ReviewActionRequest(
            @NotNull ContentStatus targetStatus,
            @Size(max=2000) String note) {}

    public record ReviewEventDto(UUID id, UUID questionId, UUID actorUserId,
                                 ContentStatus fromStatus, ContentStatus toStatus,
                                 String note, OffsetDateTime createdAt) {}

    public record ReviewCommentDto(UUID id, UUID questionId, UUID versionId,
                                   UUID actorUserId, String body, OffsetDateTime createdAt) {}

    public record VersionHistoryItem(UUID id, int versionNo, String languageCode,
                                     String stem, BigDecimal marks, Long lockVersion) {}

    public record HistoryResponse(
            UUID questionId, ContentStatus status, UUID currentVersionId,
            UUID publishedVersionId, List<VersionHistoryItem> versions,
            List<ReviewEventDto> events, List<ReviewCommentDto> comments) {}

    public record DiffResponse(UUID questionId, VersionHistoryItem fromVersion,
                               VersionHistoryItem toVersion, boolean stemChanged,
                               boolean marksChanged) {}

    public record RollbackRequest(@NotNull UUID targetVersionId) {}
}
