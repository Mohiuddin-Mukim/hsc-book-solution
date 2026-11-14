package bd.hscsolution.media;

import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "hsc.media")
public record MediaProperties(
        String backend,
        String localRoot,
        String quarantineRoot,
        long maxBytes,
        int maxWidth,
        int maxHeight,
        Set<String> allowedMimeTypes,
        Set<String> allowedExtensions,
        S3 s3) {

    public record S3(
            String bucket,
            String region,
            String endpoint,
            boolean pathStyleAccess,
            long presignSeconds) {}

    public MediaProperties {
        backend = backend == null ? "local" : backend.trim().toLowerCase();
        localRoot = localRoot == null ? "./runtime/media" : localRoot;
        quarantineRoot = quarantineRoot == null ? "./runtime/media-quarantine" : quarantineRoot;
        allowedMimeTypes = allowedMimeTypes == null ? Set.of("image/jpeg","image/png","image/webp") : allowedMimeTypes;
        allowedExtensions = allowedExtensions == null ? Set.of("jpg","jpeg","png","webp") : allowedExtensions;
    }
}
