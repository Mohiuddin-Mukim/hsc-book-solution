package bd.hscsolution.catalog.repository;
import bd.hscsolution.catalog.entity.Paper;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
// সঠিক:
import org.springframework.data.domain.Pageable;


import java.util.*;
@Repository
public interface PaperRepository extends JpaRepository<Paper,UUID> {
    Page<Paper> findBySubjectIdOrderByDisplayOrderAsc(UUID subjectId, Pageable pageable);
}
