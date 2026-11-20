package bd.hscsolution.student.offline;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;
@Dao public interface OfflineManifestDao {
    @Insert long insert(OfflineManifestEntity item);
    @Query("SELECT * FROM offline_manifest ORDER BY updatedAtEpochMs DESC") List<OfflineManifestEntity> latest();
    @Query("DELETE FROM offline_manifest") void clear();
}
