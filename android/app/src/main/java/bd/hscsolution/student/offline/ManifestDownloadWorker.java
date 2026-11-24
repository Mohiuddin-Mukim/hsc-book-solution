package bd.hscsolution.student.offline;

import android.content.Context;
import android.os.StatFs;
import androidx.annotation.NonNull;
import androidx.work.*;
import com.google.gson.*;
import bd.hscsolution.student.BuildConfig;
import bd.hscsolution.student.HscApplication;
import bd.hscsolution.student.AppDatabase;
import bd.hscsolution.student.auth.AuthManager;
import bd.hscsolution.student.auth.AuthTokens;
import bd.hscsolution.student.network.AuthInterceptor;
import okhttp3.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

public final class ManifestDownloadWorker extends Worker {
    public static final String KEY_SCOPE_TYPE = "scopeType";
    public static final String KEY_SCOPE_ID = "scopeId";
    public static final String KEY_LANGUAGE = "language";
    public static final String KEY_MANIFEST_ID = "manifestId";
    public static final String KEY_ERROR = "error";

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    public ManifestDownloadWorker(@NonNull Context appContext, @NonNull WorkerParameters params) {
        super(appContext, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            Context c = getApplicationContext();
            byte[] bundledMath;
            try (InputStream math = c.getAssets().open("mathjax/tex-chtml.js");
                 ByteArrayOutputStream mathBuffer = new ByteArrayOutputStream()) {
                byte[] mathChunk = new byte[32 * 1024];
                int mathRead;
                while ((mathRead = math.read(mathChunk)) != -1) mathBuffer.write(mathChunk, 0, mathRead);
                bundledMath = mathBuffer.toByteArray();
            }
            if (bundledMath.length < 100_000) {
                throw new IOException("Offline math renderer asset is incomplete");
            }

            AppDatabase db = AppDatabase.get(c);
            bd.hscsolution.student.content.ContentRepository repo =
                    ((HscApplication) c).container().contentRepository;

            String scopeType = getInputData().getString(KEY_SCOPE_TYPE);
            String scopeId = getInputData().getString(KEY_SCOPE_ID);
            String language = getInputData().getString(KEY_LANGUAGE);
            if (language == null || language.isBlank()) {
                language = "bn";
            }

            OfflineModels.ManifestResponse m = "BOOK".equals(scopeType)
                    ? repo.offlineBookManifest(scopeId, language)
                    : repo.offlineChapterManifest(scopeId, language);

            if (m.mathAssets() == null || (m.mathAssets().required()
                    && (!"MathJax".equalsIgnoreCase(m.mathAssets().renderer())
                    || m.mathAssets().version() == null || m.mathAssets().version().isBlank()
                    || m.mathAssets().localAssetPath() == null
                    || !m.mathAssets().localAssetPath().contains("tex-chtml.js")
                    || m.mathAssets().byteSize() != bundledMath.length
                    || m.mathAssets().checksumSha256() == null
                    || !m.mathAssets().checksumSha256().equalsIgnoreCase(sha256(bundledMath))))) {
                throw new IOException("Required offline math renderer is not declared or supported by this app.");
            }

            if (m.accessUntil() != null && !m.accessUntil().isBlank()
                    && !OffsetDateTime.parse(m.accessUntil()).isAfter(OffsetDateTime.now())) {
                return Result.failure(new Data.Builder().putString(KEY_ERROR,
                        "Offline access has expired. Connect to the internet to renew access.").build());
            }

            OfflineManifestEntity manifest = new OfflineManifestEntity(
                    m.manifestId(), m.scopeType(), m.scopeId(), m.languageCode(), m.versionNo(),
                    m.checksumSha256(), m.totalBytes(), 0, System.currentTimeMillis(), m.accessUntil(), false);
            OfflineManifestEntity oldManifest = db.offlineManifestDao().find(m.manifestId());
            manifest.scopeTitle = m.scopeTitle();
            if (oldManifest != null) {
                manifest.id = oldManifest.id;
            }
            db.offlineManifestDao().insert(manifest);

            List<OfflineResourceEntity> previousRows = db.offlineResourceDao().byManifest(m.manifestId());
            Map<String, OfflineResourceEntity> previousByKey = new HashMap<>();
            for (OfflineResourceEntity previous : previousRows) {
                previousByKey.put(previous.resourceType + ":" + previous.resourceId, previous);
            }
            List<OfflineResourceEntity> rows = new ArrayList<>();
            for (OfflineModels.Item item : m.items()) {
                OfflineResourceEntity r = new OfflineResourceEntity();
                r.manifestId = m.manifestId();
                r.resourceType = item.resourceType();
                r.resourceId = item.resourceId();
                r.resourceVersion = item.resourceVersion();
                r.downloadUrl = item.downloadUrl();
                r.byteSize = item.byteSize();
                r.checksumSha256 = item.checksumSha256();
                r.mimeType = item.mimeType();
                r.relativePath = item.relativePath();
                r.required = item.required();
                r.attachmentRole = item.attachmentRole();
                r.displayOrder = item.displayOrder();
                r.caption = item.caption();
                r.altText = item.altText();
                OfflineResourceEntity previous = previousByKey.get(r.resourceType + ":" + r.resourceId);
                if (previous != null && previous.complete
                        && Objects.equals(previous.checksumSha256, r.checksumSha256)
                        && previous.localPath != null && Files.exists(Paths.get(previous.localPath))) {
                    try {
                        verify(Paths.get(previous.localPath), r.checksumSha256, r.byteSize);
                        r.complete = true;
                        r.localPath = previous.localPath;
                    } catch (Exception invalidPreviousFile) {
                        r.complete = false;
                        r.localPath = null;
                    }
                }
                rows.add(r);
            }
            db.offlineResourceDao().upsertAll(rows);

            AuthManager auth = new AuthManager(c);
            OkHttpClient http = new OkHttpClient.Builder()
                    .addInterceptor(new AuthInterceptor(auth))
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(60, TimeUnit.SECONDS)
                    .build();

            Path root = Paths.get(c.getFilesDir().getAbsolutePath(), "offline");
            Files.createDirectories(root);
            long done = 0;
            Map<String, String> mediaPaths = new HashMap<>();

            for (OfflineResourceEntity r : rows) {
                if (isStopped()) {
                    return Result.failure();
                }

                if ("QUESTION".equals(r.resourceType)) {
                    OfflineContentEntity localContent = db.offlineContentDao().find(r.resourceId, m.languageCode(), r.resourceVersion);
                    if (localContent != null
                            && Objects.equals(localContent.versionId, r.resourceVersion)
                            && Objects.equals(localContent.sourceChecksumSha256, r.checksumSha256)
                            && localContent.json != null && !localContent.json.isBlank()) {
                        r.complete = true;
                        if (r.localPath != null
                                && db.offlineResourceDao().pathReferenceCount(r.localPath, m.manifestId()) == 0) {
                            Files.deleteIfExists(Paths.get(r.localPath));
                        }
                        r.localPath = null;
                        db.offlineResourceDao().upsert(r);
                        done += r.byteSize;
                        setProgressAsync(new Data.Builder()
                                .putLong("downloadedBytes", done)
                                .putLong("totalBytes", m.totalBytes()).build());
                        continue;
                    }
                }

                if ("MEDIA".equals(r.resourceType)) {
                    OfflineResourceEntity existing = db.offlineResourceDao().anyCompleteMedia(r.resourceId);
                    if (existing != null
                            && Objects.equals(existing.checksumSha256, r.checksumSha256)
                            && existing.localPath != null && Files.exists(Paths.get(existing.localPath))) {
                        r.localPath = existing.localPath;
                        r.complete = true;
                        db.offlineResourceDao().upsert(r);
                        mediaPaths.put(r.resourceId, r.localPath);
                        done += r.byteSize;
                        setProgressAsync(new Data.Builder()
                                .putLong("downloadedBytes", done)
                                .putLong("totalBytes", m.totalBytes()).build());
                        continue;
                    }
                }

                Path finalPath = root.resolve(r.relativePath).normalize();
                if (!finalPath.startsWith(root)) {
                    throw new IOException("Unsafe offline path");
                }
                Files.createDirectories(finalPath.getParent());
                Path temp = finalPath.resolveSibling(finalPath.getFileName() + ".part");

                long available = new StatFs(root.toString()).getAvailableBytes();
                long reserve = "MEDIA".equals(r.resourceType) ? 10_000_000L : 1_000_000L;
                if (available < r.byteSize + reserve) {
                    return Result.failure(new Data.Builder()
                            .putString(KEY_ERROR, "Not enough storage space.").build());
                }

                // A fully received .part file may remain after process death. Verify it
                // before issuing another Range request (which could otherwise return 416).
                boolean alreadyComplete = false;
                if (Files.exists(temp) && r.byteSize > 0 && Files.size(temp) == r.byteSize) {
                    try {
                        verify(temp, r.checksumSha256, r.byteSize);
                        alreadyComplete = true;
                    } catch (Exception corruptPartial) {
                        Files.deleteIfExists(temp);
                    }
                }
                if (!alreadyComplete) {
                    download(http, auth, r.downloadUrl, temp, r.byteSize);
                    // Every required resource, including question JSON, is verified before commit.
                    verify(temp, r.checksumSha256, r.byteSize);
                }

                Files.move(temp, finalPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);

                r.complete = true;
                r.localPath = finalPath.toString();
                db.offlineResourceDao().upsert(r);

                if ("MEDIA".equals(r.resourceType)) {
                    mediaPaths.put(r.resourceId, r.localPath);
                } else if ("QUESTION".equals(r.resourceType)) {
                    String json = new String(Files.readAllBytes(finalPath), StandardCharsets.UTF_8);
                    json = replaceMediaUrls(json, mediaPaths);
                    OfflineContentEntity content = new OfflineContentEntity();
                    content.questionId = r.resourceId;
                    content.languageCode = m.languageCode();
                    content.versionId = r.resourceVersion;
                    content.json = json;
                    content.byteSize = json.getBytes(StandardCharsets.UTF_8).length;
                    content.checksumSha256 = sha256(json.getBytes(StandardCharsets.UTF_8));
                    content.savedAtEpochMs = System.currentTimeMillis();
                    db.offlineContentDao().upsert(content);
                    // Question JSON is stored in Room; only binary media remains in files/offline.
                    Files.deleteIfExists(finalPath);
                    r.localPath = null;
                    db.offlineResourceDao().upsert(r);
                }

                done += r.byteSize;
                manifest.downloadedBytes = done;
                manifest.updatedAtEpochMs = System.currentTimeMillis();
                db.offlineManifestDao().insert(manifest);
                setProgressAsync(new Data.Builder()
                        .putLong("downloadedBytes", done)
                        .putLong("totalBytes", m.totalBytes()).build());
            }

            // Rebind every question's media references after all media downloads finish.
            // This also handles reused question JSON when an image changed in a newer package.
            Map<String, String> verifiedMediaPaths = new HashMap<>();
            for (OfflineResourceEntity media : db.offlineResourceDao().byManifest(m.manifestId())) {
                if ("MEDIA".equals(media.resourceType) && media.complete && media.localPath != null
                        && Files.exists(Paths.get(media.localPath))) {
                    verifiedMediaPaths.put(media.resourceId, media.localPath);
                }
            }
            for (OfflineResourceEntity question : db.offlineResourceDao().byManifest(m.manifestId())) {
                if (!"QUESTION".equals(question.resourceType) || !question.complete) continue;
                OfflineContentEntity content = db.offlineContentDao().find(question.resourceId, m.languageCode(), question.resourceVersion);
                String json;
                if (content != null && content.json != null && !content.json.isBlank()) {
                    json = content.json;
                } else if (question.localPath != null && Files.exists(Paths.get(question.localPath))) {
                    json = new String(Files.readAllBytes(Paths.get(question.localPath)), StandardCharsets.UTF_8);
                    content = new OfflineContentEntity();
                    content.questionId = question.resourceId;
                    content.languageCode = m.languageCode();
                    content.versionId = question.resourceVersion;
                } else {
                    throw new IOException("Downloaded question content is missing.");
                }
                json = rewriteMediaReferences(json, verifiedMediaPaths);
                content.json = json;
                content.byteSize = json.getBytes(StandardCharsets.UTF_8).length;
                content.checksumSha256 = sha256(content.json.getBytes(StandardCharsets.UTF_8));
                content.sourceChecksumSha256 = question.checksumSha256;
                content.savedAtEpochMs = System.currentTimeMillis();
                db.offlineContentDao().upsert(content);
            }

            OfflineManifestEntity finalManifest = db.offlineManifestDao().find(m.manifestId());
            if (finalManifest != null) {
                // Do not report completion while any required item is missing or corrupt.
                for (OfflineResourceEntity resource : db.offlineResourceDao().byManifest(m.manifestId())) {
                    if (resource.required && !resource.complete) {
                        throw new IOException("Required offline resource is incomplete: " + resource.resourceId);
                    }
                    if (resource.required && "MEDIA".equals(resource.resourceType)
                            && (resource.localPath == null || !Files.exists(Paths.get(resource.localPath)))) {
                        throw new IOException("A required offline image is missing.");
                    }
                }
                finalManifest.complete = true;
                finalManifest.downloadedBytes = m.totalBytes();
                finalManifest.updatedAtEpochMs = System.currentTimeMillis();
                db.offlineManifestDao().insert(finalManifest);
                if (!OfflineManager.isManifestIntact(db, m.manifestId())) {
                    finalManifest.complete = false;
                    db.offlineManifestDao().insert(finalManifest);
                    throw new IOException("Offline package integrity verification failed.");
                }
                // Replace stale versions only after the new package is fully verified.
                for (OfflineManifestEntity previous : db.offlineManifestDao().completeByScope(
                        m.scopeType(), m.scopeId().toString(), m.languageCode())) {
                    if (!m.manifestId().equals(previous.manifestId)) {
                        OfflineManager.deleteManifest(c, previous.manifestId);
                    }
                }
            }
            return Result.success(new Data.Builder().putString(KEY_MANIFEST_ID, m.manifestId()).build());
        } catch (java.net.UnknownHostException | java.net.ConnectException
                 | java.net.SocketTimeoutException | RetryableDownloadException e) {
            return Result.retry();
        } catch (Exception e) {
            android.util.Log.e("ManifestDownloadWorker", "Download failed", e);
            return Result.failure(new Data.Builder().putString(KEY_ERROR,
                    e.getMessage() == null ? "Offline download failed." : e.getMessage()).build());
        }

    }

