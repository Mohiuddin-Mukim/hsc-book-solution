package bd.hscsolution.offline;

import bd.hscsolution.catalog.dto.AcademicDtos.QuestionDetail;
import bd.hscsolution.catalog.entity.AcademicEnums.ContentStatus;
import bd.hscsolution.catalog.entity.Question;
import bd.hscsolution.catalog.repository.*;
import bd.hscsolution.catalog.service.AcademicCatalogService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.*;

@Service
public class OfflineManifestService {
    private final ChapterRepository chapters;
    private final PaperRepository papers;
    private final BookRepository books;
    private final QuestionRepository questions;
    private final AcademicCatalogService catalog;
    private final OfflineManifestRepository manifests;
    private final OfflineManifestItemRepository items;
    private final ObjectMapper objectMapper;
    private final OfflineProperties properties;
    private final JdbcTemplate jdbc;

    public OfflineManifestService(ChapterRepository chapters,
                                  PaperRepository papers,
                                  BookRepository books,
                                  QuestionRepository questions,
                                  AcademicCatalogService catalog,
                                  OfflineManifestRepository manifests,
                                  OfflineManifestItemRepository items,
                                  ObjectMapper objectMapper,
                                  OfflineProperties properties,
                                  JdbcTemplate jdbc) {
        this.chapters = chapters;
        this.papers = papers;
        this.books = books;
        this.questions = questions;
        this.catalog = catalog;
        this.manifests = manifests;
        this.items = items;
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.jdbc = jdbc;
    }

    @Transactional
    public OfflineManifestDtos.ManifestResponse chapter(UUID userId, UUID chapterId, String language) {
        if (!chapters.existsById(chapterId))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Chapter not found");
        requireAccess(userId, "CHAPTER", chapterId);
        return build("CHAPTER", chapterId, language, List.of(chapterId));
    }

