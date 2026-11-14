package bd.hscsolution.media;

import java.io.*;
import java.nio.file.*;
import org.springframework.stereotype.Component;

@Component
public class LocalMediaStorage implements MediaStorage {
    private final Path root;

    public LocalMediaStorage(MediaProperties p) {
        this.root = Paths.get(p.localRoot()).toAbsolutePath().normalize();
        try { Files.createDirectories(root); } catch (IOException e) {
            throw new IllegalStateException("Cannot create local media root", e);
        }
    }

    @Override public void put(String key, Path source, String contentType) throws IOException {
        Path target = safe(key);
        Files.createDirectories(target.getParent());
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
    }

    @Override public InputStream open(String key) throws IOException {
        return Files.newInputStream(safe(key), StandardOpenOption.READ);
    }

    @Override public void delete(String key) throws IOException {
        Files.deleteIfExists(safe(key));
    }

    @Override public boolean exists(String key) throws IOException {
        return Files.exists(safe(key));
    }

    private Path safe(String key) {
        Path resolved = root.resolve(key).normalize();
        if (!resolved.startsWith(root)) throw new SecurityException("Unsafe media object key");
        return resolved;
    }
}
