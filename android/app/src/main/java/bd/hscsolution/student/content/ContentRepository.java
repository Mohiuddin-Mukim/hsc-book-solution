package bd.hscsolution.student.content;

import bd.hscsolution.student.network.ApiClient;
import bd.hscsolution.student.network.ApiErrorMapper;
import bd.hscsolution.student.AppDatabase;
import bd.hscsolution.student.offline.OfflineContentEntity;
import bd.hscsolution.student.offline.OfflineManifestEntity;
import bd.hscsolution.student.offline.OfflineResourceEntity;
import bd.hscsolution.student.offline.OfflineManager;
import bd.hscsolution.student.offline.OfflineModels;
import com.google.gson.Gson;
import retrofit2.Response;

public final class ContentRepository {
    private final ContentApi api;
    private final AppDatabase database;
    private final Gson gson = new Gson();

    public ContentRepository(ApiClient client, AppDatabase database) {
        this.api = client.contentApi();
        this.database = database;
    }

    public CatalogModels.PageResponse<CatalogModels.SubjectItem> subjects(int page, int size) throws Exception {
        Response<CatalogModels.PageResponse<CatalogModels.SubjectItem>> r = api.subjects(page, size).execute();
        if (!r.isSuccessful()) throw ApiErrorMapper.from(r);
        return requireBody(r.body());
    }

    public CatalogModels.PageResponse<CatalogModels.PaperItem> papers(String subjectId, int page, int size) throws Exception {
        Response<CatalogModels.PageResponse<CatalogModels.PaperItem>> r = api.papers(subjectId, page, size).execute();
        if (!r.isSuccessful()) throw ApiErrorMapper.from(r);
        return requireBody(r.body());
    }

    public CatalogModels.PageResponse<CatalogModels.BookItem> books(String subjectId, int page, int size) throws Exception {
        Response<CatalogModels.PageResponse<CatalogModels.BookItem>> r = api.books(subjectId, page, size).execute();
        if (!r.isSuccessful()) throw ApiErrorMapper.from(r);
        return requireBody(r.body());
    }

    public CatalogModels.PageResponse<CatalogModels.EditionItem> editions(String bookId, int page, int size) throws Exception {
        Response<CatalogModels.PageResponse<CatalogModels.EditionItem>> r = api.editions(bookId, page, size).execute();
        if (!r.isSuccessful()) throw ApiErrorMapper.from(r);
        return requireBody(r.body());
    }

    public CatalogModels.PageResponse<CatalogModels.ChapterItem> chapters(String paperId, int page, int size) throws Exception {
        Response<CatalogModels.PageResponse<CatalogModels.ChapterItem>> r = api.chapters(paperId, page, size).execute();
        if (!r.isSuccessful()) throw ApiErrorMapper.from(r);
        return requireBody(r.body());
    }

    public CatalogModels.PageResponse<CatalogModels.PublishedQuestionItem> questions(
            String chapterId, String type, String difficulty, String minMarks, String maxMarks,
            int page, int size) throws Exception {
        Response<CatalogModels.PageResponse<CatalogModels.PublishedQuestionItem>> r =
                api.questions(chapterId, type, difficulty, minMarks, maxMarks, page, size).execute();
        if (!r.isSuccessful()) throw ApiErrorMapper.from(r);
        return requireBody(r.body());
    }

    public CatalogModels.QuestionDetail question(String id, String language) throws Exception {
        try {
            Response<CatalogModels.QuestionDetail> r = api.question(id, language).execute();
            if (!r.isSuccessful()) throw ApiErrorMapper.from(r);
            return requireBody(r.body());
        } catch (Exception networkOrApiError) {
            for (OfflineManifestEntity manifest : OfflineManager.uniqueCompleteManifests(database)) {
                if (manifest.accessUntil != null && !manifest.accessUntil.isBlank()
                        && !java.time.OffsetDateTime.parse(manifest.accessUntil)
                                .isAfter(java.time.OffsetDateTime.now())) continue;
                for (OfflineResourceEntity resource : database.offlineResourceDao().byManifest(manifest.manifestId)) {
                    if (!"QUESTION".equals(resource.resourceType) || !resource.complete
                            || !id.equals(resource.resourceId)) continue;
                    OfflineContentEntity local = database.offlineContentDao().find(
                            id, language, resource.resourceVersion);
                    if (local == null || local.json == null || local.json.isBlank()) continue;
                    if (!OfflineManager.isManifestIntact(database, manifest.manifestId)) continue;
                    return gson.fromJson(local.json, CatalogModels.QuestionDetail.class);
                }
            }
            throw networkOrApiError;
        }
    }

