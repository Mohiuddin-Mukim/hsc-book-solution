package bd.hscsolution.student.ui;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

public final class ConnectivityMonitor {
    private final ConnectivityManager cm;
    private final MutableLiveData<Boolean> connected = new MutableLiveData<>(false);
    private final ConnectivityManager.NetworkCallback callback = new ConnectivityManager.NetworkCallback() {
        @Override public void onAvailable(Network network) { connected.postValue(true); }
        @Override public void onLost(Network network) { connected.postValue(false); }
    };
    public ConnectivityMonitor(Context context) {
        cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        connected.setValue(cm.getActiveNetwork() != null);
    }
    public LiveData<Boolean> connected() { return connected; }
    public void start() { cm.registerDefaultNetworkCallback(callback); }
    public void stop() { try { cm.unregisterNetworkCallback(callback); } catch (Exception ignored) {} }
}
