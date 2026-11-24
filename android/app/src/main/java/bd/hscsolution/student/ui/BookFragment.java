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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import bd.hscsolution.student.HscApplication;
import bd.hscsolution.student.R;
import bd.hscsolution.student.content.*;

public final class BookFragment extends Fragment {
    private final ExecutorService offlineLookup = Executors.newSingleThreadExecutor();
    private final java.util.Map<String, java.util.UUID> activeDownloads = new java.util.HashMap<>();
    public BookFragment(){super(R.layout.fragment_book);}
    @Override public void onViewCreated(@NonNull View view,@Nullable Bundle state){
        super.onViewCreated(view,state);
        String subjectId=getArguments().getString("subjectId"), subjectName=getArguments().getString("subjectName");
        ((TextView)view.findViewById(R.id.pageTitle)).setText((subjectName==null?"":subjectName)+" • Books");
        RecyclerView list=view.findViewById(R.id.bookList);list.setLayoutManager(new LinearLayoutManager(requireContext()));
        ProgressBar progress=view.findViewById(R.id.catalogProgress);TextView error=view.findViewById(R.id.catalogError);
        TextView empty=view.findViewById(R.id.emptyState);Button retry=view.findViewById(R.id.retryButton);
        final BookAdapter[] adapterHolder = new BookAdapter[1];
        BookAdapter adapter=new BookAdapter(new BookAdapter.Listener() {
            @Override public void onClick(CatalogModels.BookItem item) {
                Bundle b=new Bundle();b.putString("bookId",item.id());b.putString("bookTitle",item.title());
                Navigation.findNavController(view).navigate(R.id.action_book_to_editions,b);
            }
            @Override public void onDownload(CatalogModels.BookItem item, Button button) {
                adapterHolder[0].markDownloading(item.id());
                var id=WorkScheduler.downloadBook(requireContext(),item.id(),"bn");
                activeDownloads.put(item.id(), id);
                WorkManager.getInstance(requireContext()).getWorkInfoByIdLiveData(id)
                        .observe(getViewLifecycleOwner(), info -> {
                            if(info==null)return;
                            if(info.getState()==WorkInfo.State.SUCCEEDED){activeDownloads.remove(item.id());adapterHolder[0].markDownloaded(item.id());}
                            else if(info.getState()==WorkInfo.State.FAILED){activeDownloads.remove(item.id());adapterHolder[0].markFailed(item.id());}
                            else if(info.getState()==WorkInfo.State.CANCELLED){activeDownloads.remove(item.id());adapterHolder[0].markCancelled(item.id());}
                            else if(info.getState()==WorkInfo.State.RUNNING){
                                long total=info.getProgress().getLong("totalBytes",0),done=info.getProgress().getLong("downloadedBytes",0);
                                button.setText(total>0?"Cancel • "+(done*100/total)+"%":"Cancel download");
                            }
                        });
            }
            @Override public void onCancel(CatalogModels.BookItem item) {
                java.util.UUID workId=activeDownloads.get(item.id());
                if(workId!=null)WorkManager.getInstance(requireContext()).cancelWorkById(workId);
            }
        });
        adapterHolder[0] = adapter;
        list.setAdapter(adapter);
        var c=((HscApplication)requireActivity().getApplication()).container();
        CatalogViewModel vm=new ViewModelProvider(this,new CatalogViewModelFactory(c.contentRepository)).get(CatalogViewModel.class);
        vm.state().observe(getViewLifecycleOwner(),s->{if(s instanceof CatalogState.Loading){progress.setVisibility(View.VISIBLE);error.setVisibility(View.GONE);retry.setVisibility(View.GONE);}else progress.setVisibility(View.GONE);
            if(s instanceof CatalogState.Error e){error.setText(e.message());error.setVisibility(View.VISIBLE);retry.setVisibility(View.VISIBLE);}
            if(s instanceof CatalogState.Success ok){
                var d=(CatalogModels.PageResponse<CatalogModels.BookItem>)ok.data();
                adapter.submit(d.items());empty.setVisibility(d.items().isEmpty()?View.VISIBLE:View.GONE);error.setVisibility(View.GONE);retry.setVisibility(View.GONE);
                offlineLookup.execute(() -> {
                    Set<String> downloaded = new HashSet<>();
                    for (CatalogModels.BookItem book : d.items()) {
                        OfflineManifestEntity manifest = c.database.offlineManifestDao()
                                .findCompleteScope("BOOK", book.id(), "bn");
                        if (manifest != null && OfflineManager.isManifestIntact(c.database, manifest.manifestId))
                            downloaded.add(book.id());
                    }
                    if (isAdded()) requireActivity().runOnUiThread(() -> adapter.setDownloadedIds(downloaded));
                });
            }});
        retry.setOnClickListener(v->vm.books(subjectId));vm.books(subjectId);
    }

    @Override public void onDestroy() {
        offlineLookup.shutdownNow();
        super.onDestroy();
    }
}
