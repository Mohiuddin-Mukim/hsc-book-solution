package bd.hscsolution.security;
import static org.junit.jupiter.api.Assertions.*; import java.util.List; import org.junit.jupiter.api.Test;
class JwtServiceTest{ @Test void roundTrip(){var p=new SecurityProperties("12345678901234567890123456789012",15,30,"http://localhost:5173","","","",5,900);var s=new JwtService(p);String t=s.createAccessToken("u","e@example.com",List.of("ADMIN"),List.of("CONTENT_READ"));assertEquals("u",s.subject(t));assertTrue(s.expiration(t).isAfter(java.time.Instant.now()));}}
