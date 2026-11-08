package bd.hscsolution.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
  private final SecurityProperties p;
  private final SecretKey key;
  public JwtService(SecurityProperties p){
    this.p=p;
    if(p.jwtSecret()==null || p.jwtSecret().length()<32) throw new IllegalStateException("JWT_SECRET must be at least 32 characters");
    key=Keys.hmacShaKeyFor(p.jwtSecret().getBytes(StandardCharsets.UTF_8));
  }
  public String createAccessToken(String userId,String email,List<String> roles,List<String> permissions){
    Instant now=Instant.now();
    return Jwts.builder().subject(userId).claim("email",email).claim("roles",roles).claim("permissions",permissions)
      .issuedAt(Date.from(now)).expiration(Date.from(now.plusSeconds(p.accessTokenMinutes()*60)))
      .signWith(key).compact();
  }
  public Claims parse(String token){ return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload(); }
  public String subject(String token){ return parse(token).getSubject(); }
  public Instant expiration(String token){ return parse(token).getExpiration().toInstant(); }
}
