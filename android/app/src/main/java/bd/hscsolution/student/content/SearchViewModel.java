package bd.hscsolution.student.content;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import bd.hscsolution.student.network.ApiException;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public final class SearchViewModel extends ViewModel {
    private final ContentRepository repository;
    private final MutableLiveData<SearchState> state = new MutableLiveData<>(new SearchState.Idle());
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final AtomicInteger generation = new AtomicInteger();
    private Future<?> activeRequest;

    public SearchViewModel(ContentRepository repository) { this.repository = repository; }

    public LiveData<SearchState> state() { return state; }

    public synchronized void search(String query, String subjectId, String paperId, String chapterId,
                                     String language, String type, String difficulty, int page) {
        int token = generation.incrementAndGet();
        if (activeRequest != null) activeRequest.cancel(true);
        if (query == null || query.trim().isEmpty()) {
            state.postValue(new SearchState.Idle());
            return;
        }
        state.postValue(new SearchState.Loading());
        activeRequest = executor.submit(() -> {
            try {
                CatalogModels.SearchPageResponse result = repository.search(
                        query.trim(), subjectId, paperId, chapterId, language, type, difficulty, page, 20);
                if (generation.get() == token) state.postValue(new SearchState.Success(result));
            } catch (Exception e) {
                if (generation.get() != token || Thread.currentThread().isInterrupted()) return;
                String message = e instanceof ApiException a ? a.error().message()
                        : (e.getMessage() == null ? "Could not search. Please try again." : e.getMessage());
                state.postValue(new SearchState.Error(message));
            }
        });
    }

    public synchronized void cancel() {
        generation.incrementAndGet();
        if (activeRequest != null) activeRequest.cancel(true);
    }

    @Override protected void onCleared() {
        generation.incrementAndGet();
        if (activeRequest != null) activeRequest.cancel(true);
        executor.shutdownNow();
    }
}
