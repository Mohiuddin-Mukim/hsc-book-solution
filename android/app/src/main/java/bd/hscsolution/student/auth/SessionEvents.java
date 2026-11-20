package bd.hscsolution.student.auth;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
public final class SessionEvents {
    private static final MutableLiveData<Boolean> expired = new MutableLiveData<>(false);
    private SessionEvents() {}
    public static LiveData<Boolean> expired() { return expired; }
    public static void notifyExpired() { expired.postValue(true); }
    public static void clear() { expired.postValue(false); }
}
