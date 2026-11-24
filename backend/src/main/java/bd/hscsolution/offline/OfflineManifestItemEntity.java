package bd.hscsolution.offline;

import jakarta.persistence.*;
import lombok.Getter;
import java.util.UUID;

@Entity
@Getter
@Table(name="offline_manifest_item",
       uniqueConstraints=@UniqueConstraint(columnNames={"manifest_id","resource_type","resource_id"}))
public class OfflineManifestItemEntity {
    @Id @GeneratedValue(strategy=GenerationType.UUID)
    public UUID id;

    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="manifest_id")
    public OfflineManifestEntity manifest;

    @Column(name="resource_type", nullable=false, length=40)
    public String resourceType;

    @Column(name="resource_id", nullable=false)
    public UUID resourceId;

    @Column(name="resource_version", nullable=false)
    public long resourceVersion;

    @Column(name="resource_version_label")
    public String resourceVersionLabel;

    @Column(name="download_url", nullable=false, columnDefinition="text")
    public String downloadUrl;

    @Column(name="byte_size", nullable=false)
    public long byteSize;

    @Column(name="checksum_sha256", length=64)
    public String checksumSha256;

    @Column(name="mime_type", length=120)
    public String mimeType;

    @Column(name="relative_path", nullable=false, length=500)
    public String relativePath;

    @Column(name="required", nullable=false)
    public boolean required = true;

    @Column(name="attachment_role", length=30)
    public String attachmentRole;

    @Column(name="display_order")
    public Integer displayOrder;

    @Column(columnDefinition="text")
    public String caption;

    @Column(name="alt_text", length=500)
    public String altText;
}
