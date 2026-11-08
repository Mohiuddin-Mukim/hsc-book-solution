package bd.hscsolution.api;
import bd.hscsolution.api.contract.ApiError; import jakarta.servlet.http.HttpServletRequest; import java.time.Instant; import java.util.Map; import org.springframework.http.*; import org.springframework.web.bind.MethodArgumentNotValidException; import org.springframework.web.bind.annotation.*; import org.springframework.web.server.ResponseStatusException;
@RestControllerAdvice public class GlobalExceptionHandler{
 @ExceptionHandler(ResponseStatusException.class) ResponseEntity<ApiError> response(ResponseStatusException e,HttpServletRequest r){return body(e.getStatusCode(),e.getReason(),r);}
 @ExceptionHandler(MethodArgumentNotValidException.class) ResponseEntity<ApiError> validation(MethodArgumentNotValidException e,HttpServletRequest r){return body(HttpStatus.BAD_REQUEST,"Validation failed",r,Map.of("fields",e.getBindingResult().getFieldErrors().stream().collect(java.util.stream.Collectors.toMap(x->x.getField(),x->x.getDefaultMessage(),(a,b)->a))));}
 @ExceptionHandler(IllegalArgumentException.class) ResponseEntity<ApiError> bad(IllegalArgumentException e,HttpServletRequest r){return body(HttpStatus.BAD_REQUEST,e.getMessage(),r);}
 @ExceptionHandler(Exception.class) ResponseEntity<ApiError> other(Exception e,HttpServletRequest r){return body(HttpStatus.INTERNAL_SERVER_ERROR,"Unexpected server error",r);}
 private ResponseEntity<ApiError> body(HttpStatusCode s,String m,HttpServletRequest r){return body(s,m,r,Map.of());}
 private ResponseEntity<ApiError> body(HttpStatusCode s,String m,HttpServletRequest r,Map<String,Object>d){return ResponseEntity.status(s).body(new ApiError(s.toString(),m,r.getHeader("X-Correlation-Id"),Instant.now(),d));}
}
