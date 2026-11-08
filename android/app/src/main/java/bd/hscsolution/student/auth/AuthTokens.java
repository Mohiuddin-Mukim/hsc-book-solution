package bd.hscsolution.student.auth;

public record AuthTokens(String accessToken, String refreshToken, long expiresAtEpochMs) {
}
