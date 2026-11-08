package bd.hscsolution.student.auth;

import android.content.Context;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import org.json.JSONObject;

public final class AuthManager {
    private final SecureTokenStore store;

    public AuthManager(Context c) {
        this.store = new SecureTokenStore(c);
    }

    public AuthTokens login(String baseUrl, String email, String password) throws Exception {
        HttpURLConnection x = open(baseUrl + "/auth/login", "POST");
        JSONObject b = new JSONObject().put("email", email).put("password", password);
        write(x, b);
        if (x.getResponseCode() != 200)
            throw new IllegalStateException("Login failed: " + x.getResponseCode());
        return save(parse(x));
    }

    public AuthTokens refresh(String baseUrl) throws Exception {
        AuthTokens old = store.load();
        if (old == null)
            throw new IllegalStateException("No refresh token");
        HttpURLConnection x = open(baseUrl + "/auth/refresh", "POST");
        write(x, new JSONObject().put("refreshToken", old.refreshToken()));
        if (x.getResponseCode() != 200) {
            store.clear();
            throw new IllegalStateException("Refresh failed: " + x.getResponseCode());
        }
        return save(parse(x));
    }

    public void logout() {
        store.clear();
    }

    public AuthTokens current() throws Exception {
        return store.load();
    }

    private AuthTokens save(AuthTokens t) throws Exception {
        store.save(t);
        return t;
    }

    private HttpURLConnection open(String url, String method) throws Exception {
        HttpURLConnection x = (HttpURLConnection) new URL(url).openConnection();
        x.setRequestMethod(method);
        x.setDoOutput(true);
        x.setRequestProperty("Content-Type", "application/json");
        x.setRequestProperty("X-Client-Type", "ANDROID");
        return x;
    }

    private void write(HttpURLConnection x, JSONObject body) throws Exception {
        x.getOutputStream().write(body.toString().getBytes(StandardCharsets.UTF_8));
    }

    private AuthTokens parse(HttpURLConnection x) throws Exception {
        JSONObject j = new JSONObject(read(x));
        return new AuthTokens(j.getString("accessToken"), j.getString("refreshToken"), j.getLong("expiresAt"));
    }

    private String read(HttpURLConnection x) throws Exception {
        try (BufferedReader r = new BufferedReader(new InputStreamReader(x.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder b = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null)
                b.append(line);
            return b.toString();
        }
    }
}
