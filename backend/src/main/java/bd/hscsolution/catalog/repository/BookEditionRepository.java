package bd.hscsolution.catalog.repository;
import bd.hscsolution.catalog.entity.BookEdition; import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.*;
@Repository
public interface BookEditionRepository extends JpaRepository<BookEdition,UUID> {}
