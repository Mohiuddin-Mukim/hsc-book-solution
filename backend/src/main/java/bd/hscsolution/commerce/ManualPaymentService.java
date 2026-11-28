package bd.hscsolution.commerce;

import bd.hscsolution.security.AuditService;
import jakarta.transaction.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.*;

@Service
public class ManualPaymentService {
    private final JdbcTemplate db;
    private final CommerceService commerce;
    private final AuditService audit;
    private final ManualPaymentProperties props;

    public ManualPaymentService(JdbcTemplate db, CommerceService commerce, AuditService audit, ManualPaymentProperties props) {
        this.db = db; this.commerce = commerce; this.audit = audit; this.props = props;
    }

    @Transactional
    public CommerceDtos.ManualPortalCreateResponse createPortal(UUID userId, UUID planId) {
        CommerceDtos.PlanResponse p = commerce.getPlan(planId);
        if (!p.active()) throw bad("Plan is inactive");

        String token = randomToken();
        OffsetDateTime expires = OffsetDateTime.now().plusMinutes(Math.max(5, props.portalTtlMinutes()));

        db.update("insert into manual_payment_portal_session(id,token_hash,user_id,plan_id,expires_at) values(?,?,?,?,?)",
                UUID.randomUUID(), hash(token), userId, planId, expires);

        String base = trimSlash(props.portalBaseUrl());
        return new CommerceDtos.ManualPortalCreateResponse(base + "?token=" + token, expires.toString());
    }

    public CommerceDtos.ManualPortalResponse portal(String token) {
        PortalContext c = context(token);
        CommerceDtos.PlanResponse p = commerce.getPlan(c.planId());

        var rows = db.query(
            "select m.reference_code,m.status,m.sender_mobile,m.transaction_id,m.created_at," +
            "m.reviewed_at,m.rejection_reason,m.admin_message," +
            "case when exists (select 1 from subscription s where s.order_id=m.order_id and s.status='ACTIVE' and s.ends_at>now()) then 'ACTIVE' else 'NONE' end," +
            "(select max(s.ends_at) from subscription s where s.order_id=m.order_id and s.status='ACTIVE' and s.ends_at>now()) " +
            "from manual_payment_request m where m.user_id=? and m.plan_id=? order by m.created_at desc limit 1",
            (rs,i) -> new Object[] {
                rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4),
                rs.getObject(5, OffsetDateTime.class), rs.getObject(6, OffsetDateTime.class),
                rs.getString(7), rs.getString(8), rs.getString(9),
                rs.getObject(10, OffsetDateTime.class)
            }, c.userId(), c.planId());

        String status = "NEW";
        String ref = null, sender = null, trx = null, applied = null, reviewed = null, rejection = null, message = null;
        String access = "NONE", expires = null;

        if (!rows.isEmpty()) {
            Object[] x = rows.get(0);
            status = (String)x[1];
            ref = (String)x[0];
            sender = (String)x[2];
            trx = (String)x[3];
            applied = toString((OffsetDateTime)x[4]);
            reviewed = toString((OffsetDateTime)x[5]);
            rejection = (String)x[6];
            message = (String)x[7];
            access = (String)x[8];
            expires = toString((OffsetDateTime)x[9]);
        }