    @Transactional
    public OfflineManifestDtos.ManifestResponse book(UUID userId, UUID bookId, String language) {
        var book = books.findById(bookId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Book not found"));

        // The current schema links chapters to papers, not to book editions.
        // Therefore a book scope is defined as the published content under the book's subject.
        List<UUID> chapterIds = new ArrayList<>();
        papers.findBySubjectIdOrderByDisplayOrderAsc(book.getSubject().getId(), PageRequest.of(0, 1000))
                .getContent().forEach(p ->
                        chapters.findByPaperIdOrderByDisplayOrderAsc(p.getId(), PageRequest.of(0, 1000))
                                .getContent().forEach(c -> chapterIds.add(c.getId())));
        requireAccess(userId, "BOOK", bookId);
        return build("BOOK", bookId, language, chapterIds);
    }

    private OfflineManifestDtos.ManifestResponse build(String scopeType, UUID scopeId, String language, List<UUID> chapterIds) {
        String lang = "en".equalsIgnoreCase(language) ? "en" : "bn";
        OffsetDateTime now = OffsetDateTime.now();

        List<OfflineManifestDtos.Item> generated = new ArrayList<>();
        for (UUID chapterId : chapterIds) {
            var page = questions.searchPublished(chapterId, ContentStatus.PUBLISHED, null, null, null, null,
                    PageRequest.of(0, 10000));
            for (Question q : page.getContent()) {
                if (q.publishedVersionId == null) continue;
                QuestionDetail detail = catalog.publicDetail(q.getId(), lang);
                if (detail.version() == null) continue;

                byte[] json;
                try {
                    json = objectMapper.writeValueAsBytes(detail);
                } catch (Exception e) {
                    throw new IllegalStateException("Could not serialize offline question", e);
                }
                generated.add(new OfflineManifestDtos.Item(
                        "QUESTION", q.getId(), detail.version().id().toString() + ":" + sha256(json),
                        "/catalog/questions/" + q.getId() + "?language=" + lang,
                        json.length, sha256(json), "application/json",
                        "content/questions/" + q.getId() + "-" + detail.version().id() + ".json",
                        true, null, null, null, null));

                collectMedia(detail, generated);
            }
        }

        if (generated.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No published questions are available for offline download.");
        }

        // The same figure can be attached to multiple locations. The manifest downloads
        // each asset once while the question JSON keeps all attachment references/order.
        Map<String, OfflineManifestDtos.Item> uniqueItems = new LinkedHashMap<>();
        for (OfflineManifestDtos.Item item : generated) {
            uniqueItems.putIfAbsent(item.resourceType() + ":" + item.resourceId(), item);
        }
        generated = new ArrayList<>(uniqueItems.values());

        long total = generated.stream().mapToLong(OfflineManifestDtos.Item::byteSize).sum();
        String checksum = manifestChecksum(generated);
        var latest = manifests.findTopByScopeTypeAndScopeIdAndLanguageCodeOrderByVersionNoDesc(scopeType, scopeId, lang);
        if (latest.isPresent() && checksum.equals(latest.get().checksumSha256)) {
            OfflineManifestEntity unchanged = latest.get();
            OffsetDateTime expiry = properties.enforceEntitlement()
                    ? entitlementEndsAt(scopeType, scopeId) : null;
            unchanged.accessUntil = expiry == null ? null
                    : expiry.plusMinutes(Math.max(0, properties.defaultGraceMinutes()));
            manifests.save(unchanged);
            return response(unchanged);
        }

        long version = latest.map(x -> x.versionNo + 1).orElse(1L);
        var manifest = new OfflineManifestEntity();
        manifest.scopeType = scopeType;
        manifest.scopeId = scopeId;
        manifest.languageCode = lang;
        manifest.versionNo = version;
        manifest.checksumSha256 = checksum;
        manifest.totalBytes = total;
        manifest.generatedAt = now;
        manifest.status = OfflineManifestEntity.Status.READY;
        OffsetDateTime entitlementExpiry = properties.enforceEntitlement()
                ? entitlementEndsAt(manifest.scopeType, manifest.scopeId) : null;
        manifest.accessUntil = entitlementExpiry == null ? null
                : entitlementExpiry.plusMinutes(Math.max(0, properties.defaultGraceMinutes()));
        manifests.save(manifest);

        for (var item : generated) {
            var e = new OfflineManifestItemEntity();
            e.manifest = manifest;
            e.resourceType = item.resourceType();
            e.resourceId = item.resourceId();
            e.resourceVersion = 1L;
            e.resourceVersionLabel = item.resourceVersion();
            e.downloadUrl = item.downloadUrl();
            e.byteSize = item.byteSize();
            e.checksumSha256 = item.checksumSha256();
            e.mimeType = item.mimeType();
            e.relativePath = item.relativePath();
            e.required = item.required();
            e.attachmentRole = item.attachmentRole();
            e.displayOrder = item.displayOrder();
            e.caption = item.caption();
            e.altText = item.altText();
            items.save(e);
        }
        return response(manifest, generated);
    }

    private void requireAccess(UUID userId, String resourceType, UUID resourceId) {
        if (!properties.enforceEntitlement()) return;
        Integer count = jdbc.queryForObject(
                "select count(*) from entitlement e join subscription s on s.id=e.subscription_id " +
                        "where s.user_id=? and s.status='ACTIVE' and e.starts_at<=now() and e.ends_at>now() " +
                        "and (e.resource_id is null or (upper(e.resource_type)=? and e.resource_id=?))",
                Integer.class, userId, resourceType, resourceId);
        if (count == null || count == 0)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Offline access requires an active entitlement");
    }

    private OffsetDateTime entitlementEndsAt(String resourceType, UUID resourceId) {
        List<OffsetDateTime> ends = jdbc.query(
                "select e.ends_at from entitlement e join subscription s on s.id=e.subscription_id " +
                        "where s.status='ACTIVE' and e.starts_at<=now() and e.ends_at>now() " +
                        "and (e.resource_id is null or (upper(e.resource_type)=? and e.resource_id=?))",
                (rs, row) -> rs.getObject(1, OffsetDateTime.class), resourceType, resourceId);
        return ends.stream().max(Comparator.naturalOrder()).orElse(null);
    }

    private void collectMedia(QuestionDetail detail, List<OfflineManifestDtos.Item> out) {
        if (detail.version() != null && detail.version().media() != null) {
            for (var m : detail.version().media()) addMedia(m, out);
        }
        if (detail.solution() != null && detail.solution().version() != null) {
            for (var step : detail.solution().version().steps()) {
                if (step.media() == null) continue;
                for (var m : step.media()) addMedia(m, out);
            }
        }
    }

    private void addMedia(bd.hscsolution.media.MediaDtos.MediaAssetResponse m, List<OfflineManifestDtos.Item> out) {
        if (m == null) return;
        if (m.id() == null || m.checksumSha256() == null || m.checksumSha256().isBlank()
                || m.byteSize() <= 0 || m.deliveryUrl() == null || m.deliveryUrl().isBlank()) {
            throw new IllegalStateException("A referenced media asset is missing required offline integrity metadata.");
        }
        String path = "media/" + m.id() + extension(m.mimeType());
        out.add(new OfflineManifestDtos.Item(
                "MEDIA", m.id(), "1",
                "/media/" + m.id(),
                m.byteSize(), m.checksumSha256(), m.mimeType(), path, true,
                m.role(), m.order(), m.caption(), m.altText()));
    }

    private String extension(String mime) {
        if (mime == null) return ".bin";
        return switch (mime.toLowerCase(Locale.ROOT)) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> ".bin";
        };
    }

