package bd.hscsolution.catalog.repository;
import bd.hscsolution.catalog.entity.Chapter;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
// সঠিক:
import org.springframework.data.domain.Pageable;


import java.util.*;
@Repository
public interface ChapterRepository extends JpaRepository<Chapter,UUID> {
    Page<Chapter> findByPaperIdOrderByDisplayOrderAsc(UUID paperId, Pageable pageable);
}
