package bd.hscsolution.student;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

public final class BookmarkDtos {
    private BookmarkDtos() {}
    public record BookmarkResponse(UUID questionId, boolean bookmarked, Instant createdAt) {}
}
