package bd.hscsolution.student.content;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

public final class QuestionDetailViewModelFactory implements ViewModelProvider.Factory {
    private final ContentRepository repository;
    public QuestionDetailViewModelFactory(ContentRepository repository) { this.repository = repository; }

    @NonNull @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(QuestionDetailViewModel.class))
            return (T) new QuestionDetailViewModel(repository);
        throw new IllegalArgumentException("Unknown ViewModel: " + modelClass);
    }
}
