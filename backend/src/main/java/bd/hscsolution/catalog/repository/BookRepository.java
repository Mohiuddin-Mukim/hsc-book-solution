package bd.hscsolution.catalog.repository;
import bd.hscsolution.catalog.entity.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
// সঠিক:
import org.springframework.data.domain.Pageable;


import java.util.*;
@Repository
public interface BookRepository extends JpaRepository<Book,UUID> {
    Page<Book> findBySubjectIdOrderByTitleAsc(UUID subjectId, Pageable pageable);
}
