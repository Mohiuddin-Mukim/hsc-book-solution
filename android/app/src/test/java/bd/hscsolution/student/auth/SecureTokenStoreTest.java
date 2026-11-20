package bd.hscsolution.student.auth;

import static org.junit.Assert.*;
import org.junit.Test;

/** Pure contract checks for token model; Android Keystore integration is exercised on device. */
public class SecureTokenStoreTest {
    @Test public void tokenModelKeepsExpiry() {
        AuthTokens t = new AuthTokens("access", "refresh", 123L);
        assertEquals("access", t.accessToken());
        assertEquals("refresh", t.refreshToken());
        assertEquals(123L, t.expiresAtEpochMs());
    }
}
