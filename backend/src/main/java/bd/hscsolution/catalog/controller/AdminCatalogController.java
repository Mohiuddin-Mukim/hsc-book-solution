package bd.hscsolution.catalog.controller;

import bd.hscsolution.catalog.dto.*;
import bd.hscsolution.catalog.service.AcademicCatalogService;
import bd.hscsolution.security.AuthPrincipal;
import jakarta.validation.Valid;

import java.util.*;

import static bd.hscsolution.catalog.entity.AcademicEnums.*;

import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/catalog")
public class AdminCatalogController {
    private final AcademicCatalogService s;

    public AdminCatalogController(AcademicCatalogService s) {
        this.s = s;
    }

    private UUID actor(AuthPrincipal p) {
        return UUID.fromString(p.userId());
    }

    @GetMapping("/subjects")
    @PreAuthorize("hasAuthority('PERM_CONTENT_READ')")
    public Page<AcademicDtos.SubjectResponse> subjects(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return s.subjects(page, size);
    }

    @PostMapping("/subjects")
    @PreAuthorize("hasAuthority('PERM_CONTENT_CREATE')")
    public AcademicDtos.SubjectResponse subject(@Valid @RequestBody AcademicDtos.SubjectRequest r, @AuthenticationPrincipal AuthPrincipal p) {
        return s.createSubject(r, actor(p));
    }

    @PutMapping("/subjects/{id}")
    @PreAuthorize("hasAuthority('PERM_CONTENT_EDIT')")
    public AcademicDtos.SubjectResponse subject(@PathVariable UUID id, @Valid @RequestBody AcademicDtos.SubjectRequest r, @AuthenticationPrincipal AuthPrincipal p) {
        return s.updateSubject(id, r, actor(p));
    }

    @GetMapping("/papers")
    @PreAuthorize("hasAuthority('PERM_CONTENT_READ')")
    public Page<AcademicDtos.PaperResponse> papers(@RequestParam UUID subjectId, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return s.papers(subjectId, page, size);
    }

    @PostMapping("/papers")
    @PreAuthorize("hasAuthority('PERM_CONTENT_CREATE')")
    public AcademicDtos.PaperResponse paper(@Valid @RequestBody AcademicDtos.PaperRequest r, @AuthenticationPrincipal AuthPrincipal p) {
        return s.createPaper(r, actor(p));
    }

    @GetMapping("/books")
    @PreAuthorize("hasAuthority('PERM_CONTENT_READ')")
    public Page<AcademicDtos.BookResponse> books(@RequestParam(required = false) UUID subjectId, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return s.books(subjectId, page, size);
    }

    @PostMapping("/books")
    @PreAuthorize("hasAuthority('PERM_CONTENT_CREATE')")
    public AcademicDtos.BookResponse book(@Valid @RequestBody AcademicDtos.BookRequest r, @AuthenticationPrincipal AuthPrincipal p) {
        return s.createBook(r, actor(p));
    }

    @PostMapping("/books/{bookId}/editions")
    @PreAuthorize("hasAuthority('PERM_CONTENT_CREATE')")
    public AcademicDtos.EditionResponse edition(@PathVariable UUID bookId, @Valid @RequestBody AcademicDtos.EditionRequest r, @AuthenticationPrincipal AuthPrincipal p) {
        return s.createEdition(bookId, r, actor(p));
    }

    @GetMapping("/chapters")
    @PreAuthorize("hasAuthority('PERM_CONTENT_READ')")
    public Page<AcademicDtos.ChapterResponse> chapters(@RequestParam UUID paperId, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return s.chapters(paperId, page, size);
    }

    @PostMapping("/chapters")
    @PreAuthorize("hasAuthority('PERM_CONTENT_CREATE')")
    public AcademicDtos.ChapterResponse chapter(@Valid @RequestBody AcademicDtos.ChapterRequest r, @AuthenticationPrincipal AuthPrincipal p) {
        return s.createChapter(r, actor(p));
    }

    @GetMapping("/topics")
    @PreAuthorize("hasAuthority('PERM_CONTENT_READ')")
    public Page<AcademicDtos.TopicResponse> topics(@RequestParam UUID chapterId, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return s.topics(chapterId, page, size);
    }

