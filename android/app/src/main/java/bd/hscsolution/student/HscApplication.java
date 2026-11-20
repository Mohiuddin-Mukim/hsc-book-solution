package bd.hscsolution.student;
import android.app.Application;
public final class HscApplication extends Application {
    private AppContainer container;
    @Override public void onCreate() { super.onCreate(); container = new AppContainer(this); }
    public AppContainer container() { return container; }
}
