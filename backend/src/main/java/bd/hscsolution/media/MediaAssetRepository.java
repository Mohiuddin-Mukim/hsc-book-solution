package bd.hscsolution.media;

import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface MediaAssetRepository extends JpaRepository<MediaAsset, UUID> {
    @Query("select count(q) from QuestionMediaAttachment q where q.mediaAsset.id=:id")
    long questionUsage(UUID id);
    @Query("select count(s) from SolutionStepMediaAttachment s where s.mediaAsset.id=:id")
    long stepUsage(UUID id);
}
