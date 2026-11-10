package bd.hscsolution.catalog.controller;

import bd.hscsolution.catalog.dto.AcademicDtos;
import bd.hscsolution.catalog.service.AcademicCatalogService;
import bd.hscsolution.security.AuthPrincipal;

import java.util.*;

import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/catalog/tags")
public class AdminTagController {
    private final AcademicCatalogService s;

    public AdminTagController(AcademicCatalogService s) {
        this.s = s;
    }

    private UUID actor(AuthPrincipal p) {
        return UUID.fromString(p.userId());
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_CONTENT_READ')")
    public Page<AcademicDtos.TagResponse> list(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return s.tags(page, size);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_CONTENT_CREATE')")
    public AcademicDtos.TagResponse create(@RequestBody AcademicDtos.TagRequest r, @AuthenticationPrincipal AuthPrincipal p) {
        return s.createTag(r, actor(p));
    }

    @PostMapping("/{tagId}/questions/{questionId}/versions/{versionId}")
    @PreAuthorize("hasAuthority('PERM_CONTENT_EDIT')")
    public void attach(@PathVariable UUID tagId, @PathVariable UUID questionId, @PathVariable UUID versionId, @AuthenticationPrincipal AuthPrincipal p) {
        s.attachTag(questionId, versionId, tagId, actor(p));
    }
}
