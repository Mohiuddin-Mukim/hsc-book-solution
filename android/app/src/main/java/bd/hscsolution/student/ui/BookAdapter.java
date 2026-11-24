package bd.hscsolution.student.ui;

import android.view.*;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import bd.hscsolution.student.R;
import bd.hscsolution.student.content.CatalogModels;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

public final class BookAdapter extends RecyclerView.Adapter<BookAdapter.VH> {
    public interface Listener {
        void onClick(CatalogModels.BookItem item);
        void onDownload(CatalogModels.BookItem item, Button button);
        void onCancel(CatalogModels.BookItem item);
    }
    private final Listener listener; private List<CatalogModels.BookItem> items=List.of();
    private final Set<String> downloadedIds = new HashSet<>();
    private final Set<String> downloadingIds = new HashSet<>();
    private final Set<String> failedIds = new HashSet<>();
    public BookAdapter(Listener l){listener=l;}
    public void submit(List<CatalogModels.BookItem> v){items=v==null?List.of():v;notifyDataSetChanged();}
    public void setDownloadedIds(Set<String> ids){downloadedIds.clear();if(ids!=null)downloadedIds.addAll(ids);notifyDataSetChanged();}
    public void markDownloaded(String id){if(id!=null){downloadedIds.add(id);downloadingIds.remove(id);failedIds.remove(id);}notifyDataSetChanged();}
    public void markDownloading(String id){if(id!=null){downloadingIds.add(id);failedIds.remove(id);}notifyDataSetChanged();}
    public void markFailed(String id){if(id!=null){downloadingIds.remove(id);failedIds.add(id);}notifyDataSetChanged();}
    public void markCancelled(String id){if(id!=null)downloadingIds.remove(id);notifyDataSetChanged();}
    @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p,int t){
        return new VH(LayoutInflater.from(p.getContext()).inflate(R.layout.item_book,p,false));
    }
    @Override public void onBindViewHolder(@NonNull VH h,int i){
        var x=items.get(i);h.title.setText(x.title());h.meta.setText(x.publisher()==null?"":x.publisher());
        h.itemView.setOnClickListener(v->listener.onClick(x));
        boolean downloaded=downloadedIds.contains(x.id());
        boolean downloading=downloadingIds.contains(x.id());
        h.download.setEnabled(!downloaded);
        h.download.setText(downloaded?"Downloaded":downloading?"Cancel download"
                :failedIds.contains(x.id())?"Retry download":h.download.getContext().getString(R.string.download_offline));
        h.download.setOnClickListener(v->{if(downloaded)return;if(downloading)listener.onCancel(x);else listener.onDownload(x,h.download);});
    }
    @Override public int getItemCount(){return items.size();}
    static final class VH extends RecyclerView.ViewHolder{
        final TextView title,meta; final Button download;
        VH(View v){super(v);title=v.findViewById(R.id.itemTitle);meta=v.findViewById(R.id.itemMeta);download=v.findViewById(R.id.downloadButton);}
    }
}
