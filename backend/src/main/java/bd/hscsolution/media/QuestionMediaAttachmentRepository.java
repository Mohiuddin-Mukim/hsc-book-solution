package bd.hscsolution.media;

import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionMediaAttachmentRepository extends JpaRepository<QuestionMediaAttachment, QuestionMediaAttachment.Key> {
    List<QuestionMediaAttachment> findByQuestionVersionIdOrderByDisplayOrderAsc(UUID questionVersionId);
    Optional<QuestionMediaAttachment> findByQuestionVersionIdAndMediaAssetId(UUID versionId, UUID assetId);
    void deleteByQuestionVersionIdAndMediaAssetId(UUID versionId, UUID assetId);
}
