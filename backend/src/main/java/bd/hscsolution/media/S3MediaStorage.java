package bd.hscsolution.media;

import java.io.*;
import java.nio.file.Path;
import java.util.Optional;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

@Component
public class S3MediaStorage implements MediaStorage {
    private final MediaProperties p;
    private final S3Client client;

    public S3MediaStorage(MediaProperties p) {
        this.p = p;
        if (!"s3".equalsIgnoreCase(p.backend())) {
            this.client = null;
            return;
        }
        var builder = S3Client.builder().region(Region.of(p.s3().region()));
        if (p.s3().endpoint() != null && !p.s3().endpoint().isBlank()) {
            builder.endpointOverride(java.net.URI.create(p.s3().endpoint()));
        }
        builder.forcePathStyle(p.s3().pathStyleAccess());
        this.client = builder.build();
    }

    public boolean enabled() { return client != null; }

    @Override public void put(String key, Path source, String contentType) throws IOException {
        ensure();
        try {
            client.putObject(PutObjectRequest.builder().bucket(p.s3().bucket()).key(key).contentType(contentType).build(),
                    RequestBody.fromFile(source));
        } catch (S3Exception e) {
            throw new IOException("S3 upload failed: " + e.awsErrorDetails().errorMessage(), e);
        }
    }

    @Override public InputStream open(String key) throws IOException {
        ensure();
        try {
            return client.getObject(GetObjectRequest.builder().bucket(p.s3().bucket()).key(key).build());
        } catch (NoSuchKeyException e) {
            throw new FileNotFoundException("Media object not found");
        } catch (S3Exception e) {
            throw new IOException("S3 get failed: " + e.awsErrorDetails().errorMessage(), e);
        }
    }

    @Override public void delete(String key) throws IOException {
        ensure();
        try {
            client.deleteObject(DeleteObjectRequest.builder().bucket(p.s3().bucket()).key(key).build());
        } catch (S3Exception e) {
            throw new IOException("S3 delete failed", e);
        }
    }

    @Override public boolean exists(String key) throws IOException {
        ensure();
        try {
            client.headObject(HeadObjectRequest.builder().bucket(p.s3().bucket()).key(key).build());
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        } catch (S3Exception e) {
            if (e.statusCode() == 404) return false;
            throw new IOException("S3 head failed", e);
        }
    }

    private void ensure() {
        if (client == null) throw new IllegalStateException("S3 storage is not enabled");
    }
}
