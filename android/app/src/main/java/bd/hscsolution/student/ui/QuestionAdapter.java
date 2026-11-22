package bd.hscsolution.student.ui;

import android.view.*;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import bd.hscsolution.student.R;
import bd.hscsolution.student.content.CatalogModels;
import java.util.List;

public final class QuestionAdapter extends RecyclerView.Adapter<QuestionAdapter.VH> {
    public interface OnQuestionClick { void onClick(CatalogModels.PublishedQuestionItem item); }
    private final OnQuestionClick listener;
    public QuestionAdapter(OnQuestionClick listener){ this.listener=listener; }
    private List<CatalogModels.PublishedQuestionItem> items=List.of();
    public void submit(List<CatalogModels.PublishedQuestionItem> v){items=v==null?List.of():v;notifyDataSetChanged();}
    @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p,int t){return new VH(LayoutInflater.from(p.getContext()).inflate(R.layout.item_question,p,false));}
    @Override public void onBindViewHolder(@NonNull VH h,int i){
        var x=items.get(i);
        String type=x.questionType()==null?"Question":x.questionType();
        String difficulty=x.difficulty()==null?"":x.difficulty();
        h.number.setText("Question "+(i+1));
        h.meta.setText(type + (difficulty.isBlank()?"":" • "+difficulty) + (x.marks()==null?"":" • "+x.marks()+" marks"));
        h.id.setText(x.id()); h.itemView.setOnClickListener(v -> listener.onClick(x));
    }
    @Override public int getItemCount(){return items.size();}
    static final class VH extends RecyclerView.ViewHolder{final TextView number,meta,id;VH(View v){super(v);number=v.findViewById(R.id.questionNumber);meta=v.findViewById(R.id.questionMeta);id=v.findViewById(R.id.questionId);}}
}
