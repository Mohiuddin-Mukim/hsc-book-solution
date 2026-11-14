package bd.hscsolution.media;

import java.io.*;
import java.nio.file.Path;
import org.springframework.stereotype.Service;

@Service
public class MediaStorageRouter {
    private final MediaProperties properties;
    private final LocalMediaStorage local;
    private final S3MediaStorage s3;

    public MediaStorageRouter(MediaProperties properties, LocalMediaStorage local, S3MediaStorage s3) {
        this.properties = properties; this.local = local; this.s3 = s3;
    }

    public String backend() { return "s3".equalsIgnoreCase(properties.backend()) ? "S3" : "LOCAL"; }
    public void put(String key, Path source, String contentType) throws IOException {
        if ("S3".equals(backend())) s3.put(key, source, contentType); else local.put(key, source, contentType);
    }
    public InputStream open(String key) throws IOException {
        return "S3".equals(backend()) ? s3.open(key) : local.open(key);
    }
    public void delete(String key) throws IOException {
        if ("S3".equals(backend())) s3.delete(key); else local.delete(key);
    }
    public boolean exists(String key) throws IOException {
        return "S3".equals(backend()) ? s3.exists(key) : local.exists(key);
    }
}
