package bd.hscsolution.student.commerce;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

public final class CommerceViewModelFactory implements ViewModelProvider.Factory {
    private final CommerceRepository r;

    public CommerceViewModelFactory(CommerceRepository r) {
        this.r = r;
    }

    @NonNull
    public <T extends ViewModel> T create(@NonNull Class<T> c) {
        return (T) new CommerceViewModel(r);
    }
}