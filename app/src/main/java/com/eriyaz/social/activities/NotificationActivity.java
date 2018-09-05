package com.eriyaz.social.activities;

import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.eriyaz.social.R;
import com.eriyaz.social.adapters.NotificationAdapter;
import com.eriyaz.social.managers.ProfileManager;

/**
 * Created by vikas on 13/2/18.
 */

public class NotificationActivity extends BaseActivity {
    private RecyclerView recyclerView;
    private ProgressBar progressBar;

    private String userId;

    private NotificationAdapter adapter;
    private SwipeRefreshLayout swipeContainer;
    private ProfileManager profileManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        actionBar = getSupportActionBar();

        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        userId = getIntent().getStringExtra(ProfileActivity.USER_ID_EXTRA_KEY);
        profileManager = ProfileManager.getInstance(this);

        progressBar = (ProgressBar) findViewById(R.id.progressBar);

        swipeContainer = (SwipeRefreshLayout) findViewById(R.id.swipeContainer);
        recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        profileManager.resetUnseenNotificationCount();
        analytics.openNotificationActivity();
        supportPostponeEnterTransition();
        adapter = new NotificationAdapter(userId, NotificationActivity.this, swipeContainer);
        adapter.setCallback(new NotificationAdapter.Callback() {

            @Override
            public void onListLoadingFinished() {
                progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onCanceled(String message) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(NotificationActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        ((SimpleItemAnimator) recyclerView.getItemAnimator()).setSupportsChangeAnimations(false);
        recyclerView.setAdapter(adapter);
        adapter.loadFirstPage();

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        profileManager.closeListeners(this);
    }
}
