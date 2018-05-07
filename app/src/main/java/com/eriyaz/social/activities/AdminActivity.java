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
            public void onLongItemClick(View view, int position) {
//                Comment selectedComment = commentsAdapter.getItemByPosition(position);
//                startActionMode(selectedComment);
            }

            @Override
            public void onAuthorClick(String authorId, View view) {
                openProfileActivity(authorId, view);
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


    private void openProfileActivity(String userId, View view) {
        Intent intent = new Intent(AdminActivity.this, ProfileActivity.class);
        intent.putExtra(ProfileActivity.USER_ID_EXTRA_KEY, userId);

//        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && view != null) {
//
//            ActivityOptions options = ActivityOptions.
//                    makeSceneTransitionAnimation(PostDetailsActivity.this,
//                            new android.util.Pair<>(view, getString(R.string.post_author_image_transition_name)));
//            startActivity(intent, options.toBundle());
//        } else {
        startActivity(intent);
//        }
    }
}
