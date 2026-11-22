package bd.hscsolution.student.content;

import bd.hscsolution.student.network.ApiClient;
import bd.hscsolution.student.network.ApiErrorMapper;
import retrofit2.Response;

public final class ContentRepository {
    private final ContentApi api;

    public ContentRepository(ApiClient client) {
        this.api = client.contentApi();
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
        Response<CatalogModels.QuestionDetail> r = api.question(id, language).execute();
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
