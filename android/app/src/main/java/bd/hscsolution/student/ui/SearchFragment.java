package bd.hscsolution.student.ui;

import android.os.*;
import android.view.*;
import android.widget.*;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import bd.hscsolution.student.HscApplication;
import bd.hscsolution.student.R;
import bd.hscsolution.student.content.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class SearchFragment extends Fragment {
    private SearchViewModel vm;
    private ContentRepository repository;
    private EditText query;
    // Spinner ভেরিয়েবল বাদ দিয়ে সিলেক্টেড পজিশন রাখা হলো
    private int selectedLangPos = 0, selectedTypePos = 0, selectedDiffPos = 0;
    private Button next, prev;
    private TextView pageLabel, empty, error;
    private int page = 0;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable pending;
    private final ExecutorService filterExecutor = Executors.newSingleThreadExecutor();
    private String subjectId, paperId, chapterId;

    private static final String[] LANG_OPTIONS = {"Bangla", "English", "All"};
    private static final String[] TYPE_OPTIONS = {"All types", "MCQ", "CQ", "SHORT", "NUMERICAL"};
    private static final String[] DIFF_OPTIONS = {"All difficulties", "EASY", "MEDIUM", "HARD"};

    public SearchFragment() { super(R.layout.fragment_search); }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle saved) {
        super.onViewCreated(view, saved);
        query = view.findViewById(R.id.searchInput);
        pageLabel = view.findViewById(R.id.searchPageLabel);
        next = view.findViewById(R.id.searchNextButton);
        prev = view.findViewById(R.id.searchPrevButton);
        empty = view.findViewById(R.id.searchEmpty);
        error = view.findViewById(R.id.searchError);

        RecyclerView list = view.findViewById(R.id.searchResultList);
        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        SearchAdapter adapter = new SearchAdapter(item -> {
            Bundle b = new Bundle();
            b.putString("questionId", item.id());
            b.putString("language", item.languageCode() == null ? "bn" : item.languageCode());
            Navigation.findNavController(view).navigate(R.id.action_search_to_detail, b);
        });
        list.setAdapter(adapter);

        var c = ((HscApplication) requireActivity().getApplication()).container();
        repository = c.contentRepository;
        vm = new ViewModelProvider(this, new SearchViewModelFactory(repository)).get(SearchViewModel.class);
        vm.state().observe(getViewLifecycleOwner(), s -> {
            if (s instanceof SearchState.Loading) {
                error.setVisibility(View.GONE); empty.setVisibility(View.GONE);
                view.findViewById(R.id.searchProgress).setVisibility(View.VISIBLE);
            } else view.findViewById(R.id.searchProgress).setVisibility(View.GONE);

            if (s instanceof SearchState.Error e) {
                error.setText(e.message()); error.setVisibility(View.VISIBLE);
                empty.setVisibility(View.GONE); next.setEnabled(false); prev.setEnabled(false);
            } else if (s instanceof SearchState.Success ok) {
                var d = ok.data();
                List<CatalogModels.SearchResult> rows = d.content() == null ? List.of() : d.content();
                adapter.submit(rows);
                error.setVisibility(View.GONE);
                empty.setText(rows.isEmpty() ? getString(R.string.search_no_results) : "");
                empty.setVisibility(rows.isEmpty() ? View.VISIBLE : View.GONE);
                pageLabel.setText("Page " + (d.page() + 1) + " of " + Math.max(1, d.totalPages()));
                prev.setEnabled(d.page() > 0);
                next.setEnabled(d.page() + 1 < d.totalPages());
            } else if (s instanceof SearchState.Idle) {
                adapter.submit(List.of()); empty.setText("Search by question ID, code, Bangla or English terms.");
                empty.setVisibility(View.VISIBLE);
            }
        });

        query.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int before, int count) {
                page = 0;
                if (pending != null) handler.removeCallbacks(pending);
                pending = () -> runSearch(0);
                handler.postDelayed(pending, 350);
            }
            @Override public void afterTextChanged(android.text.Editable e) {}
        });

        view.findViewById(R.id.filterButton).setOnClickListener(v -> showFilterSheet());
        next.setOnClickListener(v -> { page++; runSearch(page); });
        prev.setOnClickListener(v -> { if (page > 0) { page--; runSearch(page); } });
        view.findViewById(R.id.clearSearchButton).setOnClickListener(v -> {
            query.setText("");
            subjectId = null; paperId = null; chapterId = null;
            selectedLangPos = 0; selectedTypePos = 0; selectedDiffPos = 0;
            page = 0;
        });
    }

    private void configureSpinner(Spinner spinner, String[] values) {
        if (spinner == null) return;
        spinner.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item, values));
    }

    private void runSearch(int targetPage) {
        String q = query.getText() == null ? "" : query.getText().toString().trim();
        if (q.isEmpty()) { vm.cancel(); return; }
        vm.search(q, subjectId, paperId, chapterId,
                selectedLangPos == 0 ? "bn" : selectedLangPos == 1 ? "en" : null,
                selectedTypePos == 0 ? null : TYPE_OPTIONS[selectedTypePos],
                selectedDiffPos == 0 ? null : DIFF_OPTIONS[selectedDiffPos],
                targetPage);
    }

    private void showFilterSheet() {
        View sheet = getLayoutInflater().inflate(R.layout.search_filter_sheet, null, false);
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        dialog.setContentView(sheet);

        Spinner subject = sheet.findViewById(R.id.sheetSubject);
        Spinner paper = sheet.findViewById(R.id.sheetPaper);
        Spinner chapter = sheet.findViewById(R.id.sheetChapter);
        Spinner l = sheet.findViewById(R.id.sheetLanguage);
        Spinner t = sheet.findViewById(R.id.sheetType);
        Spinner d = sheet.findViewById(R.id.sheetDifficulty);

        configureSpinner(l, LANG_OPTIONS);
        configureSpinner(t, TYPE_OPTIONS);
        configureSpinner(d, DIFF_OPTIONS);
        l.setSelection(selectedLangPos);
        t.setSelection(selectedTypePos);
        d.setSelection(selectedDiffPos);

        FilterOption allSubject = new FilterOption(null, "All subjects");
        FilterOption allPaper = new FilterOption(null, "All papers");
        FilterOption allChapter = new FilterOption(null, "All chapters");
        setOptions(subject, List.of(allSubject));
        setOptions(paper, List.of(allPaper));
        setOptions(chapter, List.of(allChapter));

        filterExecutor.execute(() -> {
            try {
                CatalogModels.PageResponse<CatalogModels.SubjectItem> data = repository.subjects(0, 100);
                List<FilterOption> options = new ArrayList<>(); options.add(allSubject);
                for (CatalogModels.SubjectItem x : data.items()) options.add(new FilterOption(x.id(), x.name()));
                requireActivity().runOnUiThread(() -> setOptions(subject, options));
            } catch (Exception ignored) {}
        });

        subject.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            boolean first = true;
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                if (first && subjectId != null) {
                    first = false; return;
                }
                String sid = ((FilterOption) subject.getSelectedItem()).id;
                setOptions(paper, List.of(allPaper)); setOptions(chapter, List.of(allChapter));
                if (sid == null) return;
                filterExecutor.execute(() -> {
                    try {
                        var data = repository.papers(sid, 0, 100);
                        List<FilterOption> options = new ArrayList<>(); options.add(allPaper);
                        for (var x : data.items()) options.add(new FilterOption(x.id(), x.name()));
                        requireActivity().runOnUiThread(() -> setOptions(paper, options));
                    } catch (Exception ignored) {}
                });
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });

        paper.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                String pid = ((FilterOption) paper.getSelectedItem()).id;
                setOptions(chapter, List.of(allChapter));
                if (pid == null) return;
                filterExecutor.execute(() -> {
                    try {
                        var data = repository.chapters(pid, 0, 100);
                        List<FilterOption> options = new ArrayList<>(); options.add(allChapter);
                        for (var x : data.items()) options.add(new FilterOption(x.id(), x.title()));
                        requireActivity().runOnUiThread(() -> setOptions(chapter, options));
                    } catch (Exception ignored) {}
                });
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });

        sheet.findViewById(R.id.applySearchFilters).setOnClickListener(v -> {
            subjectId = ((FilterOption) subject.getSelectedItem()).id;
            paperId = ((FilterOption) paper.getSelectedItem()).id;
            chapterId = ((FilterOption) chapter.getSelectedItem()).id;
            selectedLangPos = l.getSelectedItemPosition();
            selectedTypePos = t.getSelectedItemPosition();
            selectedDiffPos = d.getSelectedItemPosition();
            page = 0; runSearch(0); dialog.dismiss();
        });
        dialog.show();
    }

    private void setOptions(Spinner spinner, List<FilterOption> options) {
        if (spinner == null) return;
        spinner.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item, options));
    }

    private static final class FilterOption {
        final String id; final String label;
        FilterOption(String id, String label) { this.id = id; this.label = label; }
        @Override public String toString() { return label; }
    }

    @Override public void onDestroyView() {
        if (pending != null) handler.removeCallbacks(pending);
        if (vm != null) vm.cancel();
        filterExecutor.shutdownNow();
        super.onDestroyView();
    }
}