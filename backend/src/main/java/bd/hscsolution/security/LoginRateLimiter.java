package bd.hscsolution.security;

import java.time.Instant; import java.util.concurrent.ConcurrentHashMap; import java.util.concurrent.atomic.AtomicInteger; import org.springframework.stereotype.Component;
@Component public class LoginRateLimiter {
 private record Window(Instant started,AtomicInteger count){}
 private final ConcurrentHashMap<String,Window> windows=new ConcurrentHashMap<>();
 public boolean allow(String key,int limit,long windowSeconds){
   Instant now=Instant.now(); Window w=windows.compute(key,(k,v)-> v==null||now.isAfter(v.started().plusSeconds(windowSeconds))?new Window(now,new AtomicInteger(1)):v);
   return w.count().get()<=limit;
 }
 public void clear(String key){windows.remove(key);}
}
