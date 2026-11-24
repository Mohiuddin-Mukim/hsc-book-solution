package bd.hscsolution.offline;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface OfflineManifestItemRepository extends JpaRepository<OfflineManifestItemEntity, UUID> {
    List<OfflineManifestItemEntity> findByManifestId(UUID manifestId);
}
