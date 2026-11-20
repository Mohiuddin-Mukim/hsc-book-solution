package bd.hscsolution.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;

public final class AuthDtos {
    private AuthDtos() {}

    public record LoginRequest(
            @Email @NotBlank String email,
            @NotBlank @Size(min = 8, max = 200) String password) {}

    public record LoginResponse(
            String accessToken,
            String refreshToken,
            Instant expiresAt,
            String userId,
            String email,
            List<String> roles,
            List<String> permissions) {}

    public record RefreshRequest(String refreshToken) {}

    public record MeResponse(
            String userId,
            String email,
            String displayName,
            List<String> roles,
            List<String> permissions) {}

    public record RegisterRequest(
            @Email @NotBlank @Size(max = 320) String email,
            @NotBlank @Size(min = 8, max = 200) String password,
            @NotBlank @Size(min = 2, max = 160) String displayName,
            @Size(max = 10) String preferredLanguage) {}

    public record RegisterResponse(
            String userId,
            String email,
            String displayName,
            String status,
            String role,
            String preferredLanguage) {}
}
