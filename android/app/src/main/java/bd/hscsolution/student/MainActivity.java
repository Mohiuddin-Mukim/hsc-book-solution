package bd.hscsolution.student;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import bd.hscsolution.student.auth.AuthState;
import bd.hscsolution.student.auth.AuthViewModel;
import bd.hscsolution.student.auth.AuthViewModelFactory;
import bd.hscsolution.student.auth.SessionEvents;
import bd.hscsolution.student.offline.WorkScheduler;

public final class MainActivity extends AppCompatActivity {
    private AuthViewModel authViewModel;
    private NavController nav;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_main);

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host);
        if (navHostFragment != null) {
            nav = navHostFragment.getNavController();
        } else {
            throw new IllegalStateException("NavHostFragment not found for R.id.nav_host");
        }

        authViewModel = new ViewModelProvider(this,
                new AuthViewModelFactory(((HscApplication) getApplication()).container().authRepository))
                .get(AuthViewModel.class);

        authViewModel.state().observe(this, s -> {
            if (s instanceof AuthState.SignedIn) {
                if (nav.getCurrentDestination() != null && nav.getCurrentDestination().getId() == R.id.loginFragment)
                    nav.navigate(R.id.action_login_to_home);
            } else if (s instanceof AuthState.SignedOut || s instanceof AuthState.Error) {
                if (nav.getCurrentDestination() != null && nav.getCurrentDestination().getId() == R.id.homeFragment)
                    nav.navigate(R.id.action_home_to_login);
            }
        });

        SessionEvents.expired().observe(this, expired -> {
            if (Boolean.TRUE.equals(expired)) {
                SessionEvents.clear();
                authViewModel.sessionExpired();
            }
        });

        if (state == null) authViewModel.restore();
        WorkScheduler.scheduleBaselineSync(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            if (((HscApplication) getApplication()).container().authManager.accessTokenExpired())
                authViewModel.sessionExpired();
        } catch (Exception ignored) {}
    }
}
