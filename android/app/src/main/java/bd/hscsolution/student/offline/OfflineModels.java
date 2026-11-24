package bd.hscsolution.student.offline;

import java.util.List;

public final class OfflineModels {
    private OfflineModels() {}

    public record ManifestResponse(
            String manifestId, long versionNo, String scopeType, String scopeId,
            String languageCode, String checksumSha256, long totalBytes,
            String generatedAt, String accessUntil, List<Item> items, MathAssets mathAssets,
            String scopeTitle) {}

    public record Item(
            String resourceType, String resourceId, String resourceVersion,
            String downloadUrl, long byteSize, String checksumSha256, String mimeType,
            String relativePath, boolean required, String attachmentRole, Integer displayOrder,
            String caption, String altText) {}

    public record MathAssets(String renderer, String version, String localAssetPath,
                             long byteSize, String checksumSha256, boolean required) {}
}
