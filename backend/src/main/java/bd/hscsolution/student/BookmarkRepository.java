package bd.hscsolution.student;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public class BookmarkRepository {
    private final JdbcTemplate db;
    public BookmarkRepository(JdbcTemplate db) { this.db = db; }

    public boolean exists(UUID userId, UUID questionId) {
        Integer n = db.queryForObject(
                "select count(*) from bookmark where user_id=? and question_id=?",
                Integer.class, userId, questionId);
        return n != null && n > 0;
    }

    public Instant createdAt(UUID userId, UUID questionId) {
        return db.query("select created_at from bookmark where user_id=? and question_id=?",
                (rs, row) -> rs.getTimestamp(1).toInstant(), userId, questionId)
                .stream().findFirst().orElse(null);
    }

    public void add(UUID userId, UUID questionId) {
        db.update("insert into bookmark(user_id,question_id) values(?,?) on conflict(user_id,question_id) do nothing",
                userId, questionId);
    }

    public void remove(UUID userId, UUID questionId) {
        db.update("delete from bookmark where user_id=? and question_id=?", userId, questionId);
    }

    public List<BookmarkDtos.BookmarkResponse> list(UUID userId) {
        return db.query(
                "select question_id, created_at from bookmark where user_id=? order by created_at desc",
                (rs, row) -> new BookmarkDtos.BookmarkResponse(
                        rs.getObject("question_id", UUID.class), true,
                        rs.getTimestamp("created_at").toInstant()), userId);
    }
}
