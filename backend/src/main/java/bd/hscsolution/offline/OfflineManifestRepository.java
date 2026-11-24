package bd.hscsolution.offline;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface OfflineManifestRepository extends JpaRepository<OfflineManifestEntity, UUID> {
    Optional<OfflineManifestEntity> findTopByScopeTypeAndScopeIdAndLanguageCodeOrderByVersionNoDesc(
            String scopeType, UUID scopeId, String languageCode);
}
