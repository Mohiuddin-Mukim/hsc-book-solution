package bd.hscsolution.student.offline;

import androidx.annotation.NonNull;
import androidx.room.Entity;

@Entity(tableName="offline_content", primaryKeys={"questionId","languageCode","versionId"})
public final class OfflineContentEntity {
    @NonNull public String questionId="";
    @NonNull public String languageCode="";
    @NonNull public String versionId="";
    public String json;
    public long byteSize;
    public String checksumSha256;
    public String sourceChecksumSha256;
    public long savedAtEpochMs;

    public OfflineContentEntity() {}
}
