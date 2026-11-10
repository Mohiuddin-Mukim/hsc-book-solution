package bd.hscsolution.catalog.repository;
import bd.hscsolution.catalog.entity.SolutionVersion; import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public interface SolutionVersionRepository extends JpaRepository<SolutionVersion,UUID> { Optional<SolutionVersion> findBySolutionIdAndLanguageCodeAndVersionNo(UUID s,String lang,int version); int countBySolutionIdAndLanguageCode(UUID s,String lang); List<SolutionVersion> findBySolutionIdAndLanguageCodeOrderByVersionNoDesc(UUID s,String lang); }
