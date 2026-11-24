package bd.hscsolution.student.offline;

import androidx.room.*;
import java.util.List;

@Dao
public interface OfflineManifestDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) long insert(OfflineManifestEntity item);

    @Query("SELECT * FROM offline_manifest ORDER BY updatedAtEpochMs DESC")
    List<OfflineManifestEntity> latest();

    @Query("SELECT * FROM offline_manifest WHERE manifestId=:manifestId LIMIT 1")
    OfflineManifestEntity find(String manifestId);

    @Query("DELETE FROM offline_manifest WHERE manifestId=:manifestId")
    int delete(String manifestId);

    @Query("SELECT * FROM offline_manifest WHERE complete=1 ORDER BY updatedAtEpochMs DESC")
    List<OfflineManifestEntity> completeManifests();

    @Query("SELECT * FROM offline_manifest WHERE scopeType=:type AND scopeId=:scopeId AND languageCode=:language AND complete=1 LIMIT 1")
    OfflineManifestEntity findCompleteScope(String type, String scopeId, String language);

    @Query("SELECT * FROM offline_manifest WHERE scopeType=:type AND scopeId=:scopeId AND languageCode=:language AND complete=1")
    List<OfflineManifestEntity> completeByScope(String type, String scopeId, String language);

    @Query("DELETE FROM offline_manifest")
    void clear();
}
