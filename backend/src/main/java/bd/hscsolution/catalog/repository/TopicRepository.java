package bd.hscsolution.catalog.repository;
import bd.hscsolution.catalog.entity.Topic; import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public interface TopicRepository extends JpaRepository<Topic,UUID> {}
