package bd.hscsolution.student.auth;

import bd.hscsolution.student.network.ApiErrorMapper;
import bd.hscsolution.student.network.AuthApi;
import retrofit2.Response;

public final class AuthRepository {
    private final AuthApi api;
    private final AuthManager manager;

    public AuthRepository(AuthApi api, AuthManager manager) {
        this.api = api;
        this.manager = manager;
    }

    public AuthTokens login(String email, String password) throws Exception {
        Response<AuthApi.AuthResponse> r = api.login(new AuthApi.LoginRequest(email, password)).execute();
        if (!r.isSuccessful()) throw ApiErrorMapper.from(r);
        return manager.saveResponse(r.body());
    }

    public AuthApi.RegisterResponse register(String email, String password, String displayName, String language) throws Exception {
        Response<AuthApi.RegisterResponse> r = api.register(
                new AuthApi.RegisterRequest(email, password, displayName, language)).execute();
        if (!r.isSuccessful()) throw ApiErrorMapper.from(r);
        return r.body();
    }

    public AuthTokens refresh() throws Exception {
        AuthTokens current = manager.load();
        if (current == null) throw new IllegalStateException("Session expired. Please log in again.");
        Response<AuthApi.AuthResponse> r = api.refresh(new AuthApi.RefreshRequest(current.refreshToken())).execute();
        if (!r.isSuccessful()) {
            manager.clear();
            throw ApiErrorMapper.from(r);
        }
        return manager.saveResponse(r.body());
    }

    public void logout() {
        try {
            AuthTokens t = manager.load();
            if (t != null) api.logout(new AuthApi.RefreshRequest(t.refreshToken())).execute();
        } catch (Exception ignored) {
        } finally {
            manager.clear();
        }
    }

    public boolean hasSession() { return manager.hasSession(); }
}
