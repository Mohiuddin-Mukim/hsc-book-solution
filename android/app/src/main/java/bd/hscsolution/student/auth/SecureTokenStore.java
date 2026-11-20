package bd.hscsolution.student.auth;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/** Encrypted-at-rest token storage. Never logs token values. */
public final class SecureTokenStore {
    private static final String PREF = "auth_secure";
    private static final String KEY = "token_key";
    private final Context context;

    public SecureTokenStore(Context context) { this.context = context.getApplicationContext(); }

    private SecretKey key() throws Exception {
        KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
        ks.load(null);
        if (!ks.containsAlias(KEY)) {
            KeyGenerator kg = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
            kg.init(new KeyGenParameterSpec.Builder(KEY,
                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .build());
            kg.generateKey();
        }
        return (SecretKey) ks.getKey(KEY, null);
    }

    public synchronized void save(AuthTokens tokens) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key());
        // Length-delimited JSON avoids ambiguous token separators.
        String payload = new org.json.JSONObject()
                .put("accessToken", tokens.accessToken())
                .put("refreshToken", tokens.refreshToken())
                .put("expiresAt", tokens.expiresAtEpochMs())
                .toString();
        String iv = Base64.encodeToString(cipher.getIV(), Base64.NO_WRAP);
        String ct = Base64.encodeToString(cipher.doFinal(payload.getBytes(StandardCharsets.UTF_8)), Base64.NO_WRAP);
        prefs().edit().putString("iv", iv).putString("ct", ct).apply();
    }

    public synchronized AuthTokens load() throws Exception {
        String iv = prefs().getString("iv", null);
        String ct = prefs().getString("ct", null);
        if (iv == null || ct == null) return null;
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, key(),
                new GCMParameterSpec(128, Base64.decode(iv, Base64.NO_WRAP)));
        org.json.JSONObject payload = new org.json.JSONObject(
                new String(cipher.doFinal(Base64.decode(ct, Base64.NO_WRAP)), StandardCharsets.UTF_8));
        return new AuthTokens(payload.getString("accessToken"),
                payload.getString("refreshToken"), payload.getLong("expiresAt"));
    }

    public synchronized void clear() { prefs().edit().clear().apply(); }
    private SharedPreferences prefs() { return context.getSharedPreferences(PREF, Context.MODE_PRIVATE); }
}
