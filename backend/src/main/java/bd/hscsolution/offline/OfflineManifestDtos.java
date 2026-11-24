package bd.hscsolution.offline;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public final class OfflineManifestDtos {
    private OfflineManifestDtos() {}

    public record ManifestResponse(
            String manifestId,
            long versionNo,
            String scopeType,
            UUID scopeId,
            String scopeTitle,
            String languageCode,
            String checksumSha256,
            long totalBytes,
            OffsetDateTime generatedAt,
            OffsetDateTime accessUntil,
            List<Item> items,
            MathAssets mathAssets) {}

    public record Item(
            String resourceType,
            UUID resourceId,
            String resourceVersion,
            String downloadUrl,
            long byteSize,
            String checksumSha256,
            String mimeType,
            String relativePath,
            boolean required,
            String attachmentRole,
            Integer displayOrder,
            String caption,
            String altText) {}

    public record MathAssets(
            String renderer,
            String version,
            String localAssetPath,
            long byteSize,
            String checksumSha256,
            boolean required) {}
}
