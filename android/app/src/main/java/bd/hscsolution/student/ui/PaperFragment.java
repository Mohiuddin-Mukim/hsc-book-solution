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
import bd.hscsolution.student.HscApplication;
import bd.hscsolution.student.R;
import bd.hscsolution.student.content.*;

public final class PaperFragment extends Fragment {
    public PaperFragment(){super(R.layout.fragment_paper);}
    @Override public void onViewCreated(@NonNull View view,@Nullable Bundle state){
        super.onViewCreated(view,state);
        String subjectId=getArguments().getString("subjectId"), subjectName=getArguments().getString("subjectName");
        view.findViewById(R.id.booksButton).setOnClickListener(v -> {
            Bundle b = new Bundle(); b.putString("subjectId", subjectId); b.putString("subjectName", subjectName);
            Navigation.findNavController(v).navigate(R.id.action_paper_to_books, b);
        });
        ((TextView)view.findViewById(R.id.pageTitle)).setText(subjectName==null?"Papers":subjectName);
        RecyclerView list=view.findViewById(R.id.paperList); list.setLayoutManager(new LinearLayoutManager(requireContext()));
        ProgressBar progress=view.findViewById(R.id.catalogProgress); TextView error=view.findViewById(R.id.catalogError);
        TextView empty=view.findViewById(R.id.emptyState); Button retry=view.findViewById(R.id.retryButton);
        PaperAdapter adapter=new PaperAdapter(item->{Bundle b=new Bundle();b.putString("paperId",item.id());b.putString("paperName",item.name());Navigation.findNavController(view).navigate(R.id.action_paper_to_chapters,b);});
        list.setAdapter(adapter);
        var c=((HscApplication)requireActivity().getApplication()).container();
        CatalogViewModel vm=new ViewModelProvider(this,new CatalogViewModelFactory(c.contentRepository)).get(CatalogViewModel.class);
        vm.state().observe(getViewLifecycleOwner(),s->{if(s instanceof CatalogState.Loading){progress.setVisibility(View.VISIBLE);error.setVisibility(View.GONE);retry.setVisibility(View.GONE);empty.setVisibility(View.GONE);}
            else progress.setVisibility(View.GONE);
            if(s instanceof CatalogState.Error e){error.setText(e.message()==null||e.message().isBlank()?getString(R.string.catalog_load_error):e.message());error.setVisibility(View.VISIBLE);retry.setVisibility(View.VISIBLE);}
            if(s instanceof CatalogState.Success ok){var d=(CatalogModels.PageResponse<CatalogModels.PaperItem>)ok.data();adapter.submit(d.items());empty.setVisibility(d.items().isEmpty()?View.VISIBLE:View.GONE);error.setVisibility(View.GONE);retry.setVisibility(View.GONE);}});
        retry.setOnClickListener(v->vm.papers(subjectId)); vm.papers(subjectId);
    }
}
