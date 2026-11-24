package bd.hscsolution.offline;

import jakarta.persistence.*;
import lombok.Getter;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Getter
@Table(name = "offline_manifest",
       uniqueConstraints = @UniqueConstraint(columnNames = {"scope_type", "scope_id", "version_no"}))
public class OfflineManifestEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    public UUID id;

    @Column(name="version_no", nullable=false)
    public long versionNo;

    @Column(name="scope_type", nullable=false, length=30)
    public String scopeType;

    @Column(name="scope_id")
    public UUID scopeId;

    @Column(name="language_code", nullable=false, length=10)
    public String languageCode;

    @Column(name="checksum_sha256", nullable=false, length=64)
    public String checksumSha256;

    @Column(name="total_bytes", nullable=false)
    public long totalBytes;

    @Column(name="access_until")
    public OffsetDateTime accessUntil;

    @Enumerated(EnumType.STRING)
    @Column(nullable=false, length=20)
    public Status status = Status.READY;

    @Column(name="generated_at", nullable=false)
    public OffsetDateTime generatedAt = OffsetDateTime.now();

    public enum Status { READY, EXPIRED }
}
