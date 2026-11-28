package bd.hscsolution.commerce;

import bd.hscsolution.security.AuthPrincipal;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.*;

@Service
public class ContentAccessService {
    private final JdbcTemplate db;

    public ContentAccessService(JdbcTemplate db) {
        this.db = db;
    }

    public void requireChapter(UUID chapterId, AuthPrincipal p) {
        boolean free = db.queryForObject("select exists(select 1 from free_content_access where chapter_id=?)", Boolean.class, chapterId);
        boolean priced = db.queryForObject("select exists(select 1 from plan where is_active=true and (resource_type='CHAPTER' and resource_id=? or resource_type='BOOK' and resource_id in (select b.id from book b join paper pp on pp.subject_id=b.subject_id join chapter c on c.paper_id=pp.id where c.id=?)) )", Boolean.class, chapterId, chapterId);
        if (!priced || free) return;
        if (p == null)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This chapter requires an active purchase.");
        UUID uid = UUID.fromString(p.userId());
        Integer ok = db.queryForObject("select count(*) from entitlement e join subscription s on s.id=e.subscription_id where s.user_id=? and s.status='ACTIVE' and e.starts_at<=now() and e.ends_at>now() and ((upper(e.resource_type)='CHAPTER' and e.resource_id=?) or (upper(e.resource_type)='BOOK' and e.resource_id in (select b.id from book b join paper pp on pp.subject_id=b.subject_id join chapter c on c.paper_id=pp.id where c.id=?)) or e.resource_id is null)", Integer.class, uid, chapterId, chapterId);
        if (ok == null || ok == 0)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Purchase this chapter or its book to continue.");
    }
}