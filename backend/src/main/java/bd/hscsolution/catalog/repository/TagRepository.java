package bd.hscsolution.catalog.repository;
import bd.hscsolution.catalog.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public interface TagRepository extends JpaRepository<Tag,UUID> {}
