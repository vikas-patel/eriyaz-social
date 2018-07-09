package com.eriyaz.social.activities;

import android.graphics.Bitmap;
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
    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ratings_chart);
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        webView = findViewById(R.id.webView);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon){
                // Page loading started
                showProgress(R.string.loading);
            }

            /*
                public void onPageFinished (WebView view, String url)
                    Notify the host application that a page has finished loading. This
                    method is called only for main frame. When onPageFinished() is called,
                    the rendering picture may not be updated yet. To get the notification
                    for the new Picture, use onNewPicture(WebView, Picture).

                Parameters
                    view WebView: The WebView that is initiating the callback.
                    url String: The url of the page.
            */
            @Override
            public void onPageFinished(WebView view, String url){
                // Page loading finished
                hideProgress();
            }
        });

//        setContentView(webView);
        webView.clearCache(true);
        webView.loadUrl("https://eriyaz-social-dev.firebaseapp.com/ratings_chart.html");

//        webView.loadData("<table><tr><td>1</td><td>average</td></tr><tr><td>2</td><td>good</td></tr></table>", "text/html", null);
    }
}
