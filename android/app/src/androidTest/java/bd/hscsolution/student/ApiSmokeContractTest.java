package bd.hscsolution.student;

import static org.junit.Assert.*;
import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import org.junit.Test;

public class ApiSmokeContractTest {
    @Test public void apiBaseUrlIsConfigured() {
        Context c = ApplicationProvider.getApplicationContext();
        assertNotNull(c);
        assertTrue(BuildConfig.API_BASE_URL.startsWith("http"));
        assertTrue(BuildConfig.API_BASE_URL.endsWith("/"));
    }
}
