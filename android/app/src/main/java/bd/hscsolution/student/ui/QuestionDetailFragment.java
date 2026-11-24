package bd.hscsolution.student.ui;

import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import com.bumptech.glide.Glide;
import bd.hscsolution.student.HscApplication;
import bd.hscsolution.student.R;
import bd.hscsolution.student.content.*;

public final class QuestionDetailFragment extends Fragment {
    private QuestionDetailViewModel vm;
    private String questionId;
    private LinearLayout content;
    private ScrollView scroll;
    private Button bookmark;
    private TextView error;

    public QuestionDetailFragment(){ super(R.layout.fragment_question_detail); }

    @Override public void onViewCreated(@NonNull View view,@Nullable Bundle state) {
        super.onViewCreated(view,state);
        questionId = getArguments().getString("questionId");
        String language = getArguments().getString("language","bn");
        content = view.findViewById(R.id.readerContent);
        scroll = view.findViewById(R.id.readerScroll);
        bookmark = view.findViewById(R.id.bookmarkButton);
        error = view.findViewById(R.id.readerError);
        ProgressBar progress=view.findViewById(R.id.readerProgress);
        Button retry=view.findViewById(R.id.readerRetry);
        TextView title=view.findViewById(R.id.readerTitle);

        var c=((HscApplication)requireActivity().getApplication()).container();
        vm=new ViewModelProvider(this,new QuestionDetailViewModelFactory(c.contentRepository))
                .get(QuestionDetailViewModel.class);

        vm.state().observe(getViewLifecycleOwner(), s -> {
            if (s instanceof QuestionDetailViewModel.Loading) {
                progress.setVisibility(View.VISIBLE); error.setVisibility(View.GONE);
            } else {
                progress.setVisibility(View.GONE);
            }
            if (s instanceof QuestionDetailViewModel.Error e) {
                error.setText(e.message()); error.setVisibility(View.VISIBLE);
                retry.setVisibility(View.VISIBLE);
            }
            if (s instanceof QuestionDetailViewModel.Success ok) {
                error.setVisibility(View.GONE); retry.setVisibility(View.GONE);
                title.setText(questionTitle(ok.detail()));
                render(ok.detail());
                boolean marked=ok.bookmark()!=null && ok.bookmark().bookmarked();
                bookmark.setText(marked ? "★ Bookmarked" : "☆ Bookmark");
                bookmark.setOnClickListener(v->vm.toggleBookmark(questionId,marked));
                restorePosition();
            }
        });
        retry.setOnClickListener(v->vm.load(questionId,language));
        vm.load(questionId,language);
    }

    private String questionTitle(CatalogModels.QuestionDetail d) {
        return d.version()!=null ? ("Question • "+(d.version().marks()==null?"":d.version().marks()+" marks")) : "Question";
    }

    private void render(CatalogModels.QuestionDetail d) {
        content.removeAllViews();
        if (d.version()!=null) {
            addHeading("Question");
            addBody(d.version().stem());
            addMedia(d.version().media());
            if (!d.version().options().isEmpty()) addMcq(d.version().options());
            if (!d.version().explanations().isEmpty()) {
                addHeading("Explanation");
                d.version().explanations().forEach(e->addBody(e.explanation()));
            }
        }
        if (d.solution()!=null && d.solution().version()!=null) {
            addHeading("Solution");
            if (d.solution().version().summary()!=null && !d.solution().version().summary().isBlank())
                addBody(d.solution().version().summary());
            if (d.solution().version().finalAnswer()!=null && !d.solution().version().finalAnswer().isBlank()) {
                addHeading("Final answer");
                addBody(d.solution().version().finalAnswer());
            }
            for (CatalogModels.SolutionStep step : d.solution().version().steps()) {
                addStep(step);
            }
        }
        if (d.solution()==null || d.solution().version()==null) {
            addHeading("Solution");
            addBody("A published solution is not available yet.");
        }
    }

