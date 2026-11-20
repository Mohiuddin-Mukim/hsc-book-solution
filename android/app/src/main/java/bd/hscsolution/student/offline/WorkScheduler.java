package bd.hscsolution.student.offline;
import android.content.Context;
import androidx.work.Constraints;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

public final class WorkScheduler {
    private WorkScheduler() {}
    public static void scheduleBaselineSync(Context context) {
        Constraints constraints = new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build();
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(SyncWorker.class)
                .setConstraints(constraints).build();
        WorkManager.getInstance(context).enqueue(request);
    }
}
