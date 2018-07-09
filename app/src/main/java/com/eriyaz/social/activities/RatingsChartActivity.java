package com.eriyaz.social.activities;

import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;

import com.eriyaz.social.R;
import com.eriyaz.social.adapters.BoughtFeedbackAdapter;
import com.eriyaz.social.managers.BoughtFeedbackManager;


public class RatingsChartActivity extends BaseActivity {
    private RecyclerView commentsRecyclerView;
    private BoughtFeedbackAdapter commentsAdapter;
    private BoughtFeedbackManager commentManager;
    private TextView ratingsText;
    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ratings_chart);
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        ratingsText = findViewById(R.id.textView);
        ratingsText.setText(Html.fromHtml("i love <b>html</b>. What <i>about</i> yoU?"));

        webView = findViewById(R.id.webView);
        webView.setWebViewClient(new WebViewClient());

//        setContentView(webView);
        webView.clearCache(true);
        webView.loadUrl("https://eriyaz-social-dev.firebaseapp.com/ratings_chart.html");

//        webView.loadData("<table><tr><td>1</td><td>average</td></tr><tr><td>2</td><td>good</td></tr></table>", "text/html", null);
    }
}
