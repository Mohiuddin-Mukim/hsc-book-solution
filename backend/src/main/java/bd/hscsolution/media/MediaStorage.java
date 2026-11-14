package bd.hscsolution.media;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

public interface MediaStorage {
    void put(String objectKey, Path source, String contentType) throws IOException;
    InputStream open(String objectKey) throws IOException;
    void delete(String objectKey) throws IOException;
    boolean exists(String objectKey) throws IOException;
}
