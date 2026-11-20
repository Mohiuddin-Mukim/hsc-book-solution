package bd.hscsolution.student.offline;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "offline_manifest")
public final class OfflineManifestEntity {
    @PrimaryKey(autoGenerate = true) public long id;
    public String manifestId;
    public String contentVersion;
    public long updatedAtEpochMs;
    public boolean complete;
    public OfflineManifestEntity(String manifestId, String contentVersion, long updatedAtEpochMs, boolean complete) {
        this.manifestId = manifestId; this.contentVersion = contentVersion;
        this.updatedAtEpochMs = updatedAtEpochMs; this.complete = complete;
    }
}
