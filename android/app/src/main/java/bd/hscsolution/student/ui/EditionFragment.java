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

public final class EditionFragment extends Fragment {
    public EditionFragment(){super(R.layout.fragment_edition);}
    @Override public void onViewCreated(@NonNull View view,@Nullable Bundle state){
        super.onViewCreated(view,state);
        String bookId=getArguments().getString("bookId"), title=getArguments().getString("bookTitle");
        ((TextView)view.findViewById(R.id.pageTitle)).setText((title==null?"Book":title)+" • Editions");
        RecyclerView list=view.findViewById(R.id.editionList);list.setLayoutManager(new LinearLayoutManager(requireContext()));
        ProgressBar progress=view.findViewById(R.id.catalogProgress);TextView error=view.findViewById(R.id.catalogError);
        TextView empty=view.findViewById(R.id.emptyState);Button retry=view.findViewById(R.id.retryButton);
        EditionAdapter adapter=new EditionAdapter();list.setAdapter(adapter);
        var c=((HscApplication)requireActivity().getApplication()).container();
        CatalogViewModel vm=new ViewModelProvider(this,new CatalogViewModelFactory(c.contentRepository)).get(CatalogViewModel.class);
        vm.state().observe(getViewLifecycleOwner(),s->{if(s instanceof CatalogState.Loading){progress.setVisibility(View.VISIBLE);error.setVisibility(View.GONE);retry.setVisibility(View.GONE);}else progress.setVisibility(View.GONE);
            if(s instanceof CatalogState.Error e){error.setText(e.message());error.setVisibility(View.VISIBLE);retry.setVisibility(View.VISIBLE);}
            if(s instanceof CatalogState.Success ok){var d=(CatalogModels.PageResponse<CatalogModels.EditionItem>)ok.data();adapter.submit(d.items());empty.setVisibility(d.items().isEmpty()?View.VISIBLE:View.GONE);error.setVisibility(View.GONE);retry.setVisibility(View.GONE);}});
        retry.setOnClickListener(v->vm.editions(bookId));vm.editions(bookId);
    }
}
