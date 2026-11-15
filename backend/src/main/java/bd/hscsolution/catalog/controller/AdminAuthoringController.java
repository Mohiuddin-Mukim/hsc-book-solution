package bd.hscsolution.catalog.controller;

import bd.hscsolution.catalog.dto.*;
import bd.hscsolution.catalog.entity.AcademicEnums.ContentStatus;
import bd.hscsolution.catalog.entity.Question;
import bd.hscsolution.catalog.entity.QuestionVersion;
import bd.hscsolution.catalog.repository.QuestionRepository;
import bd.hscsolution.catalog.service.AuthoringService;
import bd.hscsolution.security.AuthPrincipal;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/v1/admin/authoring")
public class AdminAuthoringController {
    private final AuthoringService service;
    private final QuestionRepository questions;

    public AdminAuthoringController(AuthoringService service, QuestionRepository questions) {
        this.service=service; this.questions=questions;
    }

    private UUID actor(AuthPrincipal p){ return UUID.fromString(p.userId()); }

    @GetMapping("/questions/{questionId}")
    @PreAuthorize("hasAuthority('PERM_CONTENT_READ')")
    public AcademicDtos.QuestionDetail detail(@PathVariable UUID questionId){
        return service.detailForAdmin(questionId);
    }

    @PutMapping("/questions/{questionId}/versions/{versionId}")
    @PreAuthorize("hasAuthority('PERM_CONTENT_EDIT')")
    public AcademicDtos.QuestionVersionDetail updateQuestionVersion(
            @PathVariable UUID questionId,@PathVariable UUID versionId,
            @Valid @RequestBody AuthoringDtos.VersionEditRequest r,
            @AuthenticationPrincipal AuthPrincipal p) {
        return service.updateQuestionVersion(questionId,versionId,r,actor(p));
    }

    @PutMapping("/questions/{questionId}/solution/versions/{versionId}")
    @PreAuthorize("hasAuthority('PERM_CONTENT_EDIT')")
    public AcademicDtos.SolutionVersionDetail updateSolutionVersion(
            @PathVariable UUID questionId,@PathVariable UUID versionId,
            @Valid @RequestBody AuthoringDtos.SolutionEditRequest r,
            @AuthenticationPrincipal AuthPrincipal p) {
        return service.updateSolutionVersion(questionId,versionId,r,actor(p));
    }

    @GetMapping("/questions/{questionId}/history")
    @PreAuthorize("hasAuthority('PERM_CONTENT_READ')")
    public AuthoringDtos.HistoryResponse history(@PathVariable UUID questionId){
        return service.history(questionId);
    }

    @GetMapping("/questions/{questionId}/diff")
    @PreAuthorize("hasAuthority('PERM_CONTENT_READ')")
    public AuthoringDtos.DiffResponse diff(@PathVariable UUID questionId,
                                           @RequestParam UUID fromVersionId,
                                           @RequestParam UUID toVersionId){
        return service.diff(questionId,fromVersionId,toVersionId);
    }

    @PostMapping("/questions/{questionId}/comments")
    @PreAuthorize("hasAuthority('PERM_CONTENT_REVIEW')")
    public AuthoringDtos.ReviewCommentDto comment(@PathVariable UUID questionId,
            @Valid @RequestBody AuthoringDtos.CommentRequest r,@AuthenticationPrincipal AuthPrincipal p){
        return service.comment(questionId,r,actor(p));
    }

    @PostMapping("/questions/{questionId}/review")
    @PreAuthorize("hasAuthority('PERM_CONTENT_REVIEW')")
    public AcademicDtos.QuestionDetail review(@PathVariable UUID questionId,
            @Valid @RequestBody AuthoringDtos.ReviewActionRequest r,@AuthenticationPrincipal AuthPrincipal p){
        if (r.targetStatus()==ContentStatus.PUBLISHED || r.targetStatus()==ContentStatus.ARCHIVED)
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN,
                    "Use the publish or unpublish endpoint for this transition");
        return service.transition(questionId,r,actor(p));
    }

    @PostMapping("/questions/{questionId}/submit")
    @PreAuthorize("hasAuthority('PERM_CONTENT_EDIT')")
    public AcademicDtos.QuestionDetail submit(@PathVariable UUID questionId,
            @RequestBody(required=false) AuthoringDtos.ReviewActionRequest r,@AuthenticationPrincipal AuthPrincipal p){
        return service.transition(questionId,new AuthoringDtos.ReviewActionRequest(ContentStatus.SUBMITTED,r==null?null:r.note()),actor(p));
    }

    @PostMapping("/questions/{questionId}/publish")
    @PreAuthorize("hasAuthority('PERM_CONTENT_PUBLISH')")
    public AcademicDtos.QuestionDetail publish(@PathVariable UUID questionId,
            @RequestBody(required=false) AuthoringDtos.ReviewActionRequest r,@AuthenticationPrincipal AuthPrincipal p){
        return service.transition(questionId,new AuthoringDtos.ReviewActionRequest(ContentStatus.PUBLISHED,r==null?null:r.note()),actor(p));
    }

    @PostMapping("/questions/{questionId}/unpublish")
    @PreAuthorize("hasAuthority('PERM_CONTENT_PUBLISH')")
    public AcademicDtos.QuestionDetail unpublish(@PathVariable UUID questionId,
            @RequestBody(required=false) AuthoringDtos.ReviewActionRequest r,@AuthenticationPrincipal AuthPrincipal p){
        return service.transition(questionId,new AuthoringDtos.ReviewActionRequest(ContentStatus.ARCHIVED,r==null?null:r.note()),actor(p));
    }

    @PostMapping("/questions/{questionId}/rollback")
    @PreAuthorize("hasAuthority('PERM_CONTENT_EDIT')")
    public AcademicDtos.QuestionDetail rollback(@PathVariable UUID questionId,
            @Valid @RequestBody AuthoringDtos.RollbackRequest r,@AuthenticationPrincipal AuthPrincipal p){
        return service.rollback(questionId,r,actor(p));
    }

    @GetMapping("/review-queue")
    @PreAuthorize("hasAuthority('PERM_CONTENT_REVIEW')")
    public List<AcademicDtos.QuestionSummary> queue(@RequestParam(defaultValue="SUBMITTED") ContentStatus status){
        return questions.findByStatusOrderByIdAsc(status).stream().map(q -> new AcademicDtos.QuestionSummary(
                q.id,q.chapter.id,q.chapter.code,q.questionType,q.difficulty,q.status,q.currentVersionId)).toList();
    }
}
