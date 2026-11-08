package bd.hscsolution.api.contract;

import java.time.Instant;
import java.util.Map;

public record ApiError(String code, String message, String correlationId, Instant timestamp,
                       Map<String, Object> details) {
}
