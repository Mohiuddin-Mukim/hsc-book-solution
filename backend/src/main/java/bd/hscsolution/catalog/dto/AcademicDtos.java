package bd.hscsolution.catalog.dto;

import java.math.BigDecimal;
import java.util.*;

import jakarta.validation.constraints.*;

import static bd.hscsolution.catalog.entity.AcademicEnums.*;

public final class AcademicDtos {
    private AcademicDtos() {
    }

    public record SubjectRequest(@NotBlank @Size(max = 50) String code, @NotBlank @Size(max = 160) String name,
                                 @Min(0) int displayOrder, boolean active) {
    }

    public record SubjectResponse(UUID id, String code, String name, int displayOrder, boolean active) {
    }

    public record PaperRequest(@NotNull UUID subjectId, @NotBlank @Size(max = 30) String code,
                               @NotBlank @Size(max = 160) String name, Short paperNumber, @Min(0) int displayOrder) {
    }

    public record PaperResponse(UUID id, UUID subjectId, String subjectCode, String code, String name,
                                Short paperNumber, int displayOrder) {
    }

    public record BookRequest(@NotNull UUID subjectId, @NotBlank @Size(max = 240) String title, String publisher,
                              String isbn) {
    }

    public record BookResponse(UUID id, UUID subjectId, String title, String publisher, String isbn) {
    }

    public record EditionRequest(@NotBlank @Size(max = 80) String editionLabel, Short publicationYear) {
    }

    public record EditionResponse(UUID id, UUID bookId, String editionLabel, Short publicationYear) {
    }

    public record ChapterRequest(@NotNull UUID paperId, @NotBlank @Size(max = 40) String code,
                                 @NotBlank @Size(max = 200) String title, @Min(0) int displayOrder) {
    }

    public record ChapterResponse(UUID id, UUID paperId, String paperCode, String code, String title,
                                  int displayOrder) {
    }

    public record TopicRequest(@NotNull UUID chapterId, @NotBlank @Size(max = 40) String code,
                               @NotBlank @Size(max = 200) String title, @Min(0) int displayOrder) {
    }

    public record TopicResponse(UUID id, UUID chapterId, String code, String title, int displayOrder) {
    }

    public record QuestionCreateRequest(@NotNull UUID chapterId, @NotNull QuestionType questionType,
                                        Difficulty difficulty, @NotBlank String languageCode, @NotBlank String stem,
                                        @DecimalMin("0.0") BigDecimal marks) {
    }

    public record QuestionVersionRequest(@NotBlank String languageCode, @NotBlank String stem,
                                         @DecimalMin("0.0") BigDecimal marks) {
    }

    public record McqOptionRequest(String optionKey, String optionText, boolean correct, int displayOrder) {
    }

    public record McqExplanationRequest(String languageCode, String explanation) {
    }

    public record SolutionStepRequest(int stepNo, String title, String body) {
    }

    public record SolutionVersionRequest(String languageCode, String summary, List<SolutionStepRequest> steps) {
    }

    public record QuestionSummary(UUID id, UUID chapterId, String chapterCode, QuestionType questionType,
                                  Difficulty difficulty, ContentStatus status, UUID currentVersionId) {
    }

    public record QuestionDetail(UUID id, UUID chapterId, String chapterCode, String topicHint,
                                 QuestionType questionType, Difficulty difficulty, ContentStatus status,
                                 QuestionVersionDetail version, SolutionDetail solution) {
    }

    public record QuestionVersionDetail(UUID id, int versionNo, String languageCode, String stem, BigDecimal marks,
                                        List<McqOptionDto> options, List<McqExplanationDto> explanations) {
    }

    public record McqOptionDto(UUID id, String optionKey, String optionText, boolean correct, int displayOrder) {
    }

    public record McqExplanationDto(UUID id, String languageCode, String explanation) {
    }

    public record SolutionDetail(UUID id, ContentStatus status, UUID currentVersionId, SolutionVersionDetail version) {
    }

    public record SolutionVersionDetail(UUID id, int versionNo, String languageCode, String summary,
                                        List<SolutionStepDto> steps) {
    }

    public record SolutionStepDto(UUID id, int stepNo, String title, String body) {
    }

    public record WorkflowRequest(ContentStatus targetStatus, String note) {
    }

    public record TagRequest(String code, String label) {
    }

    public record TagResponse(UUID id, String code, String label) {
    }
}
