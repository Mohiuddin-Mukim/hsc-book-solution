package bd.hscsolution.catalog.repository;
import bd.hscsolution.catalog.entity.McqOption; import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.*;
@Repository
public interface McqOptionRepository extends JpaRepository<McqOption,UUID> {}
