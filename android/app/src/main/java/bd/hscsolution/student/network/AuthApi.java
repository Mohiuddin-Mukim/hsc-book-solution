package bd.hscsolution.student.network;

import com.google.gson.annotations.SerializedName;
import java.time.Instant;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface AuthApi {
    @POST("auth/register")
    Call<RegisterResponse> register(@Body RegisterRequest request);

    @Headers("X-Client-Type: ANDROID")
    @POST("auth/login")
    Call<AuthResponse> login(@Body LoginRequest request);

    @Headers("X-Client-Type: ANDROID")
    @POST("auth/refresh")
    Call<AuthResponse> refresh(@Body RefreshRequest request);

    @Headers("X-Client-Type: ANDROID")
    @POST("auth/logout")
    Call<Void> logout(@Body RefreshRequest request);

    record RegisterRequest(String email, String password, String displayName, String preferredLanguage) {}
    record RegisterResponse(String userId, String email, String displayName, String status, String role, String preferredLanguage) {}
    record LoginRequest(String email, String password) {}
    record RefreshRequest(String refreshToken) {}

    record AuthResponse(
            String accessToken,
            String refreshToken,
            @SerializedName("expiresAt") String expiresAtRaw,
            String userId,
            String email,
            List<String> roles,
            List<String> permissions) {

        public long expiresAt() {
            if (expiresAtRaw == null || expiresAtRaw.isBlank()) {
                return 0L;
            }
            try {
                return Instant.parse(expiresAtRaw).toEpochMilli();
            } catch (Exception e) {
                return Long.parseLong(expiresAtRaw);
            }
        }
    }
}
