package bd.hscsolution.security;

import bd.hscsolution.api.contract.ApiError;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.*;

import java.time.Instant;
import java.util.Map;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Component
public class JsonAuthenticationEntryPoint implements AuthenticationEntryPoint {
    private final ObjectMapper mapper;

    public JsonAuthenticationEntryPoint(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                          AuthenticationException exception) {
        try {
            // Do not send a WWW-Authenticate challenge. Otherwise browsers may show
            // their native username/password popup instead of the Admin UI error.
            response.setHeader("WWW-Authenticate", null);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            mapper.writeValue(
                response.getOutputStream(),
                new ApiError(
                    "UNAUTHORIZED",
                    "Authentication is required",
                    request.getHeader("X-Correlation-Id"),
                    Instant.now(),
                    Map.of()
                )
            );
        } catch (Exception ignored) {
            // Keep the authentication failure from leaking a server-side exception.
        }
    }
}
