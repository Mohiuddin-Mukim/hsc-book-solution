package bd.hscsolution.student.network;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import okhttp3.ResponseBody;
import retrofit2.Response;

public final class ApiErrorMapper {
    private ApiErrorMapper() {}

    public static ApiException from(Response<?> response) {
        String message = "Request failed.";
        String code = "HTTP_" + response.code();
        ResponseBody body = response.errorBody();
        if (body != null) {
            try {
                JsonObject o = JsonParser.parseString(body.string()).getAsJsonObject();
                if (o.has("message") && !o.get("message").isJsonNull()) message = o.get("message").getAsString();
                if (o.has("code") && !o.get("code").isJsonNull()) code = o.get("code").getAsString();
            } catch (Exception ignored) {}
        }
        return new ApiException(new ApiError(response.code(), code, message));
    }

    public static ApiException network(IOException e) {
        return new ApiException(new ApiError(0, "NETWORK_ERROR", "Network unavailable. Check your connection and try again."));
    }
}