    private void addHeading(String text) {
        TextView t=new TextView(requireContext()); t.setText(text); t.setTextSize(20); t.setTextColor(getResources().getColor(R.color.primary));
        t.setTypeface(null,android.graphics.Typeface.BOLD); t.setPadding(0,24,0,10); content.addView(t);
    }

    private void addBody(String text) {
        if (ReaderHtml.containsMath(text)) {
            SafeMathWebView w=new SafeMathWebView(requireContext());
            w.setLayoutParams(new LinearLayout.LayoutParams(-1, 180));
            w.render(text); content.addView(w);
        } else {
            TextView t=new TextView(requireContext()); t.setText(text==null?"":text); t.setTextSize(17); t.setTextColor(getResources().getColor(R.color.text_primary));
            t.setLineSpacing(0,1.25f); t.setPadding(0,4,0,8); content.addView(t);
        }
    }

    private void addStep(CatalogModels.SolutionStep step) {
        TextView title=new TextView(requireContext());
        title.setText((step.stepNo())+". "+(step.title()==null?"":step.title()));
        title.setTextSize(18); title.setTypeface(null,android.graphics.Typeface.BOLD);
        title.setTextColor(getResources().getColor(R.color.text_primary)); title.setPadding(0,18,0,6);
        content.addView(title);
        addBody(step.body());
        addMedia(step.media());
    }

    private void addMcq(java.util.List<CatalogModels.McqOption> options) {
        addHeading("Options");
        for (CatalogModels.McqOption o:options) {
            TextView t=new TextView(requireContext());
            t.setText(o.optionKey()+". "+o.optionText()+(o.correct()?"  ✓":""));
            t.setTextSize(16); t.setPadding(16,12,16,12);
            t.setBackgroundResource(R.drawable.bg_reader_option);
            content.addView(t,new LinearLayout.LayoutParams(-1,-2));
        }
    }

    private void addMedia(java.util.List<CatalogModels.MediaAsset> media) {
        if (media==null || media.isEmpty()) return;
        for (CatalogModels.MediaAsset m:media) {
            ZoomImageView image=new ZoomImageView(requireContext());
            int width=getResources().getDisplayMetrics().widthPixels-32;
            image.setLayoutParams(new LinearLayout.LayoutParams(-1, Math.max(180, Math.min(620, width))));
            image.setContentDescription(m.altText()==null? "Solution figure":m.altText());
            image.setAdjustViewBounds(true);
            Glide.with(this).load(resolve(m.localPath()!=null && !m.localPath().isBlank() ? m.localPath() : m.deliveryPath()))
                    .override(1600,1600).fitCenter().placeholder(R.drawable.bg_media_placeholder)
                    .error(R.drawable.bg_media_error).into(image);
            content.addView(image);
            if (m.caption()!=null && !m.caption().isBlank()) {
                TextView cap=new TextView(requireContext()); cap.setText(m.caption()); cap.setTextSize(13);
                cap.setTextColor(getResources().getColor(R.color.text_secondary)); cap.setPadding(0,4,0,10);
                content.addView(cap);
            }
        }
    }

    private String resolve(String path) {
        if (path == null || path.isBlank()) return "";
        // Offline manifests rewrite media delivery URLs to file:// URIs.
        // Never prepend the API host to a local URI or an existing absolute path.
        if (path.startsWith("file://") || path.startsWith("content://")) return path;
        if (path.startsWith("http://") || path.startsWith("https://")) return path;
        java.io.File local = new java.io.File(path);
        if (local.isAbsolute() && local.exists()) return path;
        return bd.hscsolution.student.BuildConfig.API_BASE_URL.replace("/api/v1/","") + path;
    }

    private void restorePosition() {
        String key="reader_scroll_"+questionId;
        int y=requireContext().getSharedPreferences("reader",0).getInt(key,0);
        scroll.postDelayed(() -> scroll.scrollTo(0,y),180);
    }

    @Override public void onPause() {
        super.onPause();
        if (scroll!=null && questionId!=null)
            requireContext().getSharedPreferences("reader",0).edit()
                    .putInt("reader_scroll_"+questionId,scroll.getScrollY()).apply();
    }
}
