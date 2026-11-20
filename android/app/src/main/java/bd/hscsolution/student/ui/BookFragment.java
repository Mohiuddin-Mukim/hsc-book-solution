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

public final class BookFragment extends Fragment {
    public BookFragment(){super(R.layout.fragment_book);}
    @Override public void onViewCreated(@NonNull View view,@Nullable Bundle state){
        super.onViewCreated(view,state);
        String subjectId=getArguments().getString("subjectId"), subjectName=getArguments().getString("subjectName");
        ((TextView)view.findViewById(R.id.pageTitle)).setText((subjectName==null?"":subjectName)+" • Books");
        RecyclerView list=view.findViewById(R.id.bookList);list.setLayoutManager(new LinearLayoutManager(requireContext()));
        ProgressBar progress=view.findViewById(R.id.catalogProgress);TextView error=view.findViewById(R.id.catalogError);
        TextView empty=view.findViewById(R.id.emptyState);Button retry=view.findViewById(R.id.retryButton);
        BookAdapter adapter=new BookAdapter(item->{Bundle b=new Bundle();b.putString("bookId",item.id());b.putString("bookTitle",item.title());Navigation.findNavController(view).navigate(R.id.action_book_to_editions,b);});
        list.setAdapter(adapter);
        var c=((HscApplication)requireActivity().getApplication()).container();
        CatalogViewModel vm=new ViewModelProvider(this,new CatalogViewModelFactory(c.contentRepository)).get(CatalogViewModel.class);
        vm.state().observe(getViewLifecycleOwner(),s->{if(s instanceof CatalogState.Loading){progress.setVisibility(View.VISIBLE);error.setVisibility(View.GONE);retry.setVisibility(View.GONE);}else progress.setVisibility(View.GONE);
            if(s instanceof CatalogState.Error e){error.setText(e.message());error.setVisibility(View.VISIBLE);retry.setVisibility(View.VISIBLE);}
            if(s instanceof CatalogState.Success ok){var d=(CatalogModels.PageResponse<CatalogModels.BookItem>)ok.data();adapter.submit(d.items());empty.setVisibility(d.items().isEmpty()?View.VISIBLE:View.GONE);error.setVisibility(View.GONE);retry.setVisibility(View.GONE);}});
        retry.setOnClickListener(v->vm.books(subjectId));vm.books(subjectId);
    }
}
