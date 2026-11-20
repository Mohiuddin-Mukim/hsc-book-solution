package bd.hscsolution.catalog.dto;

import java.math.BigDecimal;
import java.util.UUID;
import static bd.hscsolution.catalog.entity.AcademicEnums.*;

public final class PublicCatalogDtos {
    private PublicCatalogDtos() {}

    public record SubjectItem(UUID id, String code, String name, int displayOrder) {}
    public record PaperItem(UUID id, UUID subjectId, String subjectCode, String code, String name,
                            Short paperNumber, int displayOrder) {}
    public record BookItem(UUID id, UUID subjectId, String title, String publisher, String isbn) {}
    public record EditionItem(UUID id, UUID bookId, String editionLabel, Short publicationYear) {}
    public record ChapterItem(UUID id, UUID paperId, String paperCode, String code, String title,
                              int displayOrder) {}
    public record TopicItem(UUID id, UUID chapterId, String code, String title, int displayOrder) {}
    public record PublishedQuestionItem(UUID id, UUID chapterId, String chapterCode,
                                        QuestionType questionType, Difficulty difficulty,
                                        BigDecimal marks, UUID publishedVersionId) {}
}
