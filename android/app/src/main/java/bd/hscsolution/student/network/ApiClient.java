package bd.hscsolution.student.network;

import android.content.Context;
import bd.hscsolution.student.BuildConfig;
import bd.hscsolution.student.auth.AuthManager;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import bd.hscsolution.student.content.ContentApi;

public final class ApiClient {
    private final Retrofit retrofit;
    public ApiClient(Context context) {
        AuthManager auth = new AuthManager(context);
        // First Retrofit instance is used only for auth endpoints by the authenticator.
        Retrofit authRetrofit = new Retrofit.Builder().baseUrl(BuildConfig.API_BASE_URL)
                .addConverterFactory(GsonConverterFactory.create()).build();
        AuthApi authApi = authRetrofit.create(AuthApi.class);
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BASIC); // never BODY: avoids credentials/tokens in logs.
        OkHttpClient http = new OkHttpClient.Builder()
                .addInterceptor(new AuthInterceptor(auth))
                .authenticator(new SessionAuthenticator(auth, authApi))
                .addInterceptor(logging)
                .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(20, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(20, java.util.concurrent.TimeUnit.SECONDS)
                .build();
        retrofit = new Retrofit.Builder().baseUrl(BuildConfig.API_BASE_URL)
                .client(http).addConverterFactory(GsonConverterFactory.create()).build();
    }
    public AuthApi authApi() { return retrofit.create(AuthApi.class); }
    public ContentApi contentApi() { return retrofit.create(ContentApi.class); }
}
