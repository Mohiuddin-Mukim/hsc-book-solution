package bd.hscsolution.student.commerce;

import androidx.lifecycle.*;

import java.util.*;
import java.util.concurrent.*;

public final class CommerceViewModel extends ViewModel {
    private final CommerceRepository repo;
    private final MutableLiveData<List<CommerceModels.StudentPlan>> plans = new MutableLiveData<>();
    private final MutableLiveData<List<CommerceModels.Order>> transactions = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<CommerceModels.Checkout> checkout = new MutableLiveData<>();
    private final MutableLiveData<String> manualPortalUrl = new MutableLiveData<>();
    private final MutableLiveData<List<CommerceModels.Notification>> notifications = new MutableLiveData<>();

    public CommerceViewModel(CommerceRepository r) { repo = r; }

    public LiveData<List<CommerceModels.StudentPlan>> plans() { return plans; }
    public LiveData<List<CommerceModels.Order>> transactions() { return transactions; }
    public LiveData<String> error() { return error; }
    public LiveData<CommerceModels.Checkout> checkout() { return checkout; }
    public LiveData<String> manualPortalUrl() { return manualPortalUrl; }
    public LiveData<List<CommerceModels.Notification>> notifications() { return notifications; }

    public void load() {
        Executors.newSingleThreadExecutor().execute(() -> {
            try { plans.postValue(repo.studentPlans()); }
            catch (Exception e) { error.postValue(e.getMessage()); }
        });
    }

    public void loadTransactions() {
        Executors.newSingleThreadExecutor().execute(() -> {
            try { transactions.postValue(repo.transactions()); }
            catch (Exception e) { error.postValue(e.getMessage()); }
        });
    }

    public void loadNotifications() {
        Executors.newSingleThreadExecutor().execute(() -> {
            try { notifications.postValue(repo.unreadNotifications()); }
            catch (Exception ignored) { }
        });
    }

    public void markNotificationRead(String id) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try { repo.markNotificationRead(id); } catch (Exception ignored) { }
        });
    }

    public void buy(String id) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                CommerceModels.ManualPortalCreate c = repo.manualPaymentPortal(id);
                manualPortalUrl.postValue(c.portalUrl());
            } catch (Exception e) { error.postValue(e.getMessage()); }
        });
    }
}
