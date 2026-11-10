package bd.hscsolution.catalog.repository;

import bd.hscsolution.catalog.entity.Question;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.*;

import static bd.hscsolution.catalog.entity.AcademicEnums.*;

@Repository
public interface QuestionRepository extends JpaRepository<Question, UUID> {
    @Query("select q from Question q where q.chapter.id=:chapter and (:publishedOnly=false or q.status=:publishedStatus) and (:type is null or q.questionType=:type) and (:difficulty is null or q.difficulty=:difficulty) and (:minMarks is null or exists(select v from QuestionVersion v where v.id=q.currentVersionId and v.marks>=:minMarks)) and (:maxMarks is null or exists(select v from QuestionVersion v where v.id=q.currentVersionId and v.marks<=:maxMarks))")
    Page<Question> search(@Param("chapter") UUID chapter, @Param("publishedOnly") boolean publishedOnly, @Param("publishedStatus") ContentStatus publishedStatus, @Param("type") QuestionType type, @Param("difficulty") Difficulty difficulty, @Param("minMarks") java.math.BigDecimal minMarks, @Param("maxMarks") java.math.BigDecimal maxMarks, Pageable pageable);
}
