package bd.hscsolution.security;

import bd.hscsolution.api.contract.ApiError;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.*;

import java.time.Instant;
import java.util.Map;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

@Component
public class JsonAccessDeniedHandler implements AccessDeniedHandler {
    private final ObjectMapper mapper;

    public JsonAccessDeniedHandler(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public void handle(HttpServletRequest r, HttpServletResponse s, AccessDeniedException e) {
        try {
            s.setStatus(403);
            s.setContentType("application/json");
            mapper.writeValue(s.getOutputStream(), new ApiError("FORBIDDEN", "You do not have permission for this action", r.getHeader("X-Correlation-Id"), Instant.now(), Map.of()));
        } catch (Exception ignored) {
        }
    }
}
