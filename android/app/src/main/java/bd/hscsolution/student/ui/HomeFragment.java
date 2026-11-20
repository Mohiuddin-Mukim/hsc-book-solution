package bd.hscsolution.student.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import bd.hscsolution.student.HscApplication;
import bd.hscsolution.student.R;
import bd.hscsolution.student.auth.AuthViewModel;
import bd.hscsolution.student.auth.AuthViewModelFactory;
import bd.hscsolution.student.content.CatalogModels;
import bd.hscsolution.student.content.CatalogState;
import bd.hscsolution.student.content.CatalogViewModel;
import bd.hscsolution.student.content.CatalogViewModelFactory;
import java.util.List;

public final class HomeFragment extends Fragment {
    public HomeFragment() { super(R.layout.fragment_home); }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        super.onViewCreated(view, state);

        TextView network = view.findViewById(R.id.networkState);
        ProgressBar progress = view.findViewById(R.id.catalogProgress);
        TextView error = view.findViewById(R.id.catalogError);
        TextView empty = view.findViewById(R.id.emptyState);
        Button retry = view.findViewById(R.id.retryButton);
        RecyclerView list = view.findViewById(R.id.subjectList);
        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        SubjectAdapter adapter = new SubjectAdapter(item -> {
            Bundle args = new Bundle();
            args.putString("subjectId", item.id());
            args.putString("subjectName", item.name());
            Navigation.findNavController(view).navigate(R.id.action_home_to_papers, args);
        });
        list.setAdapter(adapter);

        var container = ((HscApplication) requireActivity().getApplication()).container();
        AuthViewModel authVm = new ViewModelProvider(requireActivity(),
                new AuthViewModelFactory(container.authRepository)).get(AuthViewModel.class);
        view.findViewById(R.id.logoutButton).setOnClickListener(v -> authVm.logout());

        CatalogViewModel catalogVm = new ViewModelProvider(this,
                new CatalogViewModelFactory(container.contentRepository)).get(CatalogViewModel.class);
        catalogVm.state().observe(getViewLifecycleOwner(), catalogState -> {
            if (catalogState instanceof CatalogState.Loading) {
                progress.setVisibility(View.VISIBLE);
                error.setVisibility(View.GONE);
                retry.setVisibility(View.GONE);
                empty.setVisibility(View.GONE);
            } else {
                progress.setVisibility(View.GONE);
            }
            if (catalogState instanceof CatalogState.Error failure) {
                error.setText(failure.message());
                error.setVisibility(View.VISIBLE);
                retry.setVisibility(View.VISIBLE);
                empty.setVisibility(View.GONE);
                list.setVisibility(View.GONE);
            } else if (catalogState instanceof CatalogState.Success success) {
                @SuppressWarnings("unchecked")
                CatalogModels.PageResponse<CatalogModels.SubjectItem> page =
                        (CatalogModels.PageResponse<CatalogModels.SubjectItem>) success.data();
                List<CatalogModels.SubjectItem> subjects = page == null ? List.of() : page.items();
                adapter.submit(subjects);
                error.setVisibility(View.GONE);
                retry.setVisibility(View.GONE);
                empty.setVisibility(subjects == null || subjects.isEmpty() ? View.VISIBLE : View.GONE);
                list.setVisibility(subjects == null || subjects.isEmpty() ? View.GONE : View.VISIBLE);
            }
        });
        retry.setOnClickListener(v -> catalogVm.subjects());
        catalogVm.subjects();

        ConnectivityMonitor monitor = new ConnectivityMonitor(requireContext());
        monitor.start();
        monitor.connected().observe(getViewLifecycleOwner(),
                ok -> network.setText(ok ? R.string.online : R.string.offline));
        getViewLifecycleOwner().getLifecycle().addObserver(new androidx.lifecycle.DefaultLifecycleObserver() {
            @Override public void onDestroy(@NonNull androidx.lifecycle.LifecycleOwner owner) { monitor.stop(); }
        });
    }
}
