package bd.hscsolution.student.ui;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import bd.hscsolution.student.HscApplication;
import bd.hscsolution.student.R;
import bd.hscsolution.student.offline.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class OfflineDownloadsFragment extends Fragment {
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private LinearLayout container;
    private TextView summary, empty;
    private ProgressBar progress;

    public OfflineDownloadsFragment() { super(R.layout.fragment_offline_downloads); }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        super.onViewCreated(view, state);
        container = view.findViewById(R.id.offlinePackageContainer);
        summary = view.findViewById(R.id.offlineStorageSummary);
        empty = view.findViewById(R.id.offlineEmpty);
        progress = view.findViewById(R.id.offlineProgress);
        loadPackages();
    }

    private void loadPackages() {
        progress.setVisibility(View.VISIBLE);
        var db = ((HscApplication) requireActivity().getApplication()).container().database;
        android.content.Context appContext = requireContext().getApplicationContext();
        android.app.Activity activity = requireActivity();
        io.execute(() -> {
            List<OfflineManifestEntity> packages = OfflineManager.uniqueCompleteManifests(db);
            java.util.Map<String, Boolean> integrity = new java.util.HashMap<>();
            for (OfflineManifestEntity item : packages) {
                integrity.put(item.manifestId, OfflineManager.isManifestIntact(db, item.manifestId));
            }
            long used = OfflineManager.storageBytes(appContext);
            if (!isAdded() || activity.isFinishing()) return;
            activity.runOnUiThread(() -> {
                if (!isAdded()) return;
                progress.setVisibility(View.GONE);
                summary.setText("Storage used: " + formatBytes(used) + " • " + packages.size() + " package(s)");
                container.removeAllViews();
                empty.setVisibility(packages.isEmpty() ? View.VISIBLE : View.GONE);
                for (OfflineManifestEntity item : packages)
                    addPackageRow(item, Boolean.TRUE.equals(integrity.get(item.manifestId)));
            });
        });
    }

    private void addPackageRow(OfflineManifestEntity item, boolean intact) {
        LinearLayout card = new LinearLayout(requireContext());
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(16, 14, 16, 14);
        card.setBackgroundResource(R.drawable.bg_reader_option);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(-1, -2);
        cardParams.bottomMargin = 12;
        container.addView(card, cardParams);

        TextView title = new TextView(requireContext());
        title.setText(item.scopeTitle == null || item.scopeTitle.isBlank()
                ? ("BOOK".equals(item.scopeType) ? "Downloaded book" : "Downloaded chapter")
                : item.scopeTitle);
        title.setTextSize(17);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setTextColor(getResources().getColor(R.color.text_primary));
        card.addView(title);

        TextView meta = new TextView(requireContext());
        meta.setText((item.scopeType == null ? "CONTENT" : item.scopeType)
                + " • " + (item.languageCode == null ? "bn" : item.languageCode)
                + " • " + formatBytes(item.totalBytes)
                + " • v" + item.versionNo
                + (intact ? "" : " • needs redownload")
                + (item.accessUntil == null ? "" : " • access until " + item.accessUntil));
        meta.setTextSize(12);
        meta.setTextColor(getResources().getColor(R.color.text_secondary));
        meta.setPadding(0, 4, 0, 8);
        card.addView(meta);

        LinearLayout actions = new LinearLayout(requireContext());
        actions.setOrientation(LinearLayout.HORIZONTAL);
        Button open = new Button(requireContext());
        open.setText(intact ? "Open offline" : "Unavailable");
        open.setEnabled(intact);
        actions.addView(open, new LinearLayout.LayoutParams(0, -2, 1));
        Button delete = new Button(requireContext());
        delete.setText("Delete");
        actions.addView(delete, new LinearLayout.LayoutParams(0, -2, 1));
        card.addView(actions);

        open.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putString("offlineManifestId", item.manifestId);
            args.putString("chapterName", title.getText().toString());
            Navigation.findNavController(v).navigate(R.id.action_offline_downloads_to_questions, args);
        });
        delete.setOnClickListener(v -> new AlertDialog.Builder(requireContext())
                .setTitle("Delete offline package?")
                .setMessage("This removes the local content for " + title.getText() + ".")
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton("Delete", (dialog, which) -> {
                    android.content.Context appContext = requireContext().getApplicationContext();
                    android.app.Activity activity = requireActivity();
                    io.execute(() -> {
                        OfflineManager.deleteManifest(appContext, item.manifestId);
                        if (isAdded() && !activity.isFinishing())
                            activity.runOnUiThread(this::loadPackages);
                    });
                }).show());
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format(Locale.US, "%.1f KB", bytes / 1024.0);
        if (bytes < 1024L * 1024L * 1024L) return String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0));
        return String.format(Locale.US, "%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }

    @Override public void onDestroy() {
        super.onDestroy();
        io.shutdownNow();
    }
}