        return new CommerceDtos.ManualPortalResponse(
            ref, status, p.name(), p.salePriceMinor(), p.currency(), p.durationDays(),
            access, expires, sender, trx, applied, reviewed, rejection, message,
            props.supportWhatsappUrl(), channels()
        );
    }

    @Transactional
    public CommerceDtos.ManualPortalResponse submit(String token, CommerceDtos.ManualPaymentSubmitRequest r) {
        PortalContext c = context(token);
        CommerceDtos.PlanResponse p = commerce.getPlan(c.planId());
        if (!p.active()) throw bad("Plan is inactive");

        String method = normalizeMethod(r.paymentMethod());
        if (!Set.of("BKASH","NAGAD","ROCKET").contains(method)) throw bad("Invalid payment method");

        String trx = normalizeTrx(r.transactionId());
        String sender = r.senderMobile().trim();

        if (!sender.matches("^01[3-9][0-9]{8}$"))
            throw bad("Sender mobile must be a valid 11-digit Bangladesh number");
        if (trx.isBlank())
            throw bad("Transaction ID is required");

        Integer pending = db.queryForObject(
            "select count(*) from manual_payment_request where user_id=? and plan_id=? and status='PENDING'",
            Integer.class, c.userId(), c.planId());

        if (pending != null && pending > 0)
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "A payment request is already under review for this package");

        String duplicate = db.query(
            "select reference_code from manual_payment_request where transaction_id=? limit 1",
            (rs,i)->rs.getString(1), trx).stream().findFirst().orElse(null);

        if (duplicate != null)
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "This Transaction ID has already been submitted");

        UUID oid = UUID.randomUUID();
        String orderCode = "HSC-MAN-" + oid.toString().replace("-", "")
            .substring(0, 16).toUpperCase(Locale.ROOT);
        String reference = "HSC-MP-" + UUID.randomUUID().toString().replace("-", "")
            .substring(0, 10).toUpperCase(Locale.ROOT);

        db.update(
            "insert into customer_order(id,user_id,plan_id,order_code,status,amount_minor,currency) values(?,?,?,?,?,?,?)",
            oid, c.userId(), c.planId(), orderCode, "PENDING", p.salePriceMinor(), p.currency());

        UUID paymentId = UUID.randomUUID();
        db.update(
            "insert into payment(id,order_id,provider,status,amount_minor,currency) values(?,?,?,?,?,?)",
            paymentId, oid, "MANUAL", "PENDING_MANUAL", p.salePriceMinor(), p.currency());

        db.update(
            "insert into manual_payment_request(id,reference_code,user_id,plan_id,order_id,expected_amount_minor,currency,duration_days,payment_method,sender_mobile,transaction_id) values(?,?,?,?,?,?,?,?,?,?,?)",
            UUID.randomUUID(), reference, c.userId(), c.planId(), oid, p.salePriceMinor(),
            p.currency(), p.durationDays(), method, sender, trx);

        audit.record(c.userId(), "MANUAL_PAYMENT_SUBMITTED",
            "MANUAL_PAYMENT_REQUEST", null, null,
            Map.of("referenceCode", reference, "planId", c.planId().toString(),
                   "amountMinor", p.salePriceMinor()));

        return portal(token);
    }

    public List<CommerceDtos.ManualPaymentAdminResponse> queue(String status, int page, int size) {
        String s = status == null || status.isBlank() ? "PENDING" : status.toUpperCase(Locale.ROOT);
        if (!Set.of("PENDING","APPROVED","REJECTED").contains(s))
            throw bad("Invalid manual payment status");

        int safeSize = Math.max(1, Math.min(size, 100));
        int offset = Math.max(0, page) * safeSize;

        return db.query(
            "select m.id,m.reference_code,m.user_id,u.email,u.display_name,p.name,m.expected_amount_minor,m.currency,m.payment_method,m.sender_mobile,m.transaction_id,m.created_at,m.status,m.reviewed_at,m.rejection_reason,m.admin_message," +
            "(select max(s.ends_at) from subscription s where s.order_id=m.order_id and s.status='ACTIVE' and s.ends_at>now()) " +
            "from manual_payment_request m join app_user u on u.id=m.user_id join plan p on p.id=m.plan_id " +
            "where m.status=? order by m.created_at asc limit ? offset ?",
            this::mapAdmin, s, safeSize, offset);
    }

    @Transactional
    public CommerceDtos.ManualPaymentAdminResponse approve(UUID id, UUID admin, String message) {
        RequestRow r = lockRequest(id);
        if (!"PENDING".equals(r.status()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only PENDING requests can be approved");

        String msg = message == null || message.isBlank()
            ? "Payment verified, access unlocked!" : message.trim();

        db.update(
            "update manual_payment_request set status='APPROVED',reviewed_by=?,reviewed_at=now(),admin_message=?,updated_at=now() where id=?",
            admin, msg, id);
        db.update("update customer_order set status='PAID',updated_at=now() where id=?", r.orderId());
        db.update(
            "update payment set status='PAID',paid_at=now(),verified_at=now(),provider_transaction_id=? where order_id=?",
            r.transactionId(), r.orderId());

        commerce.grant(r.userId(), r.orderId(), r.planId());

        OffsetDateTime exp = db.queryForObject(
            "select ends_at from subscription where order_id=? and status='ACTIVE' order by ends_at desc limit 1",
            OffsetDateTime.class, r.orderId());

        db.update(
            "insert into user_notification(user_id,title,message,type,entity_type,entity_id) values(?,?,?,?,?,?)",
            r.userId(), "Payment approved", msg + " Package: " + r.planName() +
            ". Valid until " + exp, "PAYMENT", "MANUAL_PAYMENT_REQUEST", id);

        audit.record(admin, "MANUAL_PAYMENT_APPROVE", "MANUAL_PAYMENT_REQUEST", id, null,
            Map.of("referenceCode", r.referenceCode(), "message", msg));

        return adminItem(id);
    }

    @Transactional
    public CommerceDtos.ManualPaymentAdminResponse reject(UUID id, UUID admin, String message) {
        RequestRow r = lockRequest(id);
        if (!"PENDING".equals(r.status()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only PENDING requests can be rejected");

        String msg = message == null || message.isBlank()
            ? "Invalid TrxID / Payment not received / Amount mismatch" : message.trim();

        db.update(
            "update manual_payment_request set status='REJECTED',reviewed_by=?,reviewed_at=now(),rejection_reason=?,admin_message=?,updated_at=now() where id=?",
            admin, msg, msg, id);
        db.update("update customer_order set status='FAILED',updated_at=now() where id=?", r.orderId());
        db.update("update payment set status='REJECTED',verified_at=now() where order_id=?", r.orderId());

        db.update(
            "insert into user_notification(user_id,title,message,type,entity_type,entity_id) values(?,?,?,?,?,?)",
            r.userId(), "Payment request update", msg, "PAYMENT", "MANUAL_PAYMENT_REQUEST", id);

        audit.record(admin, "MANUAL_PAYMENT_REJECT", "MANUAL_PAYMENT_REQUEST", id, null,
            Map.of("referenceCode", r.referenceCode(), "message", msg));

        return adminItem(id);
    }

    public List<CommerceDtos.ManualPaymentChannelResponse> channels() {
        return db.query(
            "select id,method,account_number,payment_type,instruction,is_active,sort_order from manual_payment_channel where is_active=true order by sort_order,method",
            this::mapChannel);
    }

    public List<CommerceDtos.ManualPaymentChannelResponse> allChannels() {
        return db.query(
            "select id,method,account_number,payment_type,instruction,is_active,sort_order from manual_payment_channel order by sort_order,method",
            this::mapChannel);
    }

    @Transactional
    public CommerceDtos.ManualPaymentChannelResponse updateChannel(
        UUID id, CommerceDtos.ManualPaymentChannelRequest r, UUID admin) {

        String method = normalizeMethod(r.method());
        if (!Set.of("BKASH","NAGAD","ROCKET").contains(method))
            throw bad("Invalid payment method");

        String type = r.paymentType().trim();
        if (!Set.of("Send Money","Payment").contains(type))
            throw bad("paymentType must be Send Money or Payment");

        if (r.accountNumber() == null || !r.accountNumber().trim().matches("^[0-9]{5,32}$"))
            throw bad("Account number must contain 5-32 digits");

        if (db.update(
            "update manual_payment_channel set method=?,account_number=?,payment_type=?,instruction=?,is_active=?,sort_order=?,updated_at=now() where id=?",
            method, r.accountNumber().trim(), type,
            r.instruction() == null ? "" : r.instruction().trim(),
            r.active(), r.sortOrder(), id) == 0)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment channel not found");

        audit.record(admin, "MANUAL_PAYMENT_CHANNEL_UPDATE",
            "MANUAL_PAYMENT_CHANNEL", id, null, Map.of("method", method));

        return allChannels().stream().filter(x -> x.id().equals(id)).findFirst().orElseThrow();
    }

    public List<CommerceDtos.NotificationResponse> unread(UUID userId) {
        return db.query(
            "select id,title,message,type,entity_type,entity_id,created_at from user_notification where user_id=? and is_read=false order by created_at desc limit 20",
            (rs,i)->new CommerceDtos.NotificationResponse(
                rs.getObject(1,UUID.class),rs.getString(2),rs.getString(3),rs.getString(4),
                rs.getString(5),rs.getObject(6,UUID.class),
                toString(rs.getObject(7,OffsetDateTime.class))), userId);
    }

    @Transactional
    public void markRead(UUID userId, UUID id) {
        db.update("update user_notification set is_read=true where id=? and user_id=?", id, userId);
    }

    public CommerceDtos.ManualPaymentAdminResponse adminItem(UUID id) {
        return queueById(id).orElseThrow(() ->
            new ResponseStatusException(HttpStatus.NOT_FOUND, "Manual payment request not found"));
    }

    private Optional<CommerceDtos.ManualPaymentAdminResponse> queueById(UUID id) {
        return db.query(
            "select m.id,m.reference_code,m.user_id,u.email,u.display_name,p.name,m.expected_amount_minor,m.currency,m.payment_method,m.sender_mobile,m.transaction_id,m.created_at,m.status,m.reviewed_at,m.rejection_reason,m.admin_message," +
            "(select max(s.ends_at) from subscription s where s.order_id=m.order_id and s.status='ACTIVE' and s.ends_at>now()) " +
            "from manual_payment_request m join app_user u on u.id=m.user_id join plan p on p.id=m.plan_id where m.id=?",
            this::mapAdmin, id).stream().findFirst();
    }

    private RequestRow lockRequest(UUID id) {
        return db.query(
            "select m.id,m.user_id,m.plan_id,m.order_id,m.reference_code,m.status,m.transaction_id,p.name from manual_payment_request m join plan p on p.id=m.plan_id where m.id=? for update",
            (rs,i)->new RequestRow(
                rs.getObject(1,UUID.class),rs.getObject(2,UUID.class),rs.getObject(3,UUID.class),
                rs.getObject(4,UUID.class),rs.getString(5),rs.getString(6),rs.getString(7),rs.getString(8)),
            id).stream().findFirst().orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Manual payment request not found"));
    }

    private PortalContext context(String token) {
        if (token == null || token.isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment portal token is required");

        return db.query(
            "select user_id,plan_id from manual_payment_portal_session where token_hash=? and expires_at>now()",
            (rs,i)->new PortalContext(rs.getObject(1,UUID.class),rs.getObject(2,UUID.class)),
            hash(token)).stream().findFirst().orElseThrow(() ->
                new ResponseStatusException(HttpStatus.GONE,
                    "Payment portal link has expired. Please open Buy / Unlock again from the app."));
    }

    private CommerceDtos.ManualPaymentAdminResponse mapAdmin(java.sql.ResultSet rs, int i)
        throws java.sql.SQLException {
        return new CommerceDtos.ManualPaymentAdminResponse(
            rs.getObject(1,UUID.class),rs.getString(2),rs.getObject(3,UUID.class),
            rs.getString(4),rs.getString(5),rs.getString(6),rs.getLong(7),rs.getString(8),
            rs.getString(9),rs.getString(10),rs.getString(11),
            toString(rs.getObject(12,OffsetDateTime.class)),rs.getString(13),
            toString(rs.getObject(14,OffsetDateTime.class)),rs.getString(15),rs.getString(16),
            toString(rs.getObject(17,OffsetDateTime.class)));
    }

    private CommerceDtos.ManualPaymentChannelResponse mapChannel(java.sql.ResultSet rs, int i)
        throws java.sql.SQLException {
        return new CommerceDtos.ManualPaymentChannelResponse(
            rs.getObject(1,UUID.class),rs.getString(2),rs.getString(3),rs.getString(4),
            rs.getString(5),rs.getBoolean(6),rs.getInt(7));
    }

    private String randomToken() {
        return UUID.randomUUID().toString().replace("-","") +
               UUID.randomUUID().toString().replace("-","");
    }

    private static String hash(String s) {
        try {
            return HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256")
                    .digest(s.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static String normalizeMethod(String s) {
        return s == null ? "" : s.trim().toUpperCase(Locale.ROOT);
    }

    private static String normalizeTrx(String s) {
        return s == null ? "" : s.trim().toUpperCase(Locale.ROOT).replaceAll("\\s+", "");
    }

    private static String trimSlash(String s) {
        String x = s == null ? "" : s.trim();
        while (x.endsWith("/")) x = x.substring(0, x.length()-1);
        return x;
    }

    private static String toString(OffsetDateTime x) {
        return x == null ? null : x.toString();
    }

    private static ResponseStatusException bad(String m) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, m);
    }

    private record PortalContext(UUID userId, UUID planId) {}
    private record RequestRow(UUID id, UUID userId, UUID planId, UUID orderId,
                              String referenceCode, String status, String transactionId,
                              String planName) {}
}
