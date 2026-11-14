package bd.hscsolution.media;

import jakarta.persistence.*;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.UUID;
import static bd.hscsolution.media.MediaEnums.LifecycleState;

@Entity
@Getter
@Table(name="media_asset")
public class MediaAsset {
    @Id @GeneratedValue(strategy=GenerationType.UUID) public UUID id;
    @Column(name="storage_key",nullable=false,unique=true) public String storageKey;
    @Column(name="mime_type",nullable=false,length=120) public String mimeType;
    @Column(name="file_name",nullable=false,length=255) public String fileName;
    @Column(name="byte_size",nullable=false) public long byteSize;
    @Column(name="checksum_sha256",nullable=false,length=64) public String checksumSha256;
    public Integer width;
    public Integer height;
    @Column(name="alt_text",length=500) public String altText;
    @Column(columnDefinition="text") public String caption;
    @Column(name="created_by") public UUID createdBy;
    @Column(name="created_at",nullable=false) public OffsetDateTime createdAt;
    @Column(name="updated_at",nullable=false) public OffsetDateTime updatedAt;
    @Enumerated(EnumType.STRING) @Column(name="lifecycle_state",nullable=false,length=20) public LifecycleState lifecycleState;
    @Column(name="storage_backend",nullable=false,length=20) public String storageBackend;
    @Column(name="original_extension",length=10) public String originalExtension;

    @PrePersist void prePersist() {
        var now=OffsetDateTime.now(); if(createdAt==null)createdAt=now; updatedAt=now;
        if(lifecycleState==null)lifecycleState=LifecycleState.QUARANTINED;
    }
    @PreUpdate void preUpdate(){updatedAt=OffsetDateTime.now();}
}
