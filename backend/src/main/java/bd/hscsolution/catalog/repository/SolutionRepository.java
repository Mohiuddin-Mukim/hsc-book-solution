package bd.hscsolution.catalog.repository;
import bd.hscsolution.catalog.entity.Solution; import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

import java.util.*;
@Repository
public interface SolutionRepository extends JpaRepository<Solution,UUID> { Optional<Solution> findByQuestionId(UUID q); }
