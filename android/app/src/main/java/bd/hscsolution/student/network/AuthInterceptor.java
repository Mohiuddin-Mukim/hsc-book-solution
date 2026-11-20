package bd.hscsolution.student.network;

import bd.hscsolution.student.auth.AuthManager;
import bd.hscsolution.student.auth.AuthTokens;
import java.io.IOException;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public final class AuthInterceptor implements Interceptor {
    private final AuthManager authManager;
    public AuthInterceptor(AuthManager authManager) { this.authManager = authManager; }

    @Override public Response intercept(Chain chain) throws IOException {
        Request original = chain.request();
        if (original.url().encodedPath().endsWith("/auth/login") ||
            original.url().encodedPath().endsWith("/auth/refresh") ||
            original.url().encodedPath().endsWith("/auth/logout")) {
            return chain.proceed(original);
        }
        try {
            AuthTokens tokens = authManager.current();
            if (tokens != null && tokens.accessToken() != null && !tokens.accessToken().isBlank()) {
                original = original.newBuilder().header("Authorization", "Bearer " + tokens.accessToken()).build();
            }
        } catch (Exception ignored) {}
        return chain.proceed(original);
    }
}
