package com.eriyaz.social.activities;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.eriyaz.social.R;
import com.eriyaz.social.adapters.BoughtFeedbackAdapter;
import com.eriyaz.social.adapters.CommentsAdapter;
import com.eriyaz.social.managers.BoughtFeedbackManager;
import com.eriyaz.social.managers.CommentManager;
import com.eriyaz.social.managers.listeners.OnDataChangedListener;
import com.eriyaz.social.model.BoughtFeedback;
import com.eriyaz.social.model.Comment;

import java.util.ArrayList;
import java.util.List;


public class AdminActivity extends AppCompatActivity {
    private RecyclerView commentsRecyclerView;
    private BoughtFeedbackAdapter commentsAdapter;
    private BoughtFeedbackManager commentManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        commentsAdapter = new BoughtFeedbackAdapter();
        commentsAdapter.setCallback(new BoughtFeedbackAdapter.Callback() {

            @Override
            public void toggleResolveClick(String postId) {
                toggleResolve(postId);
            }

            @Override
            public void onAuthorClick(String authorId) {
                openProfileActivity(authorId);
            }
        });
        commentsRecyclerView = (RecyclerView) findViewById(R.id.boughtFeedbackRecyclerView);
        commentsRecyclerView.setAdapter(commentsAdapter);
        commentsRecyclerView.setNestedScrollingEnabled(false);
        commentsRecyclerView.addItemDecoration(new DividerItemDecoration(commentsRecyclerView.getContext(),
                ((LinearLayoutManager) commentsRecyclerView.getLayoutManager()).getOrientation()));

        commentManager = BoughtFeedbackManager.getInstance(this);
        commentManager.getBoughtFeedbacksList(this,  new OnDataChangedListener<BoughtFeedback>() {
            @Override
            public void onListChanged(List<BoughtFeedback> list) {
                commentsRecyclerView.setVisibility(View.VISIBLE);
                commentsAdapter.setList(list);
            }
        });

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        commentManager.closeListeners(this);
    }

    private void toggleResolve(String postId) {
        commentManager.toggleBoughtFeedback(postId);
    }


    private void openProfileActivity(String userId) {
        Intent intent = new Intent(AdminActivity.this, ProfileActivity.class);
        intent.putExtra(ProfileActivity.USER_ID_EXTRA_KEY, userId);
        startActivity(intent);
    }
}