    public CatalogModels.PageResponse<CatalogModels.PublishedQuestionItem> offlineQuestionsForManifest(
            String manifestId, String language, int page, int size,
            String type, String difficulty, String minMarks, String maxMarks) throws Exception {
        OfflineManifestEntity manifest = database.offlineManifestDao().find(manifestId);
        if (manifest == null || !manifest.complete
                || !OfflineManager.isManifestIntact(database, manifestId)) {
            throw new java.io.IOException("This offline package is incomplete or a local file is missing/corrupt. Please download it again.");
        }
        if (manifest.accessUntil != null && !manifest.accessUntil.isBlank()
                && !java.time.OffsetDateTime.parse(manifest.accessUntil).isAfter(java.time.OffsetDateTime.now())) {
            throw new java.io.IOException("Offline access has expired. Connect to the internet to renew access.");
        }
        java.util.List<CatalogModels.PublishedQuestionItem> items = new java.util.ArrayList<>();
        for (OfflineResourceEntity resource : database.offlineResourceDao().byManifest(manifestId)) {
            if (!"QUESTION".equals(resource.resourceType) || !resource.complete) continue;
            OfflineContentEntity local = database.offlineContentDao().find(resource.resourceId, language, resource.resourceVersion);
            if (local == null || local.json == null || local.json.isBlank()) continue;
            CatalogModels.QuestionDetail detail = gson.fromJson(local.json, CatalogModels.QuestionDetail.class);
            if (detail == null || detail.version() == null) continue;
            if (type != null && !type.isBlank() && !type.equalsIgnoreCase(detail.questionType())) continue;
            if (difficulty != null && !difficulty.isBlank() && !difficulty.equalsIgnoreCase(detail.difficulty())) continue;
            java.math.BigDecimal marks = detail.version().marks();
            if (minMarks != null && !minMarks.isBlank()
                    && (marks == null || marks.compareTo(new java.math.BigDecimal(minMarks)) < 0)) continue;
            if (maxMarks != null && !maxMarks.isBlank()
                    && (marks == null || marks.compareTo(new java.math.BigDecimal(maxMarks)) > 0)) continue;
            items.add(new CatalogModels.PublishedQuestionItem(
                    detail.id(), detail.chapterId(), detail.chapterCode(), detail.questionType(),
                    detail.difficulty(), marks, detail.version().id()));
        }
        items.sort(java.util.Comparator.comparing(CatalogModels.PublishedQuestionItem::id));
        int safeSize = Math.max(1, size);
        int total = items.size();
        int from = Math.min(Math.max(0, page) * safeSize, total);
        int to = Math.min(from + safeSize, total);
        int pages = total == 0 ? 0 : (int) Math.ceil(total / (double) safeSize);
        return new CatalogModels.PageResponse<>(new java.util.ArrayList<>(items.subList(from, to)),
                Math.max(0, page), safeSize, total, pages);
    }

    public OfflineModels.ManifestResponse offlineChapterManifest(String chapterId, String language) throws Exception {
        Response<OfflineModels.ManifestResponse> r = api.offlineChapter(chapterId, language).execute();
        if (!r.isSuccessful()) throw ApiErrorMapper.from(r);
        return requireBody(r.body());
    }

    public OfflineModels.ManifestResponse offlineBookManifest(String bookId, String language) throws Exception {
        Response<OfflineModels.ManifestResponse> r = api.offlineBook(bookId, language).execute();
        if (!r.isSuccessful()) throw ApiErrorMapper.from(r);
        return requireBody(r.body());
    }


    public CatalogModels.SearchPageResponse search(
            String query, String subjectId, String paperId, String chapterId,
            String language, String type, String difficulty, int page, int size) throws Exception {
        Response<CatalogModels.SearchPageResponse> r = api.search(
                query, subjectId, paperId, chapterId, language, type, difficulty, page, size).execute();
        if (!r.isSuccessful()) throw ApiErrorMapper.from(r);
        return requireBody(r.body());
    }

    public BookmarkModels.BookmarkResponse bookmark(String questionId) throws Exception {
        Response<BookmarkModels.BookmarkResponse> r = api.bookmark(questionId).execute();
        if (!r.isSuccessful()) throw ApiErrorMapper.from(r);
        return requireBody(r.body());
    }

    public BookmarkModels.BookmarkResponse addBookmark(String questionId) throws Exception {
        Response<BookmarkModels.BookmarkResponse> r = api.addBookmark(questionId).execute();
        if (!r.isSuccessful()) throw ApiErrorMapper.from(r);
        return requireBody(r.body());
    }

    public BookmarkModels.BookmarkResponse removeBookmark(String questionId) throws Exception {
        Response<BookmarkModels.BookmarkResponse> r = api.removeBookmark(questionId).execute();
        if (!r.isSuccessful()) throw ApiErrorMapper.from(r);
        return requireBody(r.body());
    }

    private static <T> T requireBody(T body) throws java.io.IOException {
        if (body == null) throw new java.io.IOException("Could not load content. The server returned an empty response.");
        return body;
    }
}
