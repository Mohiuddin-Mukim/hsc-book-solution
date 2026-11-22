package bd.hscsolution.student.content;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import bd.hscsolution.student.network.ApiException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class QuestionDetailViewModel extends ViewModel {
    public sealed interface State permits Loading, Success, Error {}
    public record Loading() implements State {}
    public record Success(CatalogModels.QuestionDetail detail, BookmarkModels.BookmarkResponse bookmark) implements State {}
    public record Error(String message) implements State {}

    private final ContentRepository repository;
    private final MutableLiveData<State> state = new MutableLiveData<>();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public QuestionDetailViewModel(ContentRepository repository) { this.repository = repository; }
    public LiveData<State> state() { return state; }

    public void load(String questionId, String language) {
        state.postValue(new Loading());
        executor.execute(() -> {
            try {
                var detail = repository.question(questionId, language);
                BookmarkModels.BookmarkResponse bm = null;
                try { bm = repository.bookmark(questionId); }
                catch (Exception ignored) {
                    // Reader remains usable if bookmark status cannot be loaded.
                }
                state.postValue(new Success(detail, bm));
            } catch (Exception e) {
                String msg = e instanceof ApiException a ? a.error().message()
                        : (e.getMessage() == null ? "Could not load the solution. Please try again." : e.getMessage());
                state.postValue(new Error(msg));
            }
        });
    }

    public void toggleBookmark(String questionId, boolean currentlyBookmarked) {
        executor.execute(() -> {
            try {
                BookmarkModels.BookmarkResponse result = currentlyBookmarked
                        ? repository.removeBookmark(questionId)
                        : repository.addBookmark(questionId);
                var current = state.getValue();
                if (current instanceof Success s)
                    state.postValue(new Success(s.detail(), result));
            } catch (Exception e) {
                // Keep current reader state; UI can show a transient failure.
            }
        });
    }

    @Override protected void onCleared() { executor.shutdownNow(); }
}
