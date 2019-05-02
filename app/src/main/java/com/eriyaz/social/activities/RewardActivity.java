package com.eriyaz.social.activities;

import android.app.ActivityOptions;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.eriyaz.social.R;
import com.eriyaz.social.adapters.BoughtFeedbackAdapter;
import com.eriyaz.social.adapters.PostsAdapter;
import com.eriyaz.social.managers.BoughtFeedbackManager;
import com.eriyaz.social.managers.PostManager;
import com.eriyaz.social.managers.listeners.OnDataChangedListener;
import com.eriyaz.social.managers.listeners.OnObjectExistListener;
import com.eriyaz.social.model.BoughtFeedback;
import com.eriyaz.social.model.Post;

import java.util.List;


public class RewardActivity extends BaseActivity {
    private PostsAdapter postsAdapter;
    private RecyclerView recyclerView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reward);
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        final ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressBar);
        SwipeRefreshLayout swipeContainer = (SwipeRefreshLayout) findViewById(R.id.swipeContainer);
        recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        postsAdapter = new PostsAdapter(this, swipeContainer);
        postsAdapter.setCallback(new PostsAdapter.Callback() {
            @Override
            public void onItemClick(final Post post, final View view) {
                if (!hasInternetConnection()) {
                    showFloatButtonRelatedSnackBar(R.string.internet_connection_failed);
                    return;
                }
                openPostDetailsActivity(post, view);
            }

            @Override
            public void onListLoadingFinished() {
                progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onAuthorClick(Post post, View view) {
                openProfileActivity(post.getAuthorId(), view);
            }

            @Override
            public void onCanceled(String message) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(RewardActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        ((SimpleItemAnimator) recyclerView.getItemAnimator()).setSupportsChangeAnimations(false);
        recyclerView.setAdapter(postsAdapter);
        postsAdapter.loadFirstPage();

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
            }
        });
        if (!hasInternetConnection())
            showFloatButtonRelatedSnackBar(R.string.internet_connection_failed);
    }


    private void openPostDetailsActivity(Post post, View v) {
        Intent intent = new Intent(RewardActivity.this, PostDetailsActivity.class);
        intent.putExtra(PostDetailsActivity.POST_ID_EXTRA_KEY, post.getId());
        intent.putExtra(PostDetailsActivity.IS_ADMIN_EXTRA_KEY, true);
        startActivityForResult(intent, PostDetailsActivity.UPDATE_POST_REQUEST);
    }

    private void openProfileActivity(String userId, View view) {
        Intent intent = new Intent(RewardActivity.this, ProfileActivity.class);
        intent.putExtra(ProfileActivity.USER_ID_EXTRA_KEY, userId);
        startActivityForResult(intent, ProfileActivity.CREATE_POST_FROM_PROFILE_REQUEST);
    }
}
