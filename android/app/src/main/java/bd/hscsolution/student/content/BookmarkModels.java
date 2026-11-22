package bd.hscsolution.student.content;

import java.time.Instant;

public final class BookmarkModels {
    private BookmarkModels() {}
    public record BookmarkResponse(String questionId, boolean bookmarked, String createdAt) {}
}
