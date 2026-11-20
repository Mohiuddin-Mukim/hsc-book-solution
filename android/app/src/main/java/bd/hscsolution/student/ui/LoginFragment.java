package bd.hscsolution.student.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import bd.hscsolution.student.AppContainer;
import bd.hscsolution.student.HscApplication;
import bd.hscsolution.student.R;
import bd.hscsolution.student.auth.AuthState;
import bd.hscsolution.student.auth.AuthViewModel;
import bd.hscsolution.student.auth.AuthViewModelFactory;

public final class LoginFragment extends Fragment {
    private AuthViewModel vm;
    private EditText email, password;
    private Button login;
    private ProgressBar progress;
    private TextView error;

    public LoginFragment() { super(R.layout.fragment_login); }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        super.onViewCreated(view, state);
        email = view.findViewById(R.id.emailInput);
        password = view.findViewById(R.id.passwordInput);
        login = view.findViewById(R.id.loginButton);
        progress = view.findViewById(R.id.loginProgress);
        error = view.findViewById(R.id.errorText);
        vm = new ViewModelProvider(requireActivity(),
                new AuthViewModelFactory(((HscApplication) requireActivity().getApplication()).container().authRepository))
                .get(AuthViewModel.class);

        view.findViewById(R.id.createAccountButton).setOnClickListener(
                v -> Navigation.findNavController(v).navigate(R.id.action_login_to_register));

        login.setOnClickListener(v -> {
            error.setText("");
            if (email.getText().toString().trim().isEmpty() || password.getText().toString().isEmpty()) {
                error.setText("Email and password are required.");
                return;
            }
            vm.login(email.getText().toString(), password.getText().toString());
        });
        vm.state().observe(getViewLifecycleOwner(), s -> {
            if (s instanceof AuthState.Loading) { progress.setVisibility(View.VISIBLE); login.setEnabled(false); }
            else { progress.setVisibility(View.GONE); login.setEnabled(true); }
            if (s instanceof AuthState.Error e) error.setText(e.message());
        });
    }
}
