package bd.hscsolution.catalog.repository;
import bd.hscsolution.catalog.entity.Book; import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.*;
@Repository
public interface BookRepository extends JpaRepository<Book,UUID> {}
