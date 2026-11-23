package bd.hscsolution.catalog.controller;

import bd.hscsolution.catalog.dto.SearchDtos;
import bd.hscsolution.catalog.entity.AcademicEnums;
import bd.hscsolution.catalog.service.SearchService;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/catalog")
public class AdminSearchController {
    private final SearchService searchService;

    public AdminSearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping("/search")
    @PreAuthorize("hasAuthority('PERM_CONTENT_READ')")
    public Page<SearchDtos.Result> search(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(required = false) UUID subjectId,
            @RequestParam(required = false) UUID paperId,
            @RequestParam(required = false) UUID chapterId,
            @RequestParam(required = false) String language,
            @RequestParam(required = false) AcademicEnums.QuestionType type,
            @RequestParam(required = false) AcademicEnums.Difficulty difficulty,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return searchService.adminSearch(q, subjectId, paperId, chapterId, language,
                type == null ? null : type.name(),
                difficulty == null ? null : difficulty.name(), page, size);
    }
}
