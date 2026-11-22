package bd.hscsolution.student;

import bd.hscsolution.security.AuthPrincipal;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/student/bookmarks")
@PreAuthorize("hasRole('STUDENT')")
public class BookmarkController {
    private final BookmarkService service;
    public BookmarkController(BookmarkService service) { this.service = service; }

    @GetMapping
    public List<BookmarkDtos.BookmarkResponse> list(@org.springframework.security.core.annotation.AuthenticationPrincipal AuthPrincipal p) {
        return service.list(UUID.fromString(p.userId()));
    }

    @GetMapping("/{questionId}")
    public BookmarkDtos.BookmarkResponse get(@PathVariable UUID questionId,
                                              @org.springframework.security.core.annotation.AuthenticationPrincipal AuthPrincipal p) {
        return service.get(UUID.fromString(p.userId()), questionId);
    }

    @PutMapping("/{questionId}")
    public BookmarkDtos.BookmarkResponse add(@PathVariable UUID questionId,
                                              @org.springframework.security.core.annotation.AuthenticationPrincipal AuthPrincipal p) {
        return service.add(UUID.fromString(p.userId()), questionId);
    }

    @DeleteMapping("/{questionId}")
    public BookmarkDtos.BookmarkResponse remove(@PathVariable UUID questionId,
                                                  @org.springframework.security.core.annotation.AuthenticationPrincipal AuthPrincipal p) {
        return service.remove(UUID.fromString(p.userId()), questionId);
    }
}
