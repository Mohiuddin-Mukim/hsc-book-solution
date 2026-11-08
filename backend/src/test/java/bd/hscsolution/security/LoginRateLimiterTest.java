package bd.hscsolution.security;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class LoginRateLimiterTest {
    @Test
    void blocksAfterLimit() {
        var l = new LoginRateLimiter();
        assertTrue(l.allow("a", 2, 60));
        assertTrue(l.allow("a", 2, 60));
        assertFalse(l.allow("a", 2, 60));
        l.clear("a");
        assertTrue(l.allow("a", 2, 60));
    }
}
