package bd.hscsolution.media;

import java.util.*;

public final class MediaDtos {
    private MediaDtos() {}

    public record MediaAssetResponse(
            UUID id, String role, int order, String caption, String altText,
            int width, int height, long byteSize, String mimeType, String checksumSha256,
            String lifecycleState, String deliveryUrl, long usageCount) {}

    public record AttachRequest(UUID assetId, String role, Integer displayOrder, String caption, String altText) {}
    public record ReorderItem(UUID assetId, int displayOrder) {}
    public record ReplacementResponse(MediaAssetResponse oldAsset, MediaAssetResponse newAsset) {}
}
