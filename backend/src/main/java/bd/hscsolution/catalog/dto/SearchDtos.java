package bd.hscsolution.catalog.dto;

import java.math.BigDecimal;
import java.util.UUID;
import static bd.hscsolution.catalog.entity.AcademicEnums.*;

public final class SearchDtos {
    private SearchDtos() {}

    public record Result(
            UUID id, String externalId, UUID chapterId, String chapterCode, String chapterTitle,
            UUID subjectId, String subjectCode, String subjectName,
            UUID paperId, String paperCode, String paperName,
            String languageCode, QuestionType questionType, Difficulty difficulty,
            BigDecimal marks, UUID versionId, double relevanceScore) {}
}
