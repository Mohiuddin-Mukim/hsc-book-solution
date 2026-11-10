package bd.hscsolution.catalog.repository;
import bd.hscsolution.catalog.entity.QuestionTag; import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public interface QuestionTagRepository extends JpaRepository<QuestionTag,QuestionTag.Key>{List<QuestionTag> findByQuestionVersionId(UUID versionId);}
