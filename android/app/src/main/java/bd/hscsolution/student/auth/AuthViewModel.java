package bd.hscsolution.student.auth;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class AuthViewModel extends ViewModel {
    private final AuthRepository repository;
    private final MutableLiveData<AuthState> state = new MutableLiveData<>(new AuthState.SignedOut());
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public AuthViewModel(AuthRepository repository) { this.repository = repository; }
    public LiveData<AuthState> state() { return state; }

    public void restore() {
        executor.execute(() -> state.postValue(repository.hasSession()
                ? new AuthState.SignedIn() : new AuthState.SignedOut()));
    }

    public void login(String email, String password) {
        state.postValue(new AuthState.Loading());
        executor.execute(() -> {
            try {
                repository.login(email.trim(), password);
                state.postValue(new AuthState.SignedIn());
            } catch (Exception e) {
                state.postValue(new AuthState.Error(userMessage(e)));
            }
        });
    }

    public void register(String email, String password, String displayName, String language) {
        state.postValue(new AuthState.Loading());
        executor.execute(() -> {
            try {
                repository.register(email.trim(), password, displayName.trim(), language);
                state.postValue(new AuthState.Registered(email.trim()));
            } catch (Exception e) {
                state.postValue(new AuthState.Error(userMessage(e)));
            }
        });
    }

    public void logout() {
        state.postValue(new AuthState.Loading());
        executor.execute(() -> {
            repository.logout();
            state.postValue(new AuthState.SignedOut());
        });
    }

    public void sessionExpired() {
        repository.logout();
        state.postValue(new AuthState.Error("Your session expired. Please log in again."));
    }

    private String userMessage(Exception e) {
        if (e instanceof bd.hscsolution.student.network.ApiException ae) {
            if (ae.error().httpCode() == 401) return "Email or password is incorrect.";
            if (ae.error().httpCode() == 409) return "An account with this email already exists.";
            if (ae.error().httpCode() == 429) return "Too many attempts. Please wait and try again.";
            if (ae.error().httpCode() == 0) return ae.error().message();
            return ae.error().message();
        }
        return e.getMessage() == null ? "Something went wrong. Please try again." : e.getMessage();
    }

    @Override protected void onCleared() { executor.shutdownNow(); }
}
