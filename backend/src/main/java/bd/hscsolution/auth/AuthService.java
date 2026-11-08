package bd.hscsolution.auth;

import bd.hscsolution.security.*;
import java.time.Instant;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {
  private final JdbcTemplate db;
  private final PasswordEncoder encoder;
  private final JwtService jwt;
  private final SecurityProperties p;
  private final LoginRateLimiter limiter;
  private final AuditService audit;

  public AuthService(JdbcTemplate db, PasswordEncoder encoder, JwtService jwt, SecurityProperties p,
      LoginRateLimiter limiter, AuditService audit) {
    this.db = db;
    this.encoder = encoder;
    this.jwt = jwt;
    this.p = p;
    this.limiter = limiter;
    this.audit = audit;
  }

  public AuthDtos.LoginResponse login(AuthDtos.LoginRequest req, String ip, String clientType) {
    String key = (req.email().trim().toLowerCase(Locale.ROOT)) + "|" + ip;
    if (!limiter.allow(key, p.loginRateLimit(), p.loginRateWindowSeconds()))
      throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many login attempts. Try again later.");
    var users = db.query("select id,email,password_hash,display_name,status from app_user where lower(email)=lower(?)",
        (rs, n) -> Map.of("id", rs.getObject("id", UUID.class), "email", rs.getString("email"), "hash",
            rs.getString("password_hash"), "name", rs.getString("display_name"), "status", rs.getString("status")),
        req.email());
    if (users.isEmpty() || users.get(0).get("hash") == null
        || !encoder.matches(req.password(), (String) users.get(0).get("hash"))
        || !"ACTIVE".equals(users.get(0).get("status"))) {
      audit.record(null, "LOGIN_FAILED", "APP_USER", null, Map.of("email", req.email()), null);
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
    }
    UUID userId = (UUID) users.get(0).get("id");
    var roles = roles(userId);
    var perms = permissions(userId);
    String access = jwt.createAccessToken(userId.toString(), req.email(), roles, perms);
    String raw = UUID.randomUUID() + "." + UUID.randomUUID();
    String hash = Hashing.sha256(raw);
    Instant exp = Instant.now().plusSeconds(p.refreshTokenDays() * 86400L);
    db.update("insert into refresh_token(user_id,token_hash,expires_at) values(?,?,?)", userId, hash,
        java.sql.Timestamp.from(exp));
    audit.record(userId, "LOGIN_SUCCESS", "APP_USER", userId, Map.of("clientType", clientType), null);
    limiter.clear(key);
    String returned = "ANDROID".equalsIgnoreCase(clientType) ? raw : null;
    return new AuthDtos.LoginResponse(access, returned, jwt.expiration(access), userId.toString(), req.email(), roles,
        perms);
  }

  public AuthDtos.LoginResponse refresh(String raw) {
    if (raw == null || raw.isBlank())
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token is required");
    String h = Hashing.sha256(raw);
    var rows = db.query(
        "select rt.id,rt.user_id,rt.expires_at,rt.revoked_at,u.email,u.status from refresh_token rt join app_user u on u.id=rt.user_id where rt.token_hash=?",
        (rs, n) -> {
          Map<String, Object> m = new HashMap<>();
          m.put("id", rs.getObject("id", UUID.class));
          m.put("userId", rs.getObject("user_id", UUID.class));
          m.put("expires", rs.getTimestamp("expires_at").toInstant());
          m.put("revoked", rs.getTimestamp("revoked_at"));
          m.put("email", rs.getString("email"));
          m.put("status", rs.getString("status"));
          return m;
        }, h);
    if (rows.isEmpty())
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token");
    var r = rows.get(0);
    if (r.get("revoked") != null || Instant.now().isAfter((Instant) r.get("expires"))
        || !"ACTIVE".equals(r.get("status")))
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token is expired or revoked");
    UUID uid = (UUID) r.get("userId");
    List<String> roles = roles(uid);
    List<String> permissions = permissions(uid);
    String access = jwt.createAccessToken(uid.toString(), (String) r.get("email"), roles, permissions);
    String newRaw = UUID.randomUUID() + "." + UUID.randomUUID();
    UUID newId = UUID.randomUUID();
    Instant exp = Instant.now().plusSeconds(p.refreshTokenDays() * 86400L);
    db.update("update refresh_token set revoked_at=now(),last_used_at=now(),replaced_by=? where id=?", newId,
        (UUID) r.get("id"));
    db.update("insert into refresh_token(id,user_id,token_hash,expires_at) values(?,?,?,?)", newId, uid,
        Hashing.sha256(newRaw), java.sql.Timestamp.from(exp));
    audit.record(uid, "TOKEN_REFRESH", "APP_USER", uid, Map.of(), null);
    return new AuthDtos.LoginResponse(access, newRaw, jwt.expiration(access), uid.toString(), (String) r.get("email"),
        roles, permissions);
  }

  public void logout(String raw, UUID actor) {
    if (raw != null && !raw.isBlank())
      db.update("update refresh_token set revoked_at=coalesce(revoked_at,now()) where token_hash=?",
          Hashing.sha256(raw));
    audit.record(actor, "LOGOUT", "APP_USER", actor, Map.of(), null);
  }

  public AuthDtos.MeResponse me(UUID uid) {
    var u = db.queryForMap("select email,display_name from app_user where id=?", uid);
    return new AuthDtos.MeResponse(uid.toString(), (String) u.get("email"), (String) u.get("display_name"), roles(uid),
        permissions(uid));
  }

  private List<String> roles(UUID uid) {
    return db.query("select r.code from role r join user_role ur on ur.role_id=r.id where ur.user_id=? order by r.code",
        (rs, n) -> rs.getString(1), uid);
  }

  private List<String> permissions(UUID uid) {
    return db.query(
        "select distinct p.code from permission p join role_permission rp on rp.permission_id=p.id join user_role ur on ur.role_id=rp.role_id where ur.user_id=? order by p.code",
        (rs, n) -> rs.getString(1), uid);
  }
}
