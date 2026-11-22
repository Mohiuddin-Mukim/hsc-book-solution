package bd.hscsolution.student;

import bd.hscsolution.catalog.repository.QuestionRepository;
import bd.hscsolution.catalog.entity.AcademicEnums.ContentStatus;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class BookmarkService {
    private final BookmarkRepository bookmarks;
    private final QuestionRepository questions;

    public BookmarkService(BookmarkRepository bookmarks, QuestionRepository questions) {
        this.bookmarks = bookmarks;
        this.questions = questions;
    }

    public BookmarkDtos.BookmarkResponse get(UUID userId, UUID questionId) {
        if (!questions.existsById(questionId))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Question not found");
        boolean marked = bookmarks.exists(userId, questionId);
        return new BookmarkDtos.BookmarkResponse(questionId, marked,
                marked ? bookmarks.createdAt(userId, questionId) : null);
    }

    public BookmarkDtos.BookmarkResponse add(UUID userId, UUID questionId) {
        ensurePublished(questionId);
        bookmarks.add(userId, questionId);
        return get(userId, questionId);
    }

    public BookmarkDtos.BookmarkResponse remove(UUID userId, UUID questionId) {
        if (!questions.existsById(questionId))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Question not found");
        bookmarks.remove(userId, questionId);
        return get(userId, questionId);
    }

    public List<BookmarkDtos.BookmarkResponse> list(UUID userId) {
        return bookmarks.list(userId);
    }

    private void ensurePublished(UUID questionId) {
        var q = questions.findById(questionId).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Question not found"));
        if (q.status != ContentStatus.PUBLISHED || q.publishedVersionId == null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Published question not found");
    }
}
