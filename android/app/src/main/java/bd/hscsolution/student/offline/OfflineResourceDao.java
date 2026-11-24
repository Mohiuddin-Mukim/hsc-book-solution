package bd.hscsolution.student.offline;

import androidx.room.*;
import java.util.List;

@Dao
public interface OfflineResourceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) void upsert(OfflineResourceEntity item);

    @Insert(onConflict = OnConflictStrategy.REPLACE) void upsertAll(List<OfflineResourceEntity> items);

    @Query("SELECT * FROM offline_resource WHERE manifestId=:manifestId")
    List<OfflineResourceEntity> byManifest(String manifestId);

    @Query("SELECT * FROM offline_resource WHERE manifestId=:manifestId AND resourceType=:type AND resourceId=:id LIMIT 1")
    OfflineResourceEntity find(String manifestId, String type, String id);

    @Query("SELECT * FROM offline_resource WHERE resourceType='MEDIA' AND resourceId=:assetId AND complete=1 LIMIT 1")
    OfflineResourceEntity anyCompleteMedia(String assetId);

    @Query("SELECT COUNT(*) FROM offline_resource WHERE resourceType='MEDIA' AND resourceId=:assetId AND complete=1")
    int mediaReferenceCount(String assetId);

    @Query("SELECT COUNT(*) FROM offline_resource WHERE resourceType='QUESTION' AND resourceId=:questionId AND manifestId!=:manifestId AND complete=1")
    int questionReferenceCount(String questionId, String manifestId);

    @Query("SELECT COUNT(*) FROM offline_resource WHERE resourceType='QUESTION' AND resourceId=:questionId AND resourceVersion=:version AND manifestId!=:manifestId AND complete=1")
    int questionVersionReferenceCount(String questionId, String version, String manifestId);

    @Query("SELECT COUNT(*) FROM offline_resource WHERE localPath=:localPath AND manifestId!=:manifestId AND complete=1")
    int pathReferenceCount(String localPath, String manifestId);

    @Query("UPDATE offline_resource SET complete=:complete, localPath=:localPath WHERE manifestId=:manifestId AND resourceType=:type AND resourceId=:id")
    int markComplete(String manifestId, String type, String id, boolean complete, String localPath);

    @Query("DELETE FROM offline_resource WHERE manifestId=:manifestId")
    int deleteByManifest(String manifestId);
}