    private static final class RetryableDownloadException extends IOException {
        RetryableDownloadException(String message) { super(message); }
    }

    private void download(OkHttpClient client, AuthManager auth, String url, Path temp, long expected) throws Exception {
        String absolute = url.startsWith("http")
                ? url
                : BuildConfig.API_BASE_URL.replaceAll("/+$", "") + (url.startsWith("/") ? url : "/" + url);

        Response response = executeGetWithRange(client, absolute, temp);
        if (response.code() == 401) {
            response.close();
            if (refreshAccessToken(client, auth)) {
                response = executeGetWithRange(client, absolute, temp);
            }
        }

        try (Response finalResponse = response) {
            if (!finalResponse.isSuccessful() && finalResponse.code() != 206) {
                if (finalResponse.code() == 429 || finalResponse.code() >= 500) {
                    throw new RetryableDownloadException("Temporary server response: HTTP " + finalResponse.code());
                }
                throw new IOException("Download failed: HTTP " + finalResponse.code());
            }
            long existing = Files.exists(temp) ? Files.size(temp) : 0;
            boolean append = existing > 0 && finalResponse.code() == 206;
            if (append) {
                String contentRange = finalResponse.header("Content-Range");
                if (contentRange == null || !contentRange.startsWith("bytes " + existing + "-")) {
                    throw new IOException("Server returned an invalid range response; retry the download.");
                }
            }
            if (existing > 0 && !append) {
                Files.deleteIfExists(temp);
            }
            try (InputStream in = finalResponse.body().byteStream();
                 OutputStream out = Files.newOutputStream(temp, append
                         ? new OpenOption[]{StandardOpenOption.CREATE, StandardOpenOption.APPEND}
                         : new OpenOption[]{StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING})) {
                byte[] buf = new byte[64 * 1024];
                int n;
                while ((n = in.read(buf)) != -1) {
                    if (isStopped()) {
                        throw new InterruptedIOException("Work cancelled");
                    }
                    out.write(buf, 0, n);
                }
            }
            if (expected > 0 && !url.contains("/catalog/questions/") && Files.size(temp) != expected) {
                throw new IOException("Incomplete download: expected " + expected + " bytes");
            }

        }
    }

