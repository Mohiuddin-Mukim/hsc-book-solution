package bd.hscsolution.security;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Creates the local bootstrap SUPER_ADMIN when no SUPER_ADMIN assignment exists.
 * It is intentionally idempotent: existing normal users do not prevent creation
 * of the bootstrap administrator.
 */
@Component
public class BootstrapAdminRunner implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(BootstrapAdminRunner.class);

    private final JdbcTemplate db;
    private final PasswordEncoder encoder;
    private final SecurityProperties properties;

    public BootstrapAdminRunner(JdbcTemplate db, PasswordEncoder encoder, SecurityProperties properties) {
        this.db = db;
        this.encoder = encoder;
        this.properties = properties;
    }

    @Override
    public void run(String... args) {
        String email = properties.bootstrapAdminEmail();
        String password = properties.bootstrapAdminPassword();
        String name = properties.bootstrapAdminName();

        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            log.warn("Bootstrap SUPER_ADMIN is disabled because bootstrap admin email/password is not configured.");
            return;
        }

        Integer userCount = db.queryForObject("select count(*) from app_user", Integer.class);
        Integer superAdminCount = db.queryForObject(
                "select count(*) from app_user u "
                        + "join user_role ur on ur.user_id=u.id "
                        + "join role r on r.id=ur.role_id "
                        + "where r.code='SUPER_ADMIN'",
                Integer.class);

        log.info("Bootstrap admin check: app_user={}, super_admin_users={}", userCount, superAdminCount);

        if (superAdminCount != null && superAdminCount > 0) {
            log.info("SUPER_ADMIN already exists; bootstrap creation skipped.");
            return;
        }

        String normalizedEmail = email.trim().toLowerCase();
        UUID existingUserId = db.query(
                "select id from app_user where lower(email)=lower(?)",
                rs -> rs.next() ? rs.getObject(1, UUID.class) : null,
                normalizedEmail);

        UUID userId;
        if (existingUserId != null) {
            userId = existingUserId;
            db.update("update app_user set password_hash=?, display_name=?, status='ACTIVE', email_verified=true where id=?",
                    encoder.encode(password), name, userId);
            log.info("Existing bootstrap email found; activated/reset password for {}", normalizedEmail);
        } else {
            userId = UUID.randomUUID();
            db.update(
                    "insert into app_user(id,email,password_hash,display_name,status,email_verified) "
                            + "values(?,?,?,?, 'ACTIVE', true)",
                    userId, normalizedEmail, encoder.encode(password), name);
            log.info("Created bootstrap user {}", normalizedEmail);
        }

        UUID roleId = db.queryForObject(
                "select id from role where code='SUPER_ADMIN'", UUID.class);
        if (roleId == null) {
            throw new IllegalStateException("SUPER_ADMIN role is missing. Check Flyway V2/V3 migrations.");
        }

        Integer assignmentCount = db.queryForObject(
                "select count(*) from user_role where user_id=? and role_id=?",
                Integer.class, userId, roleId);
        if (assignmentCount == null || assignmentCount == 0) {
            db.update("insert into user_role(user_id,role_id) values(?,?)", userId, roleId);
        }

        log.info("Bootstrap SUPER_ADMIN ready for {}", normalizedEmail);
    }
}
