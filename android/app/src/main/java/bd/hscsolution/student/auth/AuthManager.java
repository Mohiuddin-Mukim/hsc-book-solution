package bd.hscsolution.student.auth;

import android.content.Context;
import bd.hscsolution.student.network.ApiErrorMapper;
import bd.hscsolution.student.network.AuthApi;
import retrofit2.Response;

public final class AuthManager {
    private final SecureTokenStore store;

    public AuthManager(Context context) {
        store = new SecureTokenStore(context);
    }

    public AuthTokens saveResponse(AuthApi.AuthResponse response) throws Exception {
        if (response.accessToken() == null || response.refreshToken() == null) {
            throw new IllegalStateException("Authentication response is incomplete.");
        }
        AuthTokens tokens = new AuthTokens(response.accessToken(), response.refreshToken(), response.expiresAt());
        store.save(tokens);
        return tokens;
    }

    public AuthTokens load() throws Exception {
        return store.load();
    }

    public AuthTokens current() throws Exception {
        return load();
    }

    public void clear() {
        store.clear();
    }

    public boolean hasSession() {
        try {
            AuthTokens t = store.load();
            return t != null && t.accessToken() != null && !t.accessToken().isBlank();
        } catch (Exception e) {
            return false;
        }
    }

    public boolean accessTokenExpired() {
        try {
            AuthTokens t = store.load();
            return t == null || t.expiresAtEpochMs() <= System.currentTimeMillis();
        } catch (Exception e) {
            return true;
        }
    }

    public static String errorMessage(Response<?> response) {
        return ApiErrorMapper.from(response).getMessage();
    }
}