    private Response executeGetWithRange(OkHttpClient client, String url, Path temp) throws IOException {
        long existing = Files.exists(temp) ? Files.size(temp) : 0;
        Request.Builder b = new Request.Builder().url(url).get();
        if (existing > 0) {
            b.header("Range", "bytes=" + existing + "-");
        }
        return client.newCall(b.build()).execute();
    }

    private boolean refreshAccessToken(OkHttpClient client, AuthManager auth) {
        try {
            AuthTokens current = auth.current();
            if (current == null || current.refreshToken() == null || current.refreshToken().isBlank()) {
                return false;
            }
            String base = BuildConfig.API_BASE_URL.endsWith("/")
                    ? BuildConfig.API_BASE_URL
                    : BuildConfig.API_BASE_URL + "/";
            String refreshUrl = base + "auth/refresh";

            Gson gson = new Gson();
            Map<String, String> bodyMap = new HashMap<>();
            bodyMap.put("refreshToken", current.refreshToken());
            RequestBody body = RequestBody.create(gson.toJson(bodyMap), JSON);

            Request req = new Request.Builder()
                    .url(refreshUrl)
                    .post(body)
                    .build();

            try (Response res = client.newCall(req).execute()) {
                if (!res.isSuccessful() || res.body() == null) {
                    return false;
                }
                String rawJson = res.body().string();
                JsonObject obj = JsonParser.parseString(rawJson).getAsJsonObject();

                String newAccess = obj.has("accessToken") && !obj.get("accessToken").isJsonNull()
                        ? obj.get("accessToken").getAsString() : null;
                String newRefresh = obj.has("refreshToken") && !obj.get("refreshToken").isJsonNull()
                        ? obj.get("refreshToken").getAsString() : current.refreshToken();

                if (newAccess == null || newAccess.isBlank()) {
                    return false;
                }

                String expiresAtRaw = java.time.Instant.now().plusSeconds(900).toString();
                if (obj.has("expiresAt") && !obj.get("expiresAt").isJsonNull()) {
                    expiresAtRaw = obj.get("expiresAt").getAsString();
                }

                String userId = obj.has("userId") && !obj.get("userId").isJsonNull()
                        ? obj.get("userId").getAsString() : null;
                String email = obj.has("email") && !obj.get("email").isJsonNull()
                        ? obj.get("email").getAsString() : null;

                bd.hscsolution.student.network.AuthApi.AuthResponse authResponse =
                        new bd.hscsolution.student.network.AuthApi.AuthResponse(
                                newAccess,
                                newRefresh,
                                expiresAtRaw,
                                userId,
                                email,
                                List.of(),
                                List.of()
                        );
                auth.saveResponse(authResponse);
                return true;
            }
        } catch (Exception ignored) {
            return false;
        }
    }

