package bd.hscsolution.student.offline;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.Index;

@Entity(tableName = "offline_manifest", indices = {@Index(value = {"manifestId"}, unique = true)})
public final class OfflineManifestEntity {
    @PrimaryKey(autoGenerate = true) public long id;
    public String manifestId;
    public String scopeType;
    public String scopeId;
    public String scopeTitle;
    public String languageCode;
    public long versionNo;
    public String checksumSha256;
    public long totalBytes;
    public long downloadedBytes;
    public long updatedAtEpochMs;
    public String accessUntil;
    public boolean complete;

    public OfflineManifestEntity(String manifestId, String scopeType, String scopeId, String languageCode,
                                 long versionNo, String checksumSha256, long totalBytes,
                                 long downloadedBytes, long updatedAtEpochMs, String accessUntil,
                                 boolean complete) {
        this.manifestId=manifestId; this.scopeType=scopeType; this.scopeId=scopeId;
        this.languageCode=languageCode; this.versionNo=versionNo; this.checksumSha256=checksumSha256;
        this.totalBytes=totalBytes; this.downloadedBytes=downloadedBytes;
        this.updatedAtEpochMs=updatedAtEpochMs; this.accessUntil=accessUntil; this.complete=complete;
    }
}
