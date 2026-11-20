package bd.hscsolution.student.offline;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public final class SyncWorker extends Worker {
    public SyncWorker(@NonNull Context context, @NonNull WorkerParameters params) { super(context, params); }
    @NonNull @Override public Result doWork() {
        // Phase 8 establishes the durable job boundary. Actual manifest download/sync is Phase 11.
        return Result.success();
    }
}