    private void verify(Path p, String checksum, long expected) throws Exception {
        if (!p.toString().contains("/media/")) return;

        if (expected > 0 && Files.size(p) != expected) {
            throw new IOException("Size verification failed");
        }
        if (checksum == null || checksum.isBlank()) {
            throw new IOException("Required offline resource has no SHA-256 checksum.");
        }
        if (!checksum.equalsIgnoreCase(sha256(Files.readAllBytes(p)))) {
            throw new IOException("Checksum mismatch");
        }
    }

    private static String sha256(byte[] b) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(b);
        return bytesToHex(digest);
    }

    private static String bytesToHex(byte[] bytes) {
        char[] hexArray = "0123456789abcdef".toCharArray();
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    private static String rewriteMediaReferences(String json, Map<String, String> mediaPaths) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        if (root.has("version") && root.get("version").isJsonObject()) {
            JsonObject version = root.getAsJsonObject("version");
            if (version.has("media") && version.get("media").isJsonArray()) {
                rewriteMediaArray(version.getAsJsonArray("media"), mediaPaths);
            }
        }
        if (root.has("solution") && root.get("solution").isJsonObject()) {
            JsonObject solution = root.getAsJsonObject("solution");
            if (solution.has("version") && solution.get("version").isJsonObject()) {
                JsonObject version = solution.getAsJsonObject("version");
                if (version.has("steps") && version.get("steps").isJsonArray()) {
                    for (JsonElement stepElement : version.getAsJsonArray("steps")) {
                        if (!stepElement.isJsonObject()) continue;
                        JsonObject step = stepElement.getAsJsonObject();
                        if (step.has("media") && step.get("media").isJsonArray()) {
                            rewriteMediaArray(step.getAsJsonArray("media"), mediaPaths);
                        }
                    }
                }
            }
        }
        return root.toString();
    }

    private static void rewriteMediaArray(JsonArray media, Map<String, String> mediaPaths) {
        for (JsonElement element : media) {
            if (!element.isJsonObject()) continue;
            JsonObject asset = element.getAsJsonObject();
            if (!asset.has("id") || asset.get("id").isJsonNull()) continue;
            String id = asset.get("id").getAsString();
            String localPath = mediaPaths.get(id);
            if (localPath != null) {
                asset.addProperty("deliveryUrl", "file://" + localPath.replace("\\\\", "/"));
            }
        }
    }

    private static String replaceMediaUrls(String json, Map<String, String> media) {
        for (var e : media.entrySet()) {
            json = replaceMediaUrlForId(json, e.getKey(), e.getValue());
        }
        return json;
    }

    private static String replaceMediaUrlForId(String json, String assetId, String localPath) {
        String local = "file://" + localPath.replace("\\", "/");
        json = json.replace("/api/v1/media/" + assetId, local);
        return json.replaceAll("https?://[^\\\"]*/api/v1/media/" + java.util.regex.Pattern.quote(assetId), local);
    }
}