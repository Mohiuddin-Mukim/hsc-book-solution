package bd.hscsolution.student.content;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import bd.hscsolution.student.network.ApiException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class CatalogViewModel extends ViewModel {
    private final ContentRepository repository;
    private final MutableLiveData<CatalogState> state = new MutableLiveData<>();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public CatalogViewModel(ContentRepository repository) { this.repository = repository; }
    public LiveData<CatalogState> state() { return state; }

    public void subjects() { run(() -> repository.subjects(0, 50)); }
    public void papers(String subjectId) { run(() -> repository.papers(subjectId, 0, 50)); }
    public void books(String subjectId) { run(() -> repository.books(subjectId, 0, 50)); }
    public void editions(String bookId) { run(() -> repository.editions(bookId, 0, 50)); }
    public void chapters(String paperId) { run(() -> repository.chapters(paperId, 0, 100)); }
    public void questions(String chapterId, String type, String difficulty, String minMarks, String maxMarks, int page) {
        run(() -> repository.questions(chapterId, type, difficulty, minMarks, maxMarks, page, 20));
    }

    private void run(Task task) {
        state.postValue(new CatalogState.Loading());
        executor.execute(() -> {
            try {
                state.postValue(new CatalogState.Success(task.get()));
            } catch (Exception e) {
                String message = e instanceof ApiException a ? a.error().message()
                        : (e.getMessage() == null ? "Could not load content. Please try again." : e.getMessage());
                state.postValue(new CatalogState.Error(message));
            }
        });
    }

    private interface Task { Object get() throws Exception; }
    @Override protected void onCleared() { executor.shutdownNow(); }
}
