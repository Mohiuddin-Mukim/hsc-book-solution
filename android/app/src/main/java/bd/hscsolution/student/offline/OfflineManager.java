package bd.hscsolution.student.offline;

import android.content.Context;
import bd.hscsolution.student.AppDatabase;
import java.io.File;
import java.util.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;

public final class OfflineManager {
    private OfflineManager() {}

    public static long storageBytes(Context context) {
        AppDatabase db = AppDatabase.get(context);
        Set<String> countedFiles = new HashSet<>();
        Set<String> countedContent = new HashSet<>();
        long total = 0;
        for (OfflineManifestEntity manifest : uniqueCompleteManifests(db)) {
            for (OfflineResourceEntity resource : db.offlineResourceDao().byManifest(manifest.manifestId)) {
                if (resource.localPath != null && countedFiles.add(resource.localPath)) {
                    File file = new File(resource.localPath);
                    if (file.isFile()) total += file.length();
                }
                if ("QUESTION".equals(resource.resourceType)
                        && countedContent.add(resource.resourceId + "|" + manifest.languageCode + "|" + resource.resourceVersion)) {
                    OfflineContentEntity content = db.offlineContentDao().find(
                            resource.resourceId, manifest.languageCode, resource.resourceVersion);
                    if (content != null) total += Math.max(0, content.byteSize);
                }
            }
        }
        return total;
    }

    public static boolean isManifestIntact(AppDatabase db, String manifestId) {
        OfflineManifestEntity manifest = db.offlineManifestDao().find(manifestId);
        if (manifest == null || !manifest.complete) return false;
        try {
            for (OfflineResourceEntity resource : db.offlineResourceDao().byManifest(manifestId)) {
                if (!resource.required) continue;
                if (!resource.complete) return false;
                if ("QUESTION".equals(resource.resourceType)) {
                    OfflineContentEntity content = db.offlineContentDao().find(
                            resource.resourceId, manifest.languageCode, resource.resourceVersion);
                    if (content == null || content.json == null || content.json.isBlank()
                            || !Objects.equals(content.sourceChecksumSha256, resource.checksumSha256)) return false;
                    if (!matches(content.json.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                            content.checksumSha256, content.byteSize)) return false;
                    if (resource.localPath != null
                            && !verifyFile(resource.localPath, resource.checksumSha256, resource.byteSize)) return false;
                } else if ("MEDIA".equals(resource.resourceType)) {
                    if (resource.localPath == null
                            || !verifyFile(resource.localPath, resource.checksumSha256, resource.byteSize)) return false;
                } else {
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean verifyFile(String path, String checksum, long size) throws Exception {
        Path file = Paths.get(path);
        if (!Files.isRegularFile(file) || (size > 0 && Files.size(file) != size)) return false;
        return matches(Files.readAllBytes(file), checksum, size);
    }

    private static boolean matches(byte[] bytes, String checksum, long size) throws Exception {
        if (size > 0 && bytes.length != size) return false;
        if (checksum == null || checksum.isBlank()) return false;
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
        StringBuilder hex = new StringBuilder();
        for (byte b : digest) hex.append(String.format(Locale.ROOT, "%02x", b));
        return checksum.equalsIgnoreCase(hex.toString());
    }

    public static List<OfflineManifestEntity> uniqueCompleteManifests(AppDatabase db) {
        LinkedHashMap<String, OfflineManifestEntity> unique = new LinkedHashMap<>();
        for (OfflineManifestEntity item : db.offlineManifestDao().completeManifests()) {
            if (item.manifestId != null) unique.putIfAbsent(item.manifestId, item);
        }
        return new ArrayList<>(unique.values());
    }

    public static void deleteManifest(Context context, String manifestId) {
        AppDatabase db = AppDatabase.get(context);
        List<OfflineResourceEntity> resources = db.offlineResourceDao().byManifest(manifestId);
        for (OfflineResourceEntity resource : resources) {
            if (resource.localPath == null) continue;
            if ("MEDIA".equals(resource.resourceType)) {
                int refs = db.offlineResourceDao().mediaReferenceCount(resource.resourceId);
                if (refs <= 1) new File(resource.localPath).delete();
            } else if ("QUESTION".equals(resource.resourceType)) {
                int refs = db.offlineResourceDao().pathReferenceCount(resource.localPath, manifestId);
                if (refs == 0) new File(resource.localPath).delete();
            }
        }
        for (OfflineResourceEntity resource : resources) {
            if ("QUESTION".equals(resource.resourceType)
                    && db.offlineResourceDao().questionVersionReferenceCount(
                    resource.resourceId, resource.resourceVersion, manifestId) == 0) {
                db.offlineContentDao().delete(resource.resourceId, findLanguage(db, manifestId), resource.resourceVersion);
            }
        }
        db.offlineResourceDao().deleteByManifest(manifestId);
        db.offlineManifestDao().delete(manifestId);
    }

    private static String findLanguage(AppDatabase db, String manifestId) {
        OfflineManifestEntity manifest = db.offlineManifestDao().find(manifestId);
        return manifest == null || manifest.languageCode == null ? "bn" : manifest.languageCode;
    }
}