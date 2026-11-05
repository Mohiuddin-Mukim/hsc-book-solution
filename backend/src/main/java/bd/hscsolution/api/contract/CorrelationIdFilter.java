package bd.hscsolution.api.contract;
import jakarta.servlet.*; import jakarta.servlet.http.*; import org.springframework.web.filter.OncePerRequestFilter; import java.io.IOException; import java.util.UUID;
public class CorrelationIdFilter extends OncePerRequestFilter {
 public static final String HEADER="X-Correlation-Id";
 protected void doFilterInternal(HttpServletRequest r,HttpServletResponse s,FilterChain c)throws ServletException,IOException{
  String id=r.getHeader(HEADER); if(id==null||id.isBlank()) id=UUID.randomUUID().toString(); s.setHeader(HEADER,id); c.doFilter(r,s);
 }}
