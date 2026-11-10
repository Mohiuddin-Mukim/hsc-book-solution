package bd.hscsolution.catalog.repository;
import bd.hscsolution.catalog.entity.QuestionVersion; import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

import java.util.*;
@Repository
public interface QuestionVersionRepository extends JpaRepository<QuestionVersion,UUID> { Optional<QuestionVersion> findByQuestionIdAndLanguageCodeAndVersionNo(UUID q,String lang,int version); int countByQuestionIdAndLanguageCode(UUID q,String lang); List<QuestionVersion> findByQuestionIdAndLanguageCodeOrderByVersionNoDesc(UUID q,String lang); }
