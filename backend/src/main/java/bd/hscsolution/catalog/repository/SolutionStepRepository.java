package bd.hscsolution.catalog.repository;
import bd.hscsolution.catalog.entity.SolutionStep; import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.*;
@Repository
public interface SolutionStepRepository extends JpaRepository<SolutionStep,UUID> {}
