package bd.hscsolution.student;
import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import bd.hscsolution.student.offline.OfflineManifestDao;
import bd.hscsolution.student.offline.OfflineManifestEntity;

@Database(entities = {OfflineManifestEntity.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    public abstract OfflineManifestDao offlineManifestDao();
    private static volatile AppDatabase INSTANCE;
    public static AppDatabase get(Context context) {
        if (INSTANCE == null) synchronized (AppDatabase.class) {
            if (INSTANCE == null) INSTANCE = Room.databaseBuilder(
                    context.getApplicationContext(), AppDatabase.class, "hsc_academic.db").build();
        }
        return INSTANCE;
    }
}
