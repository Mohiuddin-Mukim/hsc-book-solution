package bd.hscsolution.student.offline;

import android.content.Context;
import androidx.work.*;

import java.util.UUID;

public final class WorkScheduler {
    private WorkScheduler() {}

    public static void scheduleBaselineSync(Context context) {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED).build();
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(SyncWorker.class)
                .setConstraints(constraints).build();
        WorkManager.getInstance(context).enqueue(request);
    }

    public static UUID downloadChapter(Context context, String chapterId, String language) {
        return enqueue(context, "CHAPTER", chapterId, language);
    }

    public static UUID downloadBook(Context context, String bookId, String language) {
        return enqueue(context, "BOOK", bookId, language);
    }

    private static UUID enqueue(Context context, String type, String id, String language) {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED).build();
        Data data = new Data.Builder()
                .putString(ManifestDownloadWorker.KEY_SCOPE_TYPE, type)
                .putString(ManifestDownloadWorker.KEY_SCOPE_ID, id)
                .putString(ManifestDownloadWorker.KEY_LANGUAGE, language == null ? "bn" : language)
                .build();
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(ManifestDownloadWorker.class)
                .setInputData(data)
                .setConstraints(constraints)
                .addTag("offline-download")
                .build();
        WorkManager.getInstance(context).enqueueUniqueWork(
                "offline-" + type + "-" + id + "-" + (language == null ? "bn" : language),
                ExistingWorkPolicy.REPLACE, request);
        return request.getId();
    }
}
