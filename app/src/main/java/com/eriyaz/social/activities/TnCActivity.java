package com.eriyaz.social.activities;

import android.os.Bundle;
import android.text.Html;
import android.widget.TextView;

import com.eriyaz.social.R;

public class TnCActivity extends BaseActivity {

    private TextView tncTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tnc);
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        tncTextView = findViewById(R.id.tncText);
        tncTextView.setText(Html.fromHtml(getString(R.string.tnc_html)));
    }

}
