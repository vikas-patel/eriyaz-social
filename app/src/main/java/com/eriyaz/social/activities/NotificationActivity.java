package com.eriyaz.social.activities;

import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ProgressBar;

import com.eriyaz.social.R;
import com.eriyaz.social.adapters.NotificationAdapter;
import com.eriyaz.social.managers.ProfileManager;
import com.eriyaz.social.managers.listeners.OnDataChangedListener;
import com.eriyaz.social.model.Notification;

import java.util.List;

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
        swipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                onRefreshAction();
            }
        });

        loadNotificationList();
        profileManager.resetUnseenNotificationCount();
        supportPostponeEnterTransition();
    }

    private void onRefreshAction() {
        profileManager.getNotificationsList(userId, createOnNotificationsChangedDataListener());
    }

    private void loadNotificationList() {
        if (recyclerView == null) {
            recyclerView = findViewById(R.id.recycler_view);
            adapter = new NotificationAdapter();
            ((SimpleItemAnimator) recyclerView.getItemAnimator()).setSupportsChangeAnimations(false);
            recyclerView.setAdapter(adapter);
            recyclerView.addItemDecoration(new DividerItemDecoration(recyclerView.getContext(),
                    ((LinearLayoutManager) recyclerView.getLayoutManager()).getOrientation()));
            profileManager.getNotificationsList(userId, createOnNotificationsChangedDataListener());
        }
    }

    private OnDataChangedListener<Notification> createOnNotificationsChangedDataListener() {

        return new OnDataChangedListener<Notification>() {
            @Override
            public void onListChanged(List<Notification> list) {
                swipeContainer.setRefreshing(false);
                progressBar.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
                adapter.setList(list);
            }
        };
    }
}
