package bd.hscsolution.student.ui;

import android.view.*;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import bd.hscsolution.student.R;
import bd.hscsolution.student.content.CatalogModels;
import java.util.*;

public final class SearchAdapter extends RecyclerView.Adapter<SearchAdapter.Holder> {
    public interface Listener { void onClick(CatalogModels.SearchResult item); }
    private final Listener listener;
    private final List<CatalogModels.SearchResult> items = new ArrayList<>();

    public SearchAdapter(Listener listener) { this.listener = listener; }
    public void submit(List<CatalogModels.SearchResult> next) {
        items.clear();
        if (next != null) items.addAll(next);
        notifyDataSetChanged();
    }

    @NonNull @Override public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_search_result, parent, false));
    }
    @Override public void onBindViewHolder(@NonNull Holder h, int position) {
        CatalogModels.SearchResult item = items.get(position);
        h.title.setText(item.externalId() == null || item.externalId().isBlank()
                ? item.chapterCode() : item.externalId());
        h.subject.setText(item.subjectName() + "  •  " + item.paperName());
        String meta = item.chapterTitle() + "  •  " + (item.questionType() == null ? "" : item.questionType())
                + (item.difficulty() == null ? "" : "  •  " + item.difficulty());
        h.meta.setText(meta);
        h.itemView.setOnClickListener(v -> listener.onClick(item));
    }
    @Override public int getItemCount() { return items.size(); }

    static final class Holder extends RecyclerView.ViewHolder {
        final TextView title, subject, meta;
        Holder(View v) {
            super(v);
            title=v.findViewById(R.id.searchResultTitle);
            subject=v.findViewById(R.id.searchResultSubject);
            meta=v.findViewById(R.id.searchResultMeta);
        }
    }
}
