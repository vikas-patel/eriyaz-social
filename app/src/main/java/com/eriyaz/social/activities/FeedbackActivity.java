package com.eriyaz.social.activities;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.EditText;

import com.eriyaz.social.ApplicationHelper;
import com.eriyaz.social.R;
import com.eriyaz.social.managers.listeners.OnTaskCompleteListener;

public class FeedbackActivity extends BaseActivity {

    public static final int CREATE_FEEDBACK = 25;
    protected EditText feedbackEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feedback);
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        feedbackEditText = findViewById(R.id.descriptionEditText);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.send_feedback_menu, menu);
        return true;
    }

    /**
     * Report an issue, suggest a feature, or send feedback.
     */
    private void sendFeedback() {
        String feedbackText = feedbackEditText.getText().toString().trim();
        ApplicationHelper.getDatabaseHelper().createFeedback(feedbackText, new OnTaskCompleteListener() {
            @Override
            public void onTaskComplete(boolean success) {
                setResult(RESULT_OK);
                FeedbackActivity.this.finish();
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.send:
                if (hasInternetConnection()) {
                    sendFeedback();
                } else {
                    showSnackBar(R.string.internet_connection_failed);
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