    private long parseVersion(String s) {
        try { return Long.parseLong(s); } catch (Exception e) { return 1L; }
    }

    private OfflineManifestDtos.ManifestResponse response(OfflineManifestEntity m) {
        return response(m, items.findByManifestId(m.id).stream().map(this::toDto).toList());
    }

    private OfflineManifestDtos.ManifestResponse response(OfflineManifestEntity m, List<OfflineManifestDtos.Item> its) {
        return new OfflineManifestDtos.ManifestResponse(
                m.id.toString(), m.versionNo, m.scopeType, m.scopeId, scopeTitle(m), m.languageCode,
                m.checksumSha256, m.totalBytes, m.generatedAt, m.accessUntil, its,
                new OfflineManifestDtos.MathAssets(
                        properties.mathRenderer(), properties.mathRendererVersion(),
                        properties.mathAssetPath(), properties.mathAssetBytes(),
                        properties.mathAssetSha256(), true));
    }

    private OfflineManifestDtos.Item toDto(OfflineManifestItemEntity x) {
        return new OfflineManifestDtos.Item(
                x.resourceType, x.resourceId, x.resourceVersionLabel == null ? Long.toString(x.resourceVersion) : x.resourceVersionLabel,
                x.downloadUrl, x.byteSize, x.checksumSha256, x.mimeType, x.relativePath,
                x.required, x.attachmentRole, x.displayOrder, x.caption, x.altText);
    }

    private String scopeTitle(OfflineManifestEntity manifest) {
        if ("CHAPTER".equals(manifest.scopeType)) {
            return chapters.findById(manifest.scopeId).map(c -> c.getTitle()).orElse("Downloaded chapter");
        }
        if ("BOOK".equals(manifest.scopeType)) {
            return books.findById(manifest.scopeId).map(b -> b.getTitle()).orElse("Downloaded book");
        }
        return "Offline package";
    }

    private String manifestChecksum(List<OfflineManifestDtos.Item> list) {
        try {
            String canonical = objectMapper.writeValueAsString(list.stream().map(x ->
                    List.of(x.resourceType(), x.resourceId(), x.resourceVersion(), x.byteSize(),
                            x.checksumSha256(), x.relativePath())).toList());
            return sha256(canonical.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("Could not checksum manifest", e);
        }
    }

    private static String sha256(byte[] bytes) {
        try {
            var md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(bytes));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}