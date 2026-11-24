package bd.hscsolution.student.offline;

import androidx.annotation.NonNull;
import androidx.room.Entity;

@Entity(tableName="offline_resource", primaryKeys={"manifestId","resourceType","resourceId"})
public final class OfflineResourceEntity {
    @NonNull public String manifestId="";
    @NonNull public String resourceType="";
    @NonNull public String resourceId="";
    public String resourceVersion;
    public String downloadUrl;
    public long byteSize;
    public String checksumSha256;
    public String mimeType;
    public String relativePath;
    public boolean required;
    public String attachmentRole;
    public Integer displayOrder;
    public String caption;
    public String altText;
    public boolean complete;
    public String localPath;

    public OfflineResourceEntity() {}
}
