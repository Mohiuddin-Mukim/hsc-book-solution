package bd.hscsolution.catalog.repository;

import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.UUID;

@Repository
public interface QuestionSearchRepository extends org.springframework.data.repository.Repository<bd.hscsolution.catalog.entity.Question, UUID> {

    interface Row {
        UUID getId();
        String getExternalId();
        UUID getChapterId();
        String getChapterCode();
        String getChapterTitle();
        UUID getSubjectId();
        String getSubjectCode();
        String getSubjectName();
        UUID getPaperId();
        String getPaperCode();
        String getPaperName();
        String getLanguageCode();
        String getQuestionType();
        String getDifficulty();
        BigDecimal getMarks();
        UUID getVersionId();
        Double getRelevanceScore();
    }

    @Query(value = """
        SELECT
          q.id AS id, q.external_id AS externalId,
          c.id AS chapterId, c.code AS chapterCode, c.title AS chapterTitle,
          s.id AS subjectId, s.code AS subjectCode, s.name AS subjectName,
          p.id AS paperId, p.code AS paperCode, p.name AS paperName,
          qv.language_code AS languageCode,
          q.question_type AS questionType, q.difficulty AS difficulty,
          qv.marks AS marks, qv.id AS versionId,
          CASE
            WHEN lower(coalesce(q.external_id, '')) = lower(:query) THEN 1000.0
            WHEN cast(q.id AS text) = :query THEN 1000.0
            ELSE ts_rank_cd(
              to_tsvector('simple', coalesce(qv.stem, '')),
              websearch_to_tsquery('simple', :query)
            )
          END AS relevanceScore
        FROM question q
        JOIN chapter c ON c.id = q.chapter_id
        JOIN paper p ON p.id = c.paper_id
        JOIN subject s ON s.id = p.subject_id
        JOIN question_version qv
          ON qv.id = CASE WHEN q.status = 'PUBLISHED'
                          THEN q.published_version_id
                          ELSE q.current_version_id END
        WHERE
          (:publishedOnly = false OR
             (q.status = 'PUBLISHED' AND q.published_version_id IS NOT NULL))
          AND (:query = ''
               OR lower(coalesce(q.external_id, '')) = lower(:query)
               OR cast(q.id AS text) = :query
               OR to_tsvector('simple', coalesce(qv.stem, ''))
                    @@ websearch_to_tsquery('simple', :query))
          AND (:subjectId IS NULL OR s.id = :subjectId)
          AND (:paperId IS NULL OR p.id = :paperId)
          AND (:chapterId IS NULL OR c.id = :chapterId)
          AND (:languageCode IS NULL OR qv.language_code = :languageCode)
          AND (:questionType IS NULL OR q.question_type = :questionType)
          AND (:difficulty IS NULL OR q.difficulty = :difficulty)
        ORDER BY relevanceScore DESC, q.id ASC
        """,
        countQuery = """
        SELECT count(*)
        FROM question q
        JOIN chapter c ON c.id = q.chapter_id
        JOIN paper p ON p.id = c.paper_id
        JOIN subject s ON s.id = p.subject_id
        JOIN question_version qv
          ON qv.id = CASE WHEN q.status = 'PUBLISHED'
                          THEN q.published_version_id
                          ELSE q.current_version_id END
        WHERE
          (:publishedOnly = false OR
             (q.status = 'PUBLISHED' AND q.published_version_id IS NOT NULL))
          AND (:query = ''
               OR lower(coalesce(q.external_id, '')) = lower(:query)
               OR cast(q.id AS text) = :query
               OR to_tsvector('simple', coalesce(qv.stem, ''))
                    @@ websearch_to_tsquery('simple', :query))
          AND (:subjectId IS NULL OR s.id = :subjectId)
          AND (:paperId IS NULL OR p.id = :paperId)
          AND (:chapterId IS NULL OR c.id = :chapterId)
          AND (:languageCode IS NULL OR qv.language_code = :languageCode)
          AND (:questionType IS NULL OR q.question_type = :questionType)
          AND (:difficulty IS NULL OR q.difficulty = :difficulty)
        """,
        nativeQuery = true)
    Page<Row> search(
            @Param("query") String query,
            @Param("publishedOnly") boolean publishedOnly,
            @Param("subjectId") UUID subjectId,
            @Param("paperId") UUID paperId,
            @Param("chapterId") UUID chapterId,
            @Param("languageCode") String languageCode,
            @Param("questionType") String questionType,
            @Param("difficulty") String difficulty,
            Pageable pageable);
}
