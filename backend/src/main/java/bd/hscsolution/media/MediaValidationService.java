package bd.hscsolution.media;

import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.Locale;
import javax.imageio.ImageIO;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class MediaValidationService {
    private final MediaProperties p;

    public MediaValidationService(MediaProperties p) {
        this.p = p;
        ImageIO.scanForPlugins();
    }

    public ValidatedMedia quarantine(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) throw bad("Empty media file");
        if (file.getSize() > p.maxBytes()) throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Media file is too large");
        String original = safeName(file.getOriginalFilename());
        String ext = extension(original);
        if (!p.allowedExtensions().contains(ext)) throw bad("File extension is not allowed");

        Path quarantine = Files.createDirectories(Path.of(p.quarantineRoot()).toAbsolutePath().normalize())
                .resolve(java.util.UUID.randomUUID() + "." + ext);
        try {
            file.transferTo(quarantine);
            long actual = Files.size(quarantine);
            if (actual <= 0 || actual > p.maxBytes()) throw bad("Invalid media byte size");

            String detected = detectSignature(quarantine);
            if (!p.allowedMimeTypes().contains(detected)) throw bad("File signature/MIME type is not allowed");
            if (!mimeMatchesExtension(detected, ext)) throw bad("File extension does not match actual file type");

            BufferedImage image = ImageIO.read(quarantine.toFile());
            if (image == null) throw bad("Corrupt or undecodable image");
            if (image.getWidth() <= 0 || image.getHeight() <= 0) throw bad("Invalid image dimensions");
            if (image.getWidth() > p.maxWidth() || image.getHeight() > p.maxHeight())
                throw bad("Image dimensions exceed the configured limit");

            String checksum = sha256(quarantine);
            return new ValidatedMedia(quarantine, original, detected, actual, image.getWidth(), image.getHeight(), checksum, ext);
        } catch (RuntimeException | IOException ex) {
            try { Files.deleteIfExists(quarantine); } catch (IOException ignored) {}
            throw ex;
        }
    }

    private String detectSignature(Path f) throws IOException {
        byte[] b = Files.readAllBytes(f);
        if (b.length >= 3 && (b[0]&255)==0xFF && (b[1]&255)==0xD8 && (b[2]&255)==0xFF) return "image/jpeg";
        if (b.length >= 8 && (b[0]&255)==0x89 && b[1]=='P' && b[2]=='N' && b[3]=='G' && (b[4]&255)==0x0D && (b[5]&255)==0x0A && (b[6]&255)==0x1A && (b[7]&255)==0x0A) return "image/png";
        if (b.length >= 12 && b[0]=='R' && b[1]=='I' && b[2]=='F' && b[3]=='F' && b[8]=='W' && b[9]=='E' && b[10]=='B' && b[11]=='P') return "image/webp";
        return "application/octet-stream";
    }

    private boolean mimeMatchesExtension(String mime, String ext) {
        return ("image/jpeg".equals(mime) && ("jpg".equals(ext) || "jpeg".equals(ext)))
            || ("image/png".equals(mime) && "png".equals(ext))
            || ("image/webp".equals(mime) && "webp".equals(ext));
    }

    private String sha256(Path pth) throws IOException {
        try (InputStream in = Files.newInputStream(pth)) {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] buf = new byte[8192]; int n;
            while ((n = in.read(buf)) >= 0) if (n > 0) md.update(buf, 0, n);
            return java.util.HexFormat.of().formatHex(md.digest());
        } catch (java.security.NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }

    private String safeName(String name) {
        String x = name == null ? "upload" : name.replace('\\','/').substring(name.replace('\\','/').lastIndexOf('/') + 1);
        return x.isBlank() ? "upload" : x.substring(0, Math.min(x.length(), 255));
    }

    private String extension(String name) {
        int i = name.lastIndexOf('.');
        return i < 0 ? "" : name.substring(i + 1).toLowerCase(Locale.ROOT);
    }

    private ResponseStatusException bad(String m) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, m);
    }

    public record ValidatedMedia(Path quarantine, String originalName, String mimeType, long byteSize,
                                 int width, int height, String checksumSha256, String extension) {}
}
