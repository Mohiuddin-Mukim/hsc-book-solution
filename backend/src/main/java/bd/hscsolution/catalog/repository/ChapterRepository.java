package bd.hscsolution.catalog.repository;
import bd.hscsolution.catalog.entity.Chapter; import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.*;
@Repository
public interface ChapterRepository extends JpaRepository<Chapter,UUID> {}
