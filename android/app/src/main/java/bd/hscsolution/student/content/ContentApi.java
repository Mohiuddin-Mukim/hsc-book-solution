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

    // Kept for the Phase 8 contract smoke test.
    @GET("catalog/questions/{id}")
    Call<JsonObject> questionJson(@Path("id") String id, @Query("language") String language);
}
