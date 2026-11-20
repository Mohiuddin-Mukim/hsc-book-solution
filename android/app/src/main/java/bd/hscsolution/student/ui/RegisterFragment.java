package bd.hscsolution.student.ui;

import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import bd.hscsolution.student.HscApplication;
import bd.hscsolution.student.R;
import bd.hscsolution.student.auth.AuthState;
import bd.hscsolution.student.auth.AuthViewModel;
import bd.hscsolution.student.auth.AuthViewModelFactory;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public final class RegisterFragment extends Fragment {
    private AuthViewModel vm;
    private Button register;
    private ProgressBar progress;
    private TextView error;

    public RegisterFragment() { super(R.layout.fragment_register); }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        super.onViewCreated(view, state);
        TextInputEditText name = view.findViewById(R.id.nameInput);
        TextInputEditText email = view.findViewById(R.id.registerEmailInput);
        TextInputEditText password = view.findViewById(R.id.registerPasswordInput);
        TextInputEditText confirm = view.findViewById(R.id.confirmPasswordInput);
        Spinner language = view.findViewById(R.id.languageSpinner);
        register = view.findViewById(R.id.registerButton);
        progress = view.findViewById(R.id.registerProgress);
        error = view.findViewById(R.id.registerError);

        vm = new ViewModelProvider(requireActivity(),
                new AuthViewModelFactory(((HscApplication) requireActivity().getApplication()).container().authRepository))
                .get(AuthViewModel.class);

        register.setOnClickListener(v -> {
            error.setText("");
            String n = text(name), e = text(email), p = text(password), c = text(confirm);
            if (n.length() < 2) { error.setText("Please enter your name."); return; }
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(e).matches()) { error.setText("Enter a valid email address."); return; }
            if (p.length() < 8) { error.setText("Password must be at least 8 characters."); return; }
            if (!p.equals(c)) { error.setText("Passwords do not match."); return; }
            String lang = language.getSelectedItemPosition() == 1 ? "en" : "bn";
            vm.register(e, p, n, lang);
        });

        view.findViewById(R.id.backToLoginButton).setOnClickListener(
                v -> Navigation.findNavController(v).navigateUp());

        vm.state().observe(getViewLifecycleOwner(), s -> {
            if (s instanceof AuthState.Loading) {
                progress.setVisibility(View.VISIBLE);
                register.setEnabled(false);
            } else {
                progress.setVisibility(View.GONE);
                register.setEnabled(true);
            }
            if (s instanceof AuthState.Error e) error.setText(e.message());
            if (s instanceof AuthState.Registered r) {
                Toast.makeText(requireContext(),
                        "Account created. You can now sign in.", Toast.LENGTH_LONG).show();
                Navigation.findNavController(view).navigate(R.id.action_register_to_login);
            }
        });
    }

    private String text(TextInputEditText e) {
        return e.getText() == null ? "" : e.getText().toString().trim();
    }
}
