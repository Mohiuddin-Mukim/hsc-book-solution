package bd.hscsolution.media;

import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SolutionStepMediaAttachmentRepository extends JpaRepository<SolutionStepMediaAttachment, SolutionStepMediaAttachment.Key> {
    List<SolutionStepMediaAttachment> findBySolutionStepIdOrderByDisplayOrderAsc(UUID stepId);
    Optional<SolutionStepMediaAttachment> findBySolutionStepIdAndMediaAssetId(UUID stepId, UUID assetId);
    void deleteBySolutionStepIdAndMediaAssetId(UUID stepId, UUID assetId);
}
