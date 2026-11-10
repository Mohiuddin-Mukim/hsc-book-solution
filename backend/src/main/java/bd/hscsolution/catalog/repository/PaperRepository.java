package bd.hscsolution.catalog.repository;
import bd.hscsolution.catalog.entity.Paper; import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.*;
@Repository
public interface PaperRepository extends JpaRepository<Paper,UUID> {}
