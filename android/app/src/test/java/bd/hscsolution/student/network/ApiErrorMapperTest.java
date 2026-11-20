package bd.hscsolution.student.network;

import static org.junit.Assert.*;
import org.junit.Test;
import retrofit2.Response;

public class ApiErrorMapperTest {
    @Test
    public void maps401ToStableCode() {
        String json = "{\"code\":\"AUTH_REQUIRED\",\"message\":\"Unauthorized\"}";
        ApiException e = ApiErrorMapper.from(Response.error(401,
                okhttp3.ResponseBody.create(json,
                        okhttp3.MediaType.parse("application/json"))));
        assertEquals(401, e.error().httpCode());
        assertEquals("AUTH_REQUIRED", e.error().code());
    }
}
