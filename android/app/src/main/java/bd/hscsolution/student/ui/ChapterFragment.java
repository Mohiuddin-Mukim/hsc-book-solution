package bd.hscsolution.student.ui;

import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import bd.hscsolution.student.offline.WorkScheduler;
import bd.hscsolution.student.offline.OfflineManifestEntity;
import bd.hscsolution.student.offline.OfflineManager;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import bd.hscsolution.student.HscApplication;
import bd.hscsolution.student.R;
import bd.hscsolution.student.content.*;

public final class ChapterFragment extends Fragment {
    private final java.util.concurrent.ExecutorService offlineLookup = Executors.newSingleThreadExecutor();
    private final java.util.Map<String, java.util.UUID> activeDownloads = new java.util.HashMap<>();
    public ChapterFragment(){super(R.layout.fragment_chapter);}
    @Override public void onViewCreated(@NonNull View view,@Nullable Bundle state){
        super.onViewCreated(view,state);
        String paperId=getArguments().getString("paperId"), paperName=getArguments().getString("paperName");
        ((TextView)view.findViewById(R.id.pageTitle)).setText(paperName==null?"Chapters":paperName);
        RecyclerView list=view.findViewById(R.id.chapterList);list.setLayoutManager(new LinearLayoutManager(requireContext()));
        ProgressBar progress=view.findViewById(R.id.catalogProgress);TextView error=view.findViewById(R.id.catalogError);
        TextView empty=view.findViewById(R.id.emptyState);Button retry=view.findViewById(R.id.retryButton);
        final ChapterAdapter[] adapterHolder = new ChapterAdapter[1];
        ChapterAdapter adapter=new ChapterAdapter(new ChapterAdapter.Listener() {
            @Override public void onClick(CatalogModels.ChapterItem item) {
                Bundle b=new Bundle();b.putString("chapterId",item.id());b.putString("chapterName",item.title());
                Navigation.findNavController(view).navigate(R.id.action_chapter_to_questions,b);
            }
            @Override public void onDownload(CatalogModels.ChapterItem item, Button button) {
                adapterHolder[0].markDownloading(item.id());
                var id=WorkScheduler.downloadChapter(requireContext(),item.id(),"bn");
                activeDownloads.put(item.id(), id);
                WorkManager.getInstance(requireContext()).getWorkInfoByIdLiveData(id)
                        .observe(getViewLifecycleOwner(), info -> {
                            if(info==null)return;
                            if(info.getState()==WorkInfo.State.SUCCEEDED) {
                                button.setEnabled(false); button.setText(R.string.offline_complete);
                                activeDownloads.remove(item.id());
                                adapterHolder[0].markDownloaded(item.id());
                            } else if(info.getState()==WorkInfo.State.FAILED) {
                                activeDownloads.remove(item.id());
                                adapterHolder[0].markFailed(item.id());
                            } else if(info.getState()==WorkInfo.State.CANCELLED) {
                                activeDownloads.remove(item.id());
                                adapterHolder[0].markCancelled(item.id());
                            } else if(info.getState()==WorkInfo.State.RUNNING) {
                                long total=info.getProgress().getLong("totalBytes",0);
                                long done=info.getProgress().getLong("downloadedBytes",0);
                                button.setText(total>0 ? "Cancel • "+(done*100/total)+"%" : "Cancel download");
                            }
                        });
            }
            @Override public void onCancel(CatalogModels.ChapterItem item) {
                java.util.UUID workId = activeDownloads.get(item.id());
                if (workId != null) WorkManager.getInstance(requireContext()).cancelWorkById(workId);
            }
        });
        adapterHolder[0] = adapter;
        list.setAdapter(adapter);
        var c=((HscApplication)requireActivity().getApplication()).container();
        CatalogViewModel vm=new ViewModelProvider(this,new CatalogViewModelFactory(c.contentRepository)).get(CatalogViewModel.class);
        vm.state().observe(getViewLifecycleOwner(),s->{if(s instanceof CatalogState.Loading){progress.setVisibility(View.VISIBLE);error.setVisibility(View.GONE);retry.setVisibility(View.GONE);empty.setVisibility(View.GONE);}
            else progress.setVisibility(View.GONE);
            if(s instanceof CatalogState.Error e){error.setText(e.message()==null||e.message().isBlank()?getString(R.string.catalog_load_error):e.message());error.setVisibility(View.VISIBLE);retry.setVisibility(View.VISIBLE);}
            if(s instanceof CatalogState.Success ok){
                var d=(CatalogModels.PageResponse<CatalogModels.ChapterItem>)ok.data();
                adapter.submit(d.items());
                empty.setVisibility(d.items().isEmpty()?View.VISIBLE:View.GONE);error.setVisibility(View.GONE);retry.setVisibility(View.GONE);
                offlineLookup.execute(() -> {
                    Set<String> downloaded = new HashSet<>();
                    for (CatalogModels.ChapterItem chapter : d.items()) {
                        OfflineManifestEntity manifest = c.database.offlineManifestDao()
                                .findCompleteScope("CHAPTER", chapter.id(), "bn");
                        if (manifest != null && OfflineManager.isManifestIntact(c.database, manifest.manifestId))
                            downloaded.add(chapter.id());
                    }
                    if (isAdded()) requireActivity().runOnUiThread(() -> adapter.setDownloadedIds(downloaded));
                });
            }});
        retry.setOnClickListener(v->vm.chapters(paperId));vm.chapters(paperId);
    }

    @Override public void onDestroy() {
        offlineLookup.shutdownNow();
        super.onDestroy();
    }
}