    @PostMapping("/topics")
    @PreAuthorize("hasAuthority('PERM_CONTENT_CREATE')")
    public AcademicDtos.TopicResponse topic(@Valid @RequestBody AcademicDtos.TopicRequest r, @AuthenticationPrincipal AuthPrincipal p) {
        return s.createTopic(r, actor(p));
    }

    @PostMapping("/questions")
    @PreAuthorize("hasAuthority('PERM_CONTENT_CREATE')")
    public AcademicDtos.QuestionDetail question(@Valid @RequestBody AcademicDtos.QuestionCreateRequest r, @AuthenticationPrincipal AuthPrincipal p) {
        return s.createQuestion(r, actor(p));
    }

    @PostMapping("/questions/{id}/versions")
    @PreAuthorize("hasAuthority('PERM_CONTENT_EDIT')")
    public AcademicDtos.QuestionVersionDetail qv(@PathVariable UUID id, @Valid @RequestBody AcademicDtos.QuestionVersionRequest r, @AuthenticationPrincipal AuthPrincipal p) {
        return s.addQuestionVersion(id, r, actor(p));
    }

    @PostMapping("/questions/{id}/solution/versions")
    @PreAuthorize("hasAuthority('PERM_CONTENT_EDIT')")
    public AcademicDtos.SolutionVersionDetail sv(@PathVariable UUID id, @Valid @RequestBody AcademicDtos.SolutionVersionRequest r, @AuthenticationPrincipal AuthPrincipal p) {
        return s.addSolutionVersion(id, r, actor(p));
    }

    @PostMapping("/questions/{id}/versions/{versionId}/mcq-options")
    @PreAuthorize("hasAuthority('PERM_CONTENT_EDIT')")
    public AcademicDtos.QuestionVersionDetail mcqOptions(@PathVariable UUID id, @PathVariable UUID versionId, @RequestBody List<AcademicDtos.McqOptionRequest> r, @AuthenticationPrincipal AuthPrincipal p) {
        return s.addMcqOptions(id, versionId, r, actor(p));
    }

    @PostMapping("/questions/{id}/versions/{versionId}/mcq-explanation")
    @PreAuthorize("hasAuthority('PERM_CONTENT_EDIT')")
    public AcademicDtos.QuestionVersionDetail mcqExplanation(@PathVariable UUID id, @PathVariable UUID versionId, @Valid @RequestBody AcademicDtos.McqExplanationRequest r, @AuthenticationPrincipal AuthPrincipal p) {
        return s.addMcqExplanation(id, versionId, r, actor(p));
    }

    @PostMapping("/questions/{id}/workflow")
    @PreAuthorize("hasAuthority('PERM_CONTENT_REVIEW')")
    public AcademicDtos.QuestionDetail workflow(@PathVariable UUID id, @RequestBody AcademicDtos.WorkflowRequest r, @AuthenticationPrincipal AuthPrincipal p) {
        return s.workflow(id, r, actor(p));
    }

    @PostMapping("/questions/{id}/solution/workflow")
    @PreAuthorize("hasAuthority('PERM_CONTENT_REVIEW')")
    public AcademicDtos.SolutionDetail solutionWorkflow(@PathVariable UUID id, @RequestBody AcademicDtos.WorkflowRequest r, @AuthenticationPrincipal AuthPrincipal p) {
        return s.solutionWorkflow(id, r, actor(p));
    }

    @GetMapping("/questions")
    @PreAuthorize("hasAuthority('PERM_CONTENT_READ')")
    public Page<AcademicDtos.QuestionSummary> questions(@RequestParam UUID chapterId, @RequestParam(defaultValue = "false") boolean publishedOnly, @RequestParam(required = false) QuestionType type, @RequestParam(required = false) Difficulty difficulty, @RequestParam(required = false) java.math.BigDecimal minMarks, @RequestParam(required = false) java.math.BigDecimal maxMarks, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return s.questions(chapterId, publishedOnly, type, difficulty, minMarks, maxMarks, page, size);
    }
}
