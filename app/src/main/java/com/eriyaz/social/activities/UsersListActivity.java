/*
 * Copyright 2018 Rozdoum
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.eriyaz.social.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.eriyaz.social.R;
import com.eriyaz.social.adapters.UsersAdapter;
import com.eriyaz.social.adapters.holders.UserViewHolder;
import com.eriyaz.social.managers.LikeManager;
import com.eriyaz.social.model.LikeUser;

import java.util.List;

//import com.eriyaz.social.views.FollowButton;

/**
 * Created by Alexey on 03.05.18.
 */

public class UsersListActivity extends BaseActivity {
    private static final String TAG = UsersListActivity.class.getSimpleName();

    public static final String COMMENT_ID_EXTRA_KEY = "UsersListActivity.COMMENT_ID_EXTRA_KEY";

    private UsersAdapter usersAdapter;
    private RecyclerView recyclerView;
    private String commentID;

    private ProgressBar progressBar;
    private SwipeRefreshLayout swipeContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_users_list);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        actionBar = getSupportActionBar();

        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        commentID = getIntent().getStringExtra(COMMENT_ID_EXTRA_KEY);

        initContentView();
        loadUsersList(commentID);
    }

    public void loadUsersList(String commentID) {
        if (checkInternetConnection()) {
            showLocalProgress();

            LikeManager.getInstance(UsersListActivity.this).getCommentLikeUsersList(commentID, list -> {
                    hideLocalProgress();
                    onProfilesIdsListLoaded(list);
            });
        }
    }


    private void initContentView() {
        if (recyclerView == null) {
            progressBar = findViewById(R.id.progressBar);
            swipeContainer = findViewById(R.id.swipeContainer);
            swipeContainer.setOnRefreshListener(() -> loadUsersList(commentID));

            initProfilesListRecyclerView();
        }
    }

    private void initProfilesListRecyclerView() {
        recyclerView = findViewById(R.id.usersRecyclerView);
        usersAdapter = new UsersAdapter(this);
        usersAdapter.setCallback(new UserViewHolder.Callback() {
            @Override
            public void onItemClick(int position, View view) {
                LikeUser user = usersAdapter.getItemByPosition(position);
                openProfileActivity(user.getProfileId(), view);
            }

            @Override
            public void onMessageButtonClick(int position) {
                LikeUser user = usersAdapter.getItemByPosition(position);
                openMessageActivity(user.getProfileId());
            }
        });

        recyclerView.addItemDecoration(new DividerItemDecoration(recyclerView.getContext(),
                ((LinearLayoutManager) recyclerView.getLayoutManager()).getOrientation()));

        recyclerView.setAdapter(usersAdapter);

    }

    @SuppressLint("RestrictedApi")
    private void openProfileActivity(String userId, View view) {
        if (hasInternetConnection()) {
            Intent intent = new Intent(UsersListActivity.this, ProfileActivity.class);
            intent.putExtra(ProfileActivity.USER_ID_EXTRA_KEY, userId);
            startActivity(intent);
        } else {
            showSnackBar(R.string.internet_connection_failed);
        }

//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && view != null) {
//
//            ImageView imageView = view.findViewById(R.id.photoImageView);
//
//            ActivityOptions options = ActivityOptions.
//                    makeSceneTransitionAnimation(UsersListActivity.this,
//                            new android.util.Pair<>(imageView, getString(R.string.post_author_image_transition_name)));
//            startActivityForResult(intent, UPDATE_FOLLOWING_STATE_REQ, options.toBundle());
//        } else {
//            startActivityForResult(intent, UPDATE_FOLLOWING_STATE_REQ);
//        }
    }

    private void openMessageActivity(String userId) {
        if (hasInternetConnection()) {
            Intent intent = new Intent(UsersListActivity.this, MessageActivity.class);
            intent.putExtra(ProfileActivity.USER_ID_EXTRA_KEY, userId);
            startActivity(intent);
        } else {
            showSnackBar(R.string.internet_connection_failed);
        }
    }

    public void onProfilesIdsListLoaded(List<LikeUser> list) {
        usersAdapter.setList(list);
    }

    public void showLocalProgress() {
        progressBar.setVisibility(View.VISIBLE);
    }

    public void hideLocalProgress() {
        progressBar.setVisibility(View.GONE);
        swipeContainer.setRefreshing(false);
    }
}
