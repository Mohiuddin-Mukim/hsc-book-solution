package bd.hscsolution.student.content;

import com.google.gson.annotations.SerializedName;
import java.math.BigDecimal;
import java.util.List;

public final class CatalogModels {
    private CatalogModels() {}

    public record PageResponse<T>(
            List<T> items, int page, int size, long totalItems, int totalPages) {}

    public record SubjectItem(String id, String code, String name, int displayOrder) {}
    public record PaperItem(String id, String subjectId, String subjectCode, String code, String name,
                            Integer paperNumber, int displayOrder) {}
    public record BookItem(String id, String subjectId, String title, String publisher, String isbn) {}
    public record EditionItem(String id, String bookId, String editionLabel, Integer publicationYear) {}
    public record ChapterItem(String id, String paperId, String paperCode, String code, String title,
                              int displayOrder) {}
    public record TopicItem(String id, String chapterId, String code, String title, int displayOrder) {}
    public record PublishedQuestionItem(String id, String chapterId, String chapterCode,
                                        String questionType, String difficulty,
                                        BigDecimal marks, String publishedVersionId) {}

    public record QuestionDetail(
            String id, String chapterId, String chapterCode, String topicHint,
            String questionType, String difficulty, String status,
            QuestionVersionDetail version, SolutionDetail solution) {}

    public record QuestionVersionDetail(
            String id, int versionNo, String languageCode, String stem, BigDecimal marks,
            Long lockVersion, List<McqOption> options, List<McqExplanation> explanations,
            List<MediaAsset> media) {}

    public record McqOption(String id, String optionKey, String optionText, boolean correct, int displayOrder) {}
    public record McqExplanation(String id, String languageCode, String explanation) {}
    public record SolutionDetail(String id, String status, String currentVersionId, SolutionVersionDetail version) {}
    public record SolutionVersionDetail(String id, int versionNo, String languageCode, String summary,
                                        Long lockVersion, List<SolutionStep> steps) {}
    public record SolutionStep(String id, int stepNo, String title, String body, List<MediaAsset> media) {}
    public record MediaAsset(String id, String attachmentRole, int displayOrder, String caption, String altText,
                             int width, int height, long byteSize, String mimeType, String checksumSha256,
                             String lifecycleState, String deliveryPath, long usageCount) {}
}
