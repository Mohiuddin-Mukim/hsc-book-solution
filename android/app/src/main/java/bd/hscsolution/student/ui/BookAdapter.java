package bd.hscsolution.student.ui;

import android.view.*;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import bd.hscsolution.student.R;
import bd.hscsolution.student.content.CatalogModels;
import java.util.List;

public final class BookAdapter extends RecyclerView.Adapter<BookAdapter.VH> {
    public interface Listener { void onClick(CatalogModels.BookItem item); }
    private final Listener listener; private List<CatalogModels.BookItem> items=List.of();
    public BookAdapter(Listener l){listener=l;} public void submit(List<CatalogModels.BookItem> v){items=v==null?List.of():v;notifyDataSetChanged();}
    @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p,int t){return new VH(LayoutInflater.from(p.getContext()).inflate(R.layout.item_catalog,p,false));}
    @Override public void onBindViewHolder(@NonNull VH h,int i){var x=items.get(i);h.title.setText(x.title());h.meta.setText(x.publisher()==null?"":x.publisher());h.itemView.setOnClickListener(v->listener.onClick(x));}
    @Override public int getItemCount(){return items.size();}
    static final class VH extends RecyclerView.ViewHolder{final TextView title,meta;VH(View v){super(v);title=v.findViewById(R.id.itemTitle);meta=v.findViewById(R.id.itemMeta);}}
}
