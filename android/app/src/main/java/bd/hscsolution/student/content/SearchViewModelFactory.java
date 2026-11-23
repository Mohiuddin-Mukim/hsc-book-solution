package bd.hscsolution.student.content;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

public final class SearchViewModelFactory implements ViewModelProvider.Factory {
    private final ContentRepository repository;
    public SearchViewModelFactory(ContentRepository repository) { this.repository = repository; }

    @NonNull @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(SearchViewModel.class))
            return (T) new SearchViewModel(repository);
        throw new IllegalArgumentException("Unknown ViewModel: " + modelClass);
    }
}
