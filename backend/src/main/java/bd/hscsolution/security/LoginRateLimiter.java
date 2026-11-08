package bd.hscsolution.security;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Component;

@Component
public class LoginRateLimiter {
    private record Window(Instant started, AtomicInteger count) {
    }

    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    public boolean allow(String key, int limit, long windowSeconds) {
        Instant now = Instant.now();

        // ১. উইন্ডো না থাকলে বা মেয়াদ শেষ হলে নতুন উইন্ডো তৈরি হবে (কাউন্ট শুরু হবে ০ থেকে)
        Window w = windows.compute(key, (k, v) ->
                v == null || now.isAfter(v.started().plusSeconds(windowSeconds))
                        ? new Window(now, new AtomicInteger(0))
                        : v
        );

        // ২. প্রতিটা রিকোয়েস্টে কাউন্টার ১ করে বাড়বে এবং বর্তমান মানটি লিমিটের সাথে চেক হবে
        return w.count().incrementAndGet() <= limit;
    }

    public void clear(String key) {
        windows.remove(key);
    }
}
