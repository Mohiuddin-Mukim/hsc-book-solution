package bd.hscsolution.student.content;

import com.google.gson.JsonObject;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ContentApi {
    @GET("catalog/subjects")
    Call<CatalogModels.PageResponse<CatalogModels.SubjectItem>> subjects(
            @Query("page") int page, @Query("size") int size);

    @GET("catalog/subjects/{subjectId}/papers")
    Call<CatalogModels.PageResponse<CatalogModels.PaperItem>> papers(
            @Path("subjectId") String subjectId, @Query("page") int page, @Query("size") int size);

    @GET("catalog/subjects/{subjectId}/books")
    Call<CatalogModels.PageResponse<CatalogModels.BookItem>> books(
            @Path("subjectId") String subjectId, @Query("page") int page, @Query("size") int size);

    @GET("catalog/books/{bookId}/editions")
    Call<CatalogModels.PageResponse<CatalogModels.EditionItem>> editions(
            @Path("bookId") String bookId, @Query("page") int page, @Query("size") int size);

    @GET("catalog/papers/{paperId}/chapters")
    Call<CatalogModels.PageResponse<CatalogModels.ChapterItem>> chapters(
            @Path("paperId") String paperId, @Query("page") int page, @Query("size") int size);

    @GET("catalog/chapters/{chapterId}/topics")
    Call<CatalogModels.PageResponse<CatalogModels.TopicItem>> topics(
            @Path("chapterId") String chapterId, @Query("page") int page, @Query("size") int size);

    @GET("catalog/chapters/{chapterId}/questions")
    Call<CatalogModels.PageResponse<CatalogModels.PublishedQuestionItem>> questions(
            @Path("chapterId") String chapterId,
            @Query("type") String type,
            @Query("difficulty") String difficulty,
            @Query("minMarks") String minMarks,
            @Query("maxMarks") String maxMarks,
            @Query("page") int page,
            @Query("size") int size);

    @GET("catalog/questions/{id}")
    Call<CatalogModels.QuestionDetail> question(@Path("id") String id, @Query("language") String language);

    @GET("student/bookmarks/{questionId}")
    Call<BookmarkModels.BookmarkResponse> bookmark(@Path("questionId") String questionId);

    @retrofit2.http.PUT("student/bookmarks/{questionId}")
    Call<BookmarkModels.BookmarkResponse> addBookmark(@Path("questionId") String questionId);

    @retrofit2.http.DELETE("student/bookmarks/{questionId}")
    Call<BookmarkModels.BookmarkResponse> removeBookmark(@Path("questionId") String questionId);


    @GET("catalog/search")
    Call<CatalogModels.SearchPageResponse> search(
            @Query("q") String query,
            @Query("subjectId") String subjectId,
            @Query("paperId") String paperId,
            @Query("chapterId") String chapterId,
            @Query("language") String language,
            @Query("type") String type,
            @Query("difficulty") String difficulty,
            @Query("page") int page,
            @Query("size") int size);

    // Kept for the Phase 8 contract smoke test.
    @GET("catalog/questions/{id}")
    Call<JsonObject> questionJson(@Path("id") String id, @Query("language") String language);

    @retrofit2.http.GET("student/offline/manifests/chapters/{chapterId}")
    Call<bd.hscsolution.student.offline.OfflineModels.ManifestResponse> offlineChapter(
            @Path("chapterId") String chapterId, @Query("language") String language);

    @retrofit2.http.GET("student/offline/manifests/books/{bookId}")
    Call<bd.hscsolution.student.offline.OfflineModels.ManifestResponse> offlineBook(
            @Path("bookId") String bookId, @Query("language") String language);
}
