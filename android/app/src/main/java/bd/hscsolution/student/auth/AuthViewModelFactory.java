package bd.hscsolution.student.auth;
import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
public final class AuthViewModelFactory implements ViewModelProvider.Factory {
    private final AuthRepository repository;
    public AuthViewModelFactory(AuthRepository repository) { this.repository = repository; }
    @NonNull @Override public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(AuthViewModel.class)) return (T) new AuthViewModel(repository);
        throw new IllegalArgumentException("Unknown ViewModel: " + modelClass);
    }
}
