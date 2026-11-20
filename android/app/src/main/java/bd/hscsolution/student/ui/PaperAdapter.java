package bd.hscsolution.student.ui;

import android.view.*;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import bd.hscsolution.student.R;
import bd.hscsolution.student.content.CatalogModels;
import java.util.List;

public final class PaperAdapter extends RecyclerView.Adapter<PaperAdapter.VH> {
    public interface Listener { void onClick(CatalogModels.PaperItem item); }
    private final Listener listener; private List<CatalogModels.PaperItem> items=List.of();
    public PaperAdapter(Listener l){listener=l;} public void submit(List<CatalogModels.PaperItem> v){items=v==null?List.of():v;notifyDataSetChanged();}
    @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p,int t){return new VH(LayoutInflater.from(p.getContext()).inflate(R.layout.item_catalog,p,false));}
    @Override public void onBindViewHolder(@NonNull VH h,int i){var x=items.get(i); h.title.setText(x.name()); h.meta.setText(x.paperNumber()==null?x.code():"Paper "+x.paperNumber()+" • "+x.code()); h.itemView.setOnClickListener(v->listener.onClick(x));}
    @Override public int getItemCount(){return items.size();}
    static final class VH extends RecyclerView.ViewHolder{final TextView title,meta;VH(View v){super(v);title=v.findViewById(R.id.itemTitle);meta=v.findViewById(R.id.itemMeta);}}
}
