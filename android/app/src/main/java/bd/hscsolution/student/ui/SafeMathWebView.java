package bd.hscsolution.student.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebSettings;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class SafeMathWebView extends WebView {
    private static final String MATHJAX =
            "https://cdn.jsdelivr.net/npm/mathjax@3/es5/tex-chtml.js";

    public SafeMathWebView(@NonNull Context c) { super(c); init(); }
    public SafeMathWebView(@NonNull Context c, @Nullable AttributeSet a) { super(c, a); init(); }

    @SuppressLint("SetJavaScriptEnabled")
    private void init() {
        setBackgroundColor(Color.TRANSPARENT);
        setVerticalScrollBarEnabled(false);
        setHorizontalScrollBarEnabled(false);
        WebSettings s = getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(false);
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(false);
        s.setAllowFileAccessFromFileURLs(false);
        s.setAllowUniversalAccessFromFileURLs(false);
        s.setBuiltInZoomControls(false);
        s.setDisplayZoomControls(false);

        setWebChromeClient(new WebChromeClient());
        setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return true;
            }
            @Override public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                view.evaluateJavascript(
                    "(function(){return Math.max(document.body.scrollHeight,document.documentElement.scrollHeight);})()",
                    value -> {
                        try {
                            int cssPx = Integer.parseInt(value.replaceAll("[^0-9]", ""));
                            int h = Math.max(120, Math.min(1400,
                                    (int)(cssPx * getResources().getDisplayMetrics().density)));
                            getLayoutParams().height = h;
                            requestLayout();
                        } catch (Exception ignored) {}
                    });
            }
        });
    }

    public void render(String text) {
        String safe = ReaderHtml.escapeAndFormat(text);
        String html = "<!doctype html><html><head>"
                + "<meta name='viewport' content='width=device-width, initial-scale=1, maximum-scale=1'>"
                + "<script>window.MathJax={tex:{inlineMath:[['\\\\(','\\\\)']],displayMath:[['$$','$$'],['\\\\[','\\\\]']]},svg:{fontCache:'global'}};</script>"
                + "<script src='" + MATHJAX + "'></script>"
                + "<style>body{font-family:sans-serif;color:#16202A;background:transparent;margin:0;padding:0;"
                + "font-size:17px;line-height:1.72} @media(prefers-color-scheme:dark){body{color:#F1F5F9}}"
                + ".math{overflow-x:auto} code{font-family:monospace}</style>"
                + "</head><body><div id='content'>" + safe + "</div>"
                + "</body></html>";
        loadDataWithBaseURL("https://reader.invalid/", html, "text/html", "UTF-8", null);
    }

    @Override protected void onDetachedFromWindow() {
        stopLoading();
        destroy();
        super.onDetachedFromWindow();
    }
}
