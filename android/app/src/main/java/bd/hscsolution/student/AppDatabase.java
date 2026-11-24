package bd.hscsolution.student;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.*;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import bd.hscsolution.student.offline.*;

@Database(entities = {
        OfflineManifestEntity.class,
        OfflineResourceEntity.class,
        OfflineContentEntity.class
}, version = 5, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    public abstract OfflineManifestDao offlineManifestDao();
    public abstract OfflineResourceDao offlineResourceDao();
    public abstract OfflineContentDao offlineContentDao();

    private static volatile AppDatabase INSTANCE;

    public static AppDatabase get(Context context) {
        if (INSTANCE == null) synchronized (AppDatabase.class) {
            if (INSTANCE == null) INSTANCE = Room.databaseBuilder(
                    context.getApplicationContext(), AppDatabase.class, "hsc_academic.db")
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .build();
        }
        return INSTANCE;
    }

    static final Migration MIGRATION_4_5 = new Migration(4, 5) {
        @Override public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE offline_content ADD COLUMN sourceChecksumSha256 TEXT");
        }
    };

    static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE offline_content RENAME TO offline_content_old");
            db.execSQL("CREATE TABLE IF NOT EXISTS offline_content (" +
                    "questionId TEXT NOT NULL, languageCode TEXT NOT NULL, versionId TEXT NOT NULL," +
                    "json TEXT, byteSize INTEGER NOT NULL DEFAULT 0, checksumSha256 TEXT," +
                    "savedAtEpochMs INTEGER NOT NULL DEFAULT 0," +
                    "PRIMARY KEY(questionId, languageCode, versionId))");
            db.execSQL("INSERT OR REPLACE INTO offline_content " +
                    "(questionId, languageCode, versionId, json, byteSize, checksumSha256, savedAtEpochMs) " +
                    "SELECT questionId, languageCode, COALESCE(versionId, ''), json, byteSize, checksumSha256, savedAtEpochMs " +
                    "FROM offline_content_old");
            db.execSQL("DROP TABLE offline_content_old");
        }
    };

    static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE offline_manifest ADD COLUMN scopeTitle TEXT");
            db.execSQL("DELETE FROM offline_manifest WHERE id NOT IN (SELECT MAX(id) FROM offline_manifest GROUP BY manifestId)");
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_offline_manifest_manifestId ON offline_manifest(manifestId)");
        }
    };

    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE offline_manifest ADD COLUMN scopeType TEXT");
            db.execSQL("ALTER TABLE offline_manifest ADD COLUMN scopeId TEXT");
            db.execSQL("ALTER TABLE offline_manifest ADD COLUMN languageCode TEXT NOT NULL DEFAULT 'bn'");
            db.execSQL("ALTER TABLE offline_manifest ADD COLUMN versionNo INTEGER NOT NULL DEFAULT 1");
            db.execSQL("ALTER TABLE offline_manifest ADD COLUMN checksumSha256 TEXT");
            db.execSQL("ALTER TABLE offline_manifest ADD COLUMN totalBytes INTEGER NOT NULL DEFAULT 0");
            db.execSQL("ALTER TABLE offline_manifest ADD COLUMN downloadedBytes INTEGER NOT NULL DEFAULT 0");
            db.execSQL("ALTER TABLE offline_manifest ADD COLUMN accessUntil TEXT");
            db.execSQL("CREATE TABLE IF NOT EXISTS offline_resource (" +
                    "manifestId TEXT NOT NULL, resourceType TEXT NOT NULL, resourceId TEXT NOT NULL," +
                    "resourceVersion TEXT, downloadUrl TEXT, byteSize INTEGER NOT NULL DEFAULT 0," +
                    "checksumSha256 TEXT, mimeType TEXT, relativePath TEXT, required INTEGER NOT NULL DEFAULT 1," +
                    "attachmentRole TEXT, displayOrder INTEGER, caption TEXT, altText TEXT," +
                    "complete INTEGER NOT NULL DEFAULT 0, localPath TEXT," +
                    "PRIMARY KEY(manifestId, resourceType, resourceId))");
            db.execSQL("CREATE TABLE IF NOT EXISTS offline_content (" +
                    "questionId TEXT NOT NULL, languageCode TEXT NOT NULL, versionId TEXT, json TEXT," +
                    "byteSize INTEGER NOT NULL DEFAULT 0, checksumSha256 TEXT, savedAtEpochMs INTEGER NOT NULL DEFAULT 0," +
                    "PRIMARY KEY(questionId, languageCode))");
        }
    };
}
