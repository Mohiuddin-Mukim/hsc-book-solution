package bd.hscsolution.catalog.repository;
import bd.hscsolution.catalog.entity.McqExplanation; import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.*;
@Repository
public interface McqExplanationRepository extends JpaRepository<McqExplanation,UUID> {}
