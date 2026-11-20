package bd.hscsolution.student;

import android.content.Context;
import bd.hscsolution.student.auth.AuthManager;
import bd.hscsolution.student.auth.AuthRepository;
import bd.hscsolution.student.network.ApiClient;
import bd.hscsolution.student.network.AuthApi;

/** Small explicit dependency container; no reflection/code generation is needed for Java. */
public final class AppContainer {
    public final AuthManager authManager;
    public final AuthRepository authRepository;
    public final AppDatabase database;
    public final bd.hscsolution.student.content.ContentRepository contentRepository;

    public AppContainer(Context context) {
        ApiClient apiClient = new ApiClient(context);
        authManager = new AuthManager(context);
        AuthApi authApi = apiClient.authApi();
        authRepository = new AuthRepository(authApi, authManager);
        contentRepository = new bd.hscsolution.student.content.ContentRepository(apiClient);
        database = AppDatabase.get(context);
    }
}
