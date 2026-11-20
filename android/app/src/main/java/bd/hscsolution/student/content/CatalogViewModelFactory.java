package bd.hscsolution.student.content;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

public final class CatalogViewModelFactory implements ViewModelProvider.Factory {
    private final ContentRepository repository;
    public CatalogViewModelFactory(ContentRepository repository) { this.repository = repository; }

    @NonNull @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        return modelClass.cast(new CatalogViewModel(repository));
    }
}
