package bd.hscsolution.student.ui;

import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import bd.hscsolution.student.HscApplication;
import bd.hscsolution.student.R;
import bd.hscsolution.student.content.*;

public final class QuestionListFragment extends Fragment {
    private CatalogViewModel vm; private String chapterId; private int page=0;
    private Spinner typeSpinner,difficultySpinner; private EditText minMarks,maxMarks;
    public QuestionListFragment(){super(R.layout.fragment_question_list);}
    @Override public void onViewCreated(@NonNull View view,@Nullable Bundle state){
        super.onViewCreated(view,state);
        chapterId=getArguments().getString("chapterId");
        String name=getArguments().getString("chapterName");
        ((TextView)view.findViewById(R.id.pageTitle)).setText(name==null?"Questions":name);
        RecyclerView list=view.findViewById(R.id.questionList);list.setLayoutManager(new LinearLayoutManager(requireContext()));
        ProgressBar progress=view.findViewById(R.id.catalogProgress);TextView error=view.findViewById(R.id.catalogError);
        TextView empty=view.findViewById(R.id.emptyState);Button retry=view.findViewById(R.id.retryButton);
        TextView pageLabel=view.findViewById(R.id.pageLabel);Button prev=view.findViewById(R.id.prevButton),next=view.findViewById(R.id.nextButton);
        QuestionAdapter adapter=new QuestionAdapter();list.setAdapter(adapter);
        typeSpinner=view.findViewById(R.id.typeSpinner);difficultySpinner=view.findViewById(R.id.difficultySpinner);
        minMarks=view.findViewById(R.id.minMarksInput);maxMarks=view.findViewById(R.id.maxMarksInput);
        String[] types={"All types","MCQ","CQ","SHORT","NUMERICAL"};
        String[] diffs={"All difficulties","EASY","MEDIUM","HARD"};
        typeSpinner.setAdapter(new ArrayAdapter<>(requireContext(),android.R.layout.simple_spinner_dropdown_item,types));
        difficultySpinner.setAdapter(new ArrayAdapter<>(requireContext(),android.R.layout.simple_spinner_dropdown_item,diffs));

        var c=((HscApplication)requireActivity().getApplication()).container();
        vm=new ViewModelProvider(this,new CatalogViewModelFactory(c.contentRepository)).get(CatalogViewModel.class);
        vm.state().observe(getViewLifecycleOwner(),s->{
            if(s instanceof CatalogState.Loading){progress.setVisibility(View.VISIBLE);error.setVisibility(View.GONE);retry.setVisibility(View.GONE);empty.setVisibility(View.GONE);}
            else progress.setVisibility(View.GONE);
            if(s instanceof CatalogState.Error e){error.setText(e.message()==null||e.message().isBlank()?getString(R.string.catalog_load_error):e.message());error.setVisibility(View.VISIBLE);retry.setVisibility(View.VISIBLE);empty.setVisibility(View.GONE);}
            if(s instanceof CatalogState.Success ok){
                var d=(CatalogModels.PageResponse<CatalogModels.PublishedQuestionItem>)ok.data();
                adapter.submit(d.items()); empty.setVisibility(d.items().isEmpty()?View.VISIBLE:View.GONE);error.setVisibility(View.GONE);retry.setVisibility(View.GONE);
                pageLabel.setText("Page "+(d.page()+1)+" of "+Math.max(1,d.totalPages()));
                prev.setEnabled(d.page()>0);next.setEnabled(d.page()+1<d.totalPages());
            }
        });
        View.OnClickListener reload=v->{page=0;load();};
        view.findViewById(R.id.applyFilterButton).setOnClickListener(reload);
        retry.setOnClickListener(v->{page=0;load();});
        prev.setOnClickListener(v->{if(page>0){page--;load();}});
        next.setOnClickListener(v->{page++;load();});
        load();
    }
    private void load(){
        String type=typeSpinner.getSelectedItemPosition()==0?null:typeSpinner.getSelectedItem().toString();
        String diff=difficultySpinner.getSelectedItemPosition()==0?null:difficultySpinner.getSelectedItem().toString();
        String min=minMarks.getText()==null?null:minMarks.getText().toString().trim();
        String max=maxMarks.getText()==null?null:maxMarks.getText().toString().trim();
        if(min!=null&&min.isBlank())min=null;if(max!=null&&max.isBlank())max=null;
        vm.questions(chapterId,type,diff,min,max,page);
    }
}
