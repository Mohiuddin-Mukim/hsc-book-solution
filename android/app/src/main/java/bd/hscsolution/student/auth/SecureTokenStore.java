package bd.hscsolution.student.auth;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import java.security.KeyStore;
import android.util.Base64;
import java.nio.charset.StandardCharsets;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

public final class SecureTokenStore {
    private static final String PREF = "auth_secure";
    private static final String KEY = "token_key";
    private final Context c;

    public SecureTokenStore(Context c) {
        this.c = c.getApplicationContext();
    }

    private SecretKey key() throws Exception {
        var ks = KeyStore.getInstance("AndroidKeyStore");
        ks.load(null);
        if (!ks.containsAlias(KEY)) {
            var kg = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
            kg.init(new KeyGenParameterSpec.Builder(KEY, KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());
            kg.generateKey();
        }
        return ((SecretKey) ks.getKey(KEY, null));
    }

 public void save(AuthTokens t)throws Exception{Cipher x=Cipher.getInstance("AES/GCM/NoPadding");x.init(Cipher.ENCRYPT_MODE,key());String payload=t.accessToken()+" "+t.refreshToken()+" "+t.expiresAtEpochMs();String iv=Base64.encodeToString(x.getIV(),Base64.NO_WRAP);String ct=Base64.encodeToString(x.doFinal(payload.getBytes(StandardCharsets.UTF_8)),Base64.NO_WRAP);prefs().edit().putString("iv",iv).putString("ct",ct).apply();}

    public AuthTokens load() throws Exception {
        String iv = prefs().getString("iv", null), ct = prefs().getString("ct", null);
        if (iv == null || ct == null)
            return null;
        Cipher x = Cipher.getInstance("AES/GCM/NoPadding");
        x.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(128, Base64.decode(iv, Base64.NO_WRAP)));
        String[] p = new String(x.doFinal(Base64.decode(ct, Base64.NO_WRAP)), StandardCharsets.UTF_8).split("\n", 3);
        return new AuthTokens(p[0], p[1], Long.parseLong(p[2]));
    }

    public void clear() {
        prefs().edit().clear().apply();
    }

    private SharedPreferences prefs() {
        return c.getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }
}
