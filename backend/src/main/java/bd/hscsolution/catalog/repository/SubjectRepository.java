package bd.hscsolution.catalog.repository;
import bd.hscsolution.catalog.entity.Subject; 
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public interface SubjectRepository extends JpaRepository<Subject,UUID> {}
