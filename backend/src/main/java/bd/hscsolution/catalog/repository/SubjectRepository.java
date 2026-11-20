package bd.hscsolution.catalog.repository;
import bd.hscsolution.catalog.entity.Subject;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
// সঠিক:
import org.springframework.data.domain.Pageable;


import java.util.*;

@Repository
public interface SubjectRepository extends JpaRepository<Subject,UUID> {
    Page<Subject> findByActiveTrueOrderByDisplayOrderAsc(Pageable pageable);
}
