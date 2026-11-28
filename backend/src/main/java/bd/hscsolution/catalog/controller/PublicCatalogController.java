package bd.hscsolution.catalog.controller;

import bd.hscsolution.catalog.dto.ContentPageResponse;
import bd.hscsolution.catalog.dto.PublicCatalogDtos;
import bd.hscsolution.catalog.service.AcademicCatalogService;
import bd.hscsolution.commerce.ContentAccessService;
import bd.hscsolution.security.AuthPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static bd.hscsolution.catalog.entity.AcademicEnums.*;

@RestController
@RequestMapping("/api/v1/catalog")
public class PublicCatalogController {
    private final AcademicCatalogService s;
    private final ContentAccessService access;

    public PublicCatalogController(AcademicCatalogService s, ContentAccessService access) {
        this.s = s;
        this.access = access;
    }

    private <T> ResponseEntity<ContentPageResponse<T>> page(Page<T> data) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(60, TimeUnit.SECONDS).cachePublic())
                .body(ContentPageResponse.of(data));
    }

    @GetMapping("/subjects")
    public ResponseEntity<ContentPageResponse<PublicCatalogDtos.SubjectItem>> subjects(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return page(s.publicSubjects(page, size));
    }

    @GetMapping("/subjects/{subjectId}/papers")
    public ResponseEntity<ContentPageResponse<PublicCatalogDtos.PaperItem>> papers(
            @PathVariable UUID subjectId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return page(s.publicPapers(subjectId, page, size));
    }

    @GetMapping("/subjects/{subjectId}/books")
    public ResponseEntity<ContentPageResponse<PublicCatalogDtos.BookItem>> books(
            @PathVariable UUID subjectId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return page(s.publicBooks(subjectId, page, size));
    }

    @GetMapping("/books/{bookId}/editions")
    public ResponseEntity<ContentPageResponse<PublicCatalogDtos.EditionItem>> editions(
            @PathVariable UUID bookId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return page(s.publicBookEditions(bookId, page, size));
    }

    @GetMapping("/papers/{paperId}/chapters")
    public ResponseEntity<ContentPageResponse<PublicCatalogDtos.ChapterItem>> chapters(
            @PathVariable UUID paperId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return page(s.publicChapters(paperId, page, size));
    }

    @GetMapping("/chapters/{chapterId}/topics")
    public ResponseEntity<ContentPageResponse<PublicCatalogDtos.TopicItem>> topics(
            @PathVariable UUID chapterId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return page(s.publicTopics(chapterId, page, size));
    }

    @GetMapping("/chapters/{chapterId}/questions")
    public ResponseEntity<ContentPageResponse<PublicCatalogDtos.PublishedQuestionItem>> questions(
            @PathVariable UUID chapterId,
            @RequestParam(required = false) QuestionType type,
            @RequestParam(required = false) Difficulty difficulty,
            @RequestParam(required = false) BigDecimal minMarks,
            @RequestParam(required = false) BigDecimal maxMarks,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal AuthPrincipal principal) {
        access.requireChapter(chapterId, principal);
        return page(s.publicQuestions(chapterId, type, difficulty, minMarks, maxMarks, page, size));
    }

    /**
     * Backward-compatible Phase 4 endpoint. It now returns the published-version ID
     * in QuestionSummary rather than a potentially newer draft currentVersionId.
     */
    @GetMapping("/questions")
    public ResponseEntity<ContentPageResponse<PublicCatalogDtos.PublishedQuestionItem>> legacyQuestions(
            @RequestParam UUID chapterId,
            @RequestParam(required = false) QuestionType type,
            @RequestParam(required = false) Difficulty difficulty,
            @RequestParam(required = false) BigDecimal minMarks,
            @RequestParam(required = false) BigDecimal maxMarks,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal AuthPrincipal principal) {
        access.requireChapter(chapterId, principal);
        return page(s.publicQuestions(chapterId, type, difficulty, minMarks, maxMarks, page, size));
    }

    @GetMapping("/questions/{id}")
    public ResponseEntity<bd.hscsolution.catalog.dto.AcademicDtos.QuestionDetail> question(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "bn") String language,
            @AuthenticationPrincipal AuthPrincipal principal) {
        var d = s.publicDetail(id, language);
        access.requireChapter(d.chapterId(), principal);
        String etag = "\"q-" + d.id() + "-" + (d.version() == null ? "0" : d.version().id()) + "\"";
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(60, TimeUnit.SECONDS).cachePublic())
                .eTag(etag)
                .body(d);
    }
}
