package bd.hscsolution.catalog.repository;
import bd.hscsolution.catalog.entity.Topic;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
// সঠিক:
import org.springframework.data.domain.Pageable;


import java.util.*;

@Repository
public interface TopicRepository extends JpaRepository<Topic,UUID> {
    Page<Topic> findByChapterIdOrderByDisplayOrderAsc(UUID chapterId, Pageable pageable);
}
