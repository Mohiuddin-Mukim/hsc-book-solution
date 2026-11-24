package bd.hscsolution.offline;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix="offline")
public record OfflineProperties(
        boolean enforceEntitlement,
        int defaultGraceMinutes,
        String mathRenderer,
        String mathRendererVersion,
        String mathAssetPath,
        long mathAssetBytes,
        String mathAssetSha256) {}
