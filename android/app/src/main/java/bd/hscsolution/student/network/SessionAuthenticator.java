package bd.hscsolution.student.network;

import bd.hscsolution.student.auth.AuthManager;
import bd.hscsolution.student.auth.SessionEvents;
import java.io.IOException;
import okhttp3.Authenticator;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;

/** Refreshes an expired access token once; never logs token values. */
public final class SessionAuthenticator implements Authenticator {
    private final AuthManager manager;
    private final AuthApi api;
    public SessionAuthenticator(AuthManager manager, AuthApi api) { this.manager = manager; this.api = api; }

    @Override public synchronized Request authenticate(Route route, Response response) throws IOException {
        if (responseCount(response) >= 2) return null;
        try {
            var current = manager.load();
            if (current == null) return null;
            var refreshed = api.refresh(new AuthApi.RefreshRequest(current.refreshToken())).execute();
            if (!refreshed.isSuccessful() || refreshed.body() == null) {
                manager.clear();
                SessionEvents.notifyExpired();
                return null;
            }
            manager.saveResponse(refreshed.body());
            var updated = manager.load();
            return response.request().newBuilder()
                    .header("Authorization", "Bearer " + updated.accessToken())
                    .build();
        } catch (Exception e) {
            manager.clear();
            SessionEvents.notifyExpired();
            return null;
        }
    }

    private int responseCount(Response response) {
        int result = 1;
        while ((response = response.priorResponse()) != null) result++;
        return result;
    }
}
