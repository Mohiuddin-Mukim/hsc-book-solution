package bd.hscsolution.student.ui;

import android.view.*;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import bd.hscsolution.student.R;
import bd.hscsolution.student.content.CatalogModels;
import java.util.List;

public final class EditionAdapter extends RecyclerView.Adapter<EditionAdapter.VH> {
    private List<CatalogModels.EditionItem> items=List.of();
    public void submit(List<CatalogModels.EditionItem> v){items=v==null?List.of():v;notifyDataSetChanged();}
    @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p,int t){return new VH(LayoutInflater.from(p.getContext()).inflate(R.layout.item_catalog,p,false));}
    @Override public void onBindViewHolder(@NonNull VH h,int i){var x=items.get(i);h.title.setText(x.editionLabel());h.meta.setText(x.publicationYear()==null?"Edition":String.valueOf(x.publicationYear()));}
    @Override public int getItemCount(){return items.size();}
    static final class VH extends RecyclerView.ViewHolder{final TextView title,meta;VH(View v){super(v);title=v.findViewById(R.id.itemTitle);meta=v.findViewById(R.id.itemMeta);}}
}
