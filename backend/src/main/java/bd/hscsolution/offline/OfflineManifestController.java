package bd.hscsolution.offline;

import bd.hscsolution.security.AuthPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/student/offline")
@PreAuthorize("hasRole('STUDENT')")
public class OfflineManifestController {
    private final OfflineManifestService service;

    public OfflineManifestController(OfflineManifestService service) {
        this.service = service;
    }

    @GetMapping("/manifests/chapters/{chapterId}")
    public OfflineManifestDtos.ManifestResponse chapter(
            @PathVariable UUID chapterId,
            @RequestParam(defaultValue = "bn") String language,
            @AuthenticationPrincipal AuthPrincipal principal) {
        return service.chapter(UUID.fromString(principal.userId()), chapterId, language);
    }

    @GetMapping("/manifests/books/{bookId}")
    public OfflineManifestDtos.ManifestResponse book(
            @PathVariable UUID bookId,
            @RequestParam(defaultValue = "bn") String language,
            @AuthenticationPrincipal AuthPrincipal principal) {
        return service.book(UUID.fromString(principal.userId()), bookId, language);
    }
}
