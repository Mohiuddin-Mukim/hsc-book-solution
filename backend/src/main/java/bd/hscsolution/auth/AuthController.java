package bd.hscsolution.auth;

import bd.hscsolution.security.AuthPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService service;

    public AuthController(AuthService service) {
        this.service = service;
    }

    @PostMapping("/login")
    public AuthDtos.LoginResponse login(@Valid @RequestBody AuthDtos.LoginRequest req, HttpServletRequest r, HttpServletResponse s) {
        var out = service.login(req, clientIp(r), r.getHeader("X-Client-Type"));
        if (out.refreshToken() != null) {
            s.addHeader("Set-Cookie", "refresh_token=" + out.refreshToken() + "; Path=/api/v1/auth; HttpOnly; SameSite=Lax");
        }
        return out;
    }

    @PostMapping("/refresh")
    public AuthDtos.LoginResponse refresh(@RequestBody(required = false) AuthDtos.RefreshRequest body, @CookieValue(name = "refresh_token", required = false) String cookie, HttpServletRequest r, HttpServletResponse s) {
        String raw = "ANDROID".equalsIgnoreCase(r.getHeader("X-Client-Type")) ? (body == null ? null : body.refreshToken()) : cookie;
        var out = service.refresh(raw);
        if (!"ANDROID".equalsIgnoreCase(r.getHeader("X-Client-Type"))) {
            s.addHeader("Set-Cookie", "refresh_token=" + out.refreshToken() + "; Path=/api/v1/auth; HttpOnly; SameSite=Lax");
            return new AuthDtos.LoginResponse(out.accessToken(), null, out.expiresAt(), out.userId(), out.email(), out.roles(), out.permissions());
        }
        return out;
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody(required = false) AuthDtos.RefreshRequest body, @CookieValue(name = "refresh_token", required = false) String cookie, @AuthenticationPrincipal AuthPrincipal principal, HttpServletResponse s) {
        String raw = cookie != null ? cookie : (body == null ? null : body.refreshToken());
        service.logout(raw, principal == null ? null : UUID.fromString(principal.userId()));
        s.addHeader("Set-Cookie", "refresh_token=; Path=/api/v1/auth; Max-Age=0; HttpOnly; SameSite=Lax");
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public AuthDtos.MeResponse me(@AuthenticationPrincipal AuthPrincipal p) {
        return service.me(UUID.fromString(p.userId()));
    }

    private String clientIp(HttpServletRequest r) {
        String x = r.getHeader("X-Forwarded-For");
        return x == null || x.isBlank() ? r.getRemoteAddr() : x.split(",")[0].trim();
    }
}
