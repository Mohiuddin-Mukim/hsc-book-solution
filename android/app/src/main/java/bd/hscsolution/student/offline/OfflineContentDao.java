package bd.hscsolution.student.offline;

import androidx.room.*;
import java.util.List;

@Dao
public interface OfflineContentDao {
    @Insert(onConflict=OnConflictStrategy.REPLACE) void upsert(OfflineContentEntity item);

    @Query("SELECT * FROM offline_content WHERE questionId=:questionId AND languageCode=:language AND versionId=:versionId LIMIT 1")
    OfflineContentEntity find(String questionId, String language, String versionId);

    @Query("DELETE FROM offline_content WHERE questionId=:questionId AND languageCode=:language AND versionId=:versionId")
    int delete(String questionId, String language, String versionId);

    @Query("SELECT * FROM offline_content")
    List<OfflineContentEntity> all();
}
