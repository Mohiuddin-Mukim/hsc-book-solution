package bd.hscsolution.commerce;

import bd.hscsolution.catalog.repository.BookRepository;
import bd.hscsolution.catalog.repository.ChapterRepository;
import bd.hscsolution.security.AuditService;
import jakarta.transaction.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.*;
import java.time.OffsetDateTime;
import java.util.*;

@Service
public class CommerceService {
    private final JdbcTemplate db;
    private final PaymentProvider provider;
    private final AuditService audit;
    private final BookRepository books;
    private final ChapterRepository chapters;

    public CommerceService(JdbcTemplate db, PaymentProvider provider, AuditService audit, BookRepository books, ChapterRepository chapters) {
        this.db = db;
        this.provider = provider;
        this.audit = audit;
        this.books = books;
        this.chapters = chapters;
    }

    private static ResponseStatusException bad(String m) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, m);
    }

    private static ResponseStatusException nf(String m) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, m);
    }

    private static String currency(String c) {
        return c == null || c.isBlank() ? "BDT" : c.toUpperCase(Locale.ROOT);
    }

    private static BigDecimal discount(long o, long s) {
        return o == 0 ? BigDecimal.ZERO : BigDecimal.valueOf(o - s).multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(o), 2, RoundingMode.HALF_UP);
    }

    private static void price(long o, long s) {
        if (o < 0 || s < 0 || s > o) throw bad("Sale price must be between 0 and original price");
    }

    public List<CommerceDtos.PlanResponse> plans(boolean active) {
        String q = "select id,code,name,plan_type,resource_type,resource_id,original_price_minor,sale_price_minor,discount_percent,currency,duration_days,description,is_active from plan" + (active ? " where is_active=true" : "") + " order by updated_at desc";
        return db.query(q, (rs, i) -> new CommerceDtos.PlanResponse(rs.getObject(1, UUID.class), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5), rs.getObject(6, UUID.class), rs.getLong(7), rs.getLong(8), rs.getBigDecimal(9), rs.getString(10), rs.getInt(11), rs.getString(12), rs.getBoolean(13), packageBooks(rs.getObject(1, UUID.class))));
    }

    private List<UUID> packageBooks(UUID id) {
        return db.query("select book_id from plan_package_item where plan_id=? order by book_id", (rs, i) -> rs.getObject(1, UUID.class), id);
    }

    public CommerceDtos.PlanResponse getPlan(UUID id) {
        return plans(false).stream().filter(x -> x.id().equals(id)).findFirst().orElseThrow(() -> nf("Plan not found"));
    }

    @Transactional
    public CommerceDtos.PlanResponse createPlan(CommerceDtos.PlanRequest r, UUID actor) {
        validate(r);
        UUID id = UUID.randomUUID();
        BigDecimal d = discount(r.originalPriceMinor(), r.salePriceMinor());
        db.update("insert into plan(id,code,name,price_minor,currency,duration_days,is_active,metadata,plan_type,resource_type,resource_id,original_price_minor,sale_price_minor,discount_percent,description,updated_at) values(?,?,?,?,?,? ,?, '{}',?,?,?,?,?,?,?,now())", id, r.code(), r.name(), r.salePriceMinor(), currency(r.currency()), r.durationDays(), Boolean.TRUE.equals(r.active()), r.planType(), r.resourceType(), r.resourceId(), r.originalPriceMinor(), r.salePriceMinor(), d, r.description());
        if ("PACKAGE".equals(r.planType()))
            for (UUID b : r.bookIds()) db.update("insert into plan_package_item(plan_id,book_id) values(?,?)", id, b);
        audit.record(actor, "PLAN_CREATE", "PLAN", id, null, Map.of("salePriceMinor", r.salePriceMinor()));
        return getPlan(id);
    }

    @Transactional
    public CommerceDtos.PlanResponse updatePlan(UUID id, CommerceDtos.PlanRequest r, UUID actor) {
        validate(r);
        BigDecimal d = discount(r.originalPriceMinor(), r.salePriceMinor());
        if (db.update("update plan set code=?,name=?,plan_type=?,resource_type=?,resource_id=?,original_price_minor=?,sale_price_minor=?,price_minor=?,discount_percent=?,duration_days=?,currency=?,description=?,is_active=?,updated_at=now() where id=?", r.code(), r.name(), r.planType(), r.resourceType(), r.resourceId(), r.originalPriceMinor(), r.salePriceMinor(), r.salePriceMinor(), d, r.durationDays(), currency(r.currency()), r.description(), Boolean.TRUE.equals(r.active()), id) == 0)
            throw nf("Plan not found");
        db.update("delete from plan_package_item where plan_id=?", id);
        if ("PACKAGE".equals(r.planType()))
            for (UUID b : r.bookIds()) db.update("insert into plan_package_item(plan_id,book_id) values(?,?)", id, b);
        audit.record(actor, "PLAN_UPDATE", "PLAN", id, null, Map.of("discountPercent", d));
        return getPlan(id);
    }

    private void validate(CommerceDtos.PlanRequest r) {
        if (!Set.of("CHAPTER", "BOOK", "PACKAGE").contains(r.planType()))
            throw bad("planType must be CHAPTER, BOOK or PACKAGE");
        price(r.originalPriceMinor(), r.salePriceMinor());
        if ("PACKAGE".equals(r.planType()) && (r.bookIds() == null || r.bookIds().isEmpty()))
            throw bad("Package must contain at least one book");
        if (!"PACKAGE".equals(r.planType()) && (r.resourceId() == null))
            throw bad("A chapter/book plan needs resourceId");
        if ("PACKAGE".equals(r.planType()) && r.resourceId() != null) throw bad("Package resourceId must be empty");
    }

    @Transactional
    public CommerceDtos.CheckoutResponse checkout(UUID userId, CommerceDtos.CheckoutRequest r) {
        CommerceDtos.PlanResponse p = getPlan(r.planId());
        if (!p.active()) throw bad("Plan is inactive");
        db.update("update subscription set status='EXPIRED' where user_id=? and plan_id=? and status='ACTIVE' and ends_at<=now()", userId, p.id());
        var active = db.query("select o.id,o.order_code,o.status,o.amount_minor,o.currency from subscription s join customer_order o on o.id=s.order_id where s.user_id=? and s.plan_id=? and s.status='ACTIVE' and s.ends_at>now() limit 1", (rs,i)->new Object[]{rs.getObject(1,UUID.class),rs.getString(2),rs.getString(3),rs.getLong(4),rs.getString(5)}, userId,p.id());
        if(!active.isEmpty()){Object[] x=active.get(0);return new CommerceDtos.CheckoutResponse((UUID)x[0],(String)x[1],((Number)x[3]).longValue(),(String)x[4],(String)x[2],provider.name(),null,null,true);}
        var ex = db.query("select id,order_code,status,amount_minor,currency from customer_order where user_id=? and idempotency_key=?", (rs, i) -> new Object[]{rs.getObject(1, UUID.class), rs.getString(2), rs.getString(3), rs.getLong(4), rs.getString(5)}, userId, r.idempotencyKey());
        if (!ex.isEmpty()) {
            Object[] x = ex.get(0);
            if (((Number)x[3]).longValue()!=p.salePriceMinor() || !String.valueOf(x[4]).equalsIgnoreCase(p.currency())) throw new ResponseStatusException(HttpStatus.CONFLICT,"Idempotency key is already bound to a different payment amount or currency");
            return new CommerceDtos.CheckoutResponse((UUID) x[0], (String) x[1], ((Number) x[3]).longValue(), (String) x[4], (String) x[2], provider.name(), null, null, "PAID".equalsIgnoreCase(String.valueOf(x[2])));
        }
        UUID oid = UUID.randomUUID();
        String code = "HSC-" + oid.toString().replace("-", "").substring(0, 16).toUpperCase(Locale.ROOT);
        db.update("insert into customer_order(id,user_id,plan_id,order_code,status,amount_minor,currency,idempotency_key) values(?,?,?,?,?,?,?,?)", oid, userId, p.id(), code, "PENDING", p.salePriceMinor(), p.currency(), r.idempotencyKey());
        UUID pid = UUID.randomUUID();
        db.update("insert into payment(id,order_id,provider,status,amount_minor,currency) values(?,?,?,?,?,?)", pid, oid, provider.name(), "INITIATED", p.salePriceMinor(), p.currency());
        String email = db.queryForObject("select email from app_user where id=?", String.class, userId);
        String name = db.queryForObject("select display_name from app_user where id=?", String.class, userId);
        var pr = provider.initiate(new PaymentProvider.CommerceOrderContext(code, p.salePriceMinor(), p.currency(), email, name), r.idempotencyKey());
        db.update("update payment set provider_payment_id=?,provider_transaction_id=? where id=?", pr.providerTransactionId(), pr.providerTransactionId(), pid);
        return new CommerceDtos.CheckoutResponse(oid, code, p.salePriceMinor(), p.currency(), "PENDING", pr.provider(), pr.checkoutUrl(), pr.providerTransactionId(), false);
    }

    public CommerceDtos.PaymentStatusResponse status(UUID uid, UUID oid) {
        var a = db.query("select o.order_code,o.status,o.amount_minor,o.currency,p.provider,p.provider_transaction_id from customer_order o left join payment p on p.order_id=o.id where o.id=? and o.user_id=?", (rs, i) -> new Object[]{rs.getString(1), rs.getString(2), rs.getLong(3), rs.getString(4), rs.getString(5), rs.getString(6)}, oid, uid);
        if (a.isEmpty()) throw nf("Order not found");
        Object[] x = a.get(0);
        String e = db.queryForObject("select case when exists(select 1 from entitlement e join subscription s on s.id=e.subscription_id where s.order_id=? and s.status='ACTIVE' and e.ends_at>now()) then 'ACTIVE' else 'NONE' end", String.class, oid);
        return new CommerceDtos.PaymentStatusResponse(oid, (String) x[0], (String) x[1], ((Number) x[2]).longValue(), (String) x[3], (String) x[4], (String) x[5], e);
    }

    public List<CommerceDtos.OrderResponse> orders(UUID uid) {
        return db.query("select o.id,o.order_code,o.status,o.amount_minor,o.currency,p.name,o.created_at,pm.provider,m.payment_method,m.sender_mobile,m.transaction_id,pm.paid_at,case when exists(select 1 from entitlement e join subscription s on s.id=e.subscription_id where s.order_id=o.id and s.status='ACTIVE' and s.ends_at>now()) then 'ACTIVE' else 'NONE' end,(select max(s.ends_at) from subscription s where s.order_id=o.id),case when m.id is null then 'GATEWAY' else 'MANUAL' end,m.reference_code from customer_order o join plan p on p.id=o.plan_id left join payment pm on pm.order_id=o.id left join manual_payment_request m on m.order_id=o.id where o.user_id=? order by o.created_at desc", (rs,i)->new CommerceDtos.OrderResponse(rs.getObject(1,UUID.class),rs.getString(2),rs.getString(3),rs.getLong(4),rs.getString(5),rs.getString(6),toString(rs.getObject(7,OffsetDateTime.class)),rs.getString(8),rs.getString(9),rs.getString(10),rs.getString(11),toString(rs.getObject(12,OffsetDateTime.class)),rs.getString(13),toString(rs.getObject(14,OffsetDateTime.class)),rs.getString(15),rs.getString(16)),uid);
    }

    public List<CommerceDtos.StudentPlanResponse> studentPlans(UUID uid) {
        return plans(true).stream().map(p -> {
            var a = db.query("select s.ends_at from subscription s where s.user_id=? and s.plan_id=? and s.status='ACTIVE' and s.ends_at>now() order by s.ends_at desc limit 1",
                    (rs,i)->rs.getObject(1, OffsetDateTime.class), uid, p.id());
            String status = a.isEmpty() ? "AVAILABLE" : "PAID";
            String expires = a.isEmpty() ? null : a.get(0).toString();
            return new CommerceDtos.StudentPlanResponse(p.id(),p.code(),p.name(),p.planType(),p.resourceType(),p.resourceId(),p.originalPriceMinor(),p.salePriceMinor(),p.discountPercent(),p.currency(),p.durationDays(),p.description(),p.active(),p.bookIds(),status,expires);
        }).toList();
    }

    public List<CommerceDtos.TransactionResponse> transactions(UUID uid) {
        return db.query("select o.id,m.reference_code,p.name,o.amount_minor,o.currency,coalesce(m.status,o.status),m.payment_method,m.sender_mobile,m.transaction_id,o.created_at,m.reviewed_at,m.rejection_reason,m.admin_message,case when exists(select 1 from entitlement e join subscription s on s.id=e.subscription_id where s.order_id=o.id and s.status='ACTIVE' and s.ends_at>now()) then 'ACTIVE' else 'NONE' end,(select max(s.ends_at) from subscription s where s.order_id=o.id),case when m.id is null then 'GATEWAY' else 'MANUAL' end from customer_order o join plan p on p.id=o.plan_id left join manual_payment_request m on m.order_id=o.id where o.user_id=? order by o.created_at desc",
                (rs,i)->new CommerceDtos.TransactionResponse(rs.getObject(1,UUID.class),rs.getString(2),rs.getString(3),rs.getLong(4),rs.getString(5),rs.getString(6),rs.getString(7),rs.getString(8),rs.getString(9),toString(rs.getObject(10,OffsetDateTime.class)),toString(rs.getObject(11,OffsetDateTime.class)),rs.getString(12),rs.getString(13),rs.getString(14),toString(rs.getObject(15,OffsetDateTime.class)),rs.getString(16)),uid);
    }

    @Transactional
    public void webhook(CommerceDtos.WebhookRequest r, String raw) {
        if (!provider.name().equalsIgnoreCase(r.provider())) throw bad("Unsupported provider");
        if (!provider.verifyWebhookSignature(raw, r.signature()))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid webhook signature");
        int inserted = db.update("insert into webhook_event(id,provider,provider_event_id,event_type,signature_verified,status,payload) values(?,?,?,?,true,'RECEIVED',?::jsonb) on conflict(provider,provider_event_id) do nothing", UUID.randomUUID(), r.provider(), r.eventId(), r.eventType(), new com.fasterxml.jackson.databind.ObjectMapper().valueToTree(r.payload()).toString());
        if (inserted == 0) return;
        var rows = db.query("select id,user_id,plan_id,amount_minor,currency,status from customer_order where order_code=? for update", (rs, i) -> new Object[]{rs.getObject(1, UUID.class), rs.getObject(2, UUID.class), rs.getObject(3, UUID.class), rs.getLong(4), rs.getString(5), rs.getString(6)}, r.orderCode());
        if (rows.isEmpty()) throw nf("Order not found");
        Object[] o = rows.get(0);
        long expected = ((Number) o[3]).longValue();
        String incomingStatus = r.status();
        String st = ("PAID".equalsIgnoreCase(incomingStatus) || "SUCCESS".equalsIgnoreCase(incomingStatus))
                ? "PAID"
                : (("CANCELLED".equalsIgnoreCase(incomingStatus) || "FAILED".equalsIgnoreCase(incomingStatus)) ? "FAILED" : "PENDING");
        if (r.amountMinor() == null || r.amountMinor() != expected || !String.valueOf(o[4]).equalsIgnoreCase(r.currency())) {
            db.update("insert into commerce_reconciliation_item(provider,provider_transaction_id,order_id,expected_amount_minor,actual_amount_minor,expected_status,actual_status,mismatch_type) values(?,?,?,?,?,?,?,?)", r.provider(), r.transactionId(), (UUID) o[0], expected, r.amountMinor(), o[5], st, "AMOUNT_OR_CURRENCY_MISMATCH");
            db.update("update webhook_event set status='REJECTED',processed_at=now() where provider=? and provider_event_id=?", r.provider(), r.eventId());
            return;
        }
        db.update("update customer_order set status=?,updated_at=now() where id=?", st, (UUID) o[0]);
        db.update("update payment set status=?,provider_transaction_id=?,verified_at=now(),paid_at=case when ?='PAID' then now() else paid_at end where order_id=?", st, r.transactionId(), st, (UUID) o[0]);
        if ("PAID".equals(st)) grant((UUID) o[1], (UUID) o[0], (UUID) o[2]);
        db.update("update webhook_event set status='PROCESSED',processed_at=now() where provider=? and provider_event_id=?", r.provider(), r.eventId());
    }

    @Transactional
    public void grant(UUID uid, UUID oid, UUID pid) {
        db.update("update subscription set status='EXPIRED' where user_id=? and plan_id=? and status='ACTIVE' and ends_at<=now()", uid, pid);
        if (db.queryForObject("select count(*) from subscription where order_id=?", Integer.class, oid) > 0) return;
        CommerceDtos.PlanResponse p = getPlan(pid);
        OffsetDateTime s = OffsetDateTime.now(), e = s.plusDays(p.durationDays());
        UUID sid = UUID.randomUUID();
        db.update("insert into subscription(id,user_id,order_id,plan_id,status,starts_at,ends_at) values(?,?,?,?,?,?,?)", sid, uid, oid, pid, "ACTIVE", s, e);
        LinkedHashSet<String> res = new LinkedHashSet<>();
        if (p.resourceType() != null && p.resourceId() != null) res.add(p.resourceType() + ":" + p.resourceId());
        if ("BOOK".equals(p.planType()) && p.resourceId() != null) res.addAll(bookResources(p.resourceId()));
        if ("PACKAGE".equals(p.planType())) for (UUID b : p.bookIds()) res.addAll(bookResources(b));
        for (String x : res) {
            String[] a = x.split(":", 2);
            db.update("insert into entitlement(id,subscription_id,resource_type,resource_id,starts_at,ends_at) values(?,?,?,?,?,?)", UUID.randomUUID(), sid, a[0], UUID.fromString(a[1]), s, e);
        }
    }

    private List<String> bookResources(UUID b) {
        List<String> x = new ArrayList<>();
        x.add("BOOK:" + b);
        x.addAll(db.query("select 'CHAPTER:'||c.id from chapter c join paper p on p.id=c.paper_id join book b on b.subject_id=p.subject_id where b.id=?", (rs, i) -> rs.getString(1), b));
        return x;
    }

    @Transactional
    public CommerceDtos.FreeChapterResponse free(CommerceDtos.FreeChapterRequest r, UUID actor) {
        if (!books.existsById(r.bookId()) || !chapters.existsById(r.chapterId())) throw nf("Book or chapter not found");
        db.update("insert into free_content_access(id,book_id,chapter_id,created_by) values(?,?,?,?) on conflict(book_id,chapter_id) do nothing", UUID.randomUUID(), r.bookId(), r.chapterId(), actor);
        UUID id = db.queryForObject("select id from free_content_access where book_id=? and chapter_id=?", UUID.class, r.bookId(), r.chapterId());
        audit.record(actor, "FREE_CHAPTER_SET", "BOOK", r.bookId(), null, Map.of("chapterId", r.chapterId().toString()));
        return new CommerceDtos.FreeChapterResponse(id, r.bookId(), r.chapterId());
    }

    @Transactional
    public void unfree(UUID b, UUID c, UUID actor) {
        db.update("delete from free_content_access where book_id=? and chapter_id=?", b, c);
        audit.record(actor, "FREE_CHAPTER_REMOVE", "BOOK", b, Map.of("chapterId", c.toString()), null);
    }

    public List<CommerceDtos.ReconciliationItemResponse> reconciliation() {
        return db.query("select id,provider,provider_transaction_id,order_id,expected_amount_minor,actual_amount_minor,expected_status,actual_status,mismatch_type,resolved,created_at from commerce_reconciliation_item order by created_at desc", (rs, i) -> new CommerceDtos.ReconciliationItemResponse(rs.getObject(1, UUID.class), rs.getString(2), rs.getString(3), rs.getObject(4, UUID.class), (Long) rs.getObject(5), (Long) rs.getObject(6), rs.getString(7), rs.getString(8), rs.getString(9), rs.getBoolean(10), rs.getObject(11, OffsetDateTime.class).toString()));
    }
    private static String toString(OffsetDateTime x){ return x==null?null:x.toString(); }

}