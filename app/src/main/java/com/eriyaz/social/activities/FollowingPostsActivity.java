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
import android.app.ActivityOptions;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.eriyaz.social.R;
import com.eriyaz.social.adapters.FollowPostsAdapter;
import com.eriyaz.social.fragments.PlaybackFragment;
import com.eriyaz.social.managers.PostManager;
import com.eriyaz.social.managers.listeners.OnPostChangedListener;
import com.eriyaz.social.model.FollowingPost;
import com.eriyaz.social.model.Post;
import com.eriyaz.social.model.Rating;
import com.eriyaz.social.model.RecordingItem;

import java.util.List;

public class FollowingPostsActivity extends BaseActivity{

    private FollowPostsAdapter postsAdapter;
    private PostManager postManager;
    private RecyclerView recyclerView;

    private ProgressBar progressBar;
    private SwipeRefreshLayout swipeContainer;
    private TextView message_following_posts_empty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_follow_posts);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        actionBar = getSupportActionBar();
        postManager = PostManager.getInstance(FollowingPostsActivity.this);

        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        initContentView();

        loadFollowingPosts();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && requestCode == PostDetailsActivity.UPDATE_POST_REQUEST) {
            postsAdapter.updateSelectedItem();
        }
    }

    public void loadFollowingPosts() {
        if (checkInternetConnection()) {
            if (getCurrentUserId() != null) {
                showLocalProgress();
                postManager.getFollowingPosts(getCurrentUserId(), list -> {
                    hideLocalProgress();
                    onFollowingPostsLoaded(list);
                    showEmptyListMessage(list.isEmpty());
                });
            } else {
                    showEmptyListMessage(true);
                    hideLocalProgress();
            }
        } else {
            hideLocalProgress();
        }
    }

    public void onRefresh() {
        loadFollowingPosts();
    }

    public void attemptOpenAuthorProfile(String postId, View authorView) {
        postManager.getSinglePostValue(postId, new OnPostChangedListener() {
            @Override
            public void onObjectChanged(Post obj) {
                openProfileActivity(obj.getAuthorId(), authorView);
            }

            @Override
            public void onError(String errorText) {
                showSnackBar(errorText);
            }
        });
    }

    public void attemptOpenPlayFragment(String postId, Rating rating, String authorName, View authorView) {
        postManager.getSinglePostValue(postId, new OnPostChangedListener() {
            @Override
            public void onObjectChanged(Post obj) {
                openPlayFragment(obj, rating, authorName, authorView);
            }

            @Override
            public void onError(String errorText) {
                showSnackBar(errorText);
            }
        });
    }

    protected void openPlayFragment(Post post, Rating rating, String authorName, View view) {
        try {
            RecordingItem item = new RecordingItem();
            item.setName(post.getTitle());
            item.setLength(post.getAudioDuration());
            item.setFilePath(post.getImagePath());
            PlaybackFragment playbackFragment =
                    new PlaybackFragment().newInstance(item, post, rating, authorName);
            FragmentTransaction transaction = ((AppCompatActivity)view.getContext()).getSupportFragmentManager()
                    .beginTransaction();
            playbackFragment.show(transaction, "dialog_playback");
        } catch (Exception e) {
            Log.e(TAG, "exception", e);
        }
    }

    public void onFollowingPostsLoaded(List<FollowingPost> list) {
        postsAdapter.setList(list);
    }

    public void showLocalProgress() {
        if (!swipeContainer.isRefreshing()) {
            progressBar.setVisibility(View.VISIBLE);
        }
    }

    public void hideLocalProgress() {
        swipeContainer.setRefreshing(false);
        progressBar.setVisibility(View.GONE);
    }

    public void showEmptyListMessage(boolean show) {
        message_following_posts_empty.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void initContentView() {
        if (recyclerView == null) {
            progressBar = findViewById(R.id.progressBar);
            message_following_posts_empty = findViewById(R.id.message_following_posts_empty);
            swipeContainer = findViewById(R.id.swipeContainer);
            swipeContainer.setOnRefreshListener(() -> onRefresh());

            initPostListRecyclerView();
        }
    }

    private void initPostListRecyclerView() {
        recyclerView = findViewById(R.id.recyclerView);
        postsAdapter = new FollowPostsAdapter(this);
        postsAdapter.setCallBack(new FollowPostsAdapter.CallBack() {
            @Override
            public void onItemClick(FollowingPost followingPost, View view) {
//                presenter.onPostClicked(followingPost.getPostId(), view);
                openPostDetailsActivity(followingPost.getPostId(), view);
            }

            @Override
            public void onPlayClick(FollowingPost followingPost, Rating rating, String authorName, View view) {
                attemptOpenPlayFragment(followingPost.getPostId(), rating, authorName, view);
            }

            @Override
            public void onAuthorClick(int position, View view) {
                String postId = postsAdapter.getItemByPosition(position).getPostId();
                attemptOpenAuthorProfile(postId, view);
            }
        });
        ((SimpleItemAnimator) recyclerView.getItemAnimator()).setSupportsChangeAnimations(false);
        recyclerView.setAdapter(postsAdapter);
    }

    @SuppressLint("RestrictedApi")
    public void openPostDetailsActivity(String postId, View v) {
        Intent intent = new Intent(FollowingPostsActivity.this, PostDetailsActivity.class);
        intent.putExtra(PostDetailsActivity.POST_ID_EXTRA_KEY, postId);

//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//
//            View imageView = v.findViewById(R.id.postImageView);
//            View authorImageView = v.findViewById(R.id.authorImageView);
//
//            ActivityOptions options = ActivityOptions.
//                    makeSceneTransitionAnimation(FollowingPostsActivity.this,
//                            new android.util.Pair<>(imageView, getString(R.string.post_image_transition_name)),
//                            new android.util.Pair<>(authorImageView, getString(R.string.post_author_image_transition_name))
//                    );
//            startActivityForResult(intent, PostDetailsActivity.UPDATE_POST_REQUEST, options.toBundle());
//        } else {
//            startActivityForResult(intent, PostDetailsActivity.UPDATE_POST_REQUEST);
//        }
        startActivityForResult(intent, PostDetailsActivity.UPDATE_POST_REQUEST);
    }

    @SuppressLint("RestrictedApi")
    public void openProfileActivity(String userId, View view) {
        Intent intent = new Intent(FollowingPostsActivity.this, ProfileActivity.class);
        intent.putExtra(ProfileActivity.USER_ID_EXTRA_KEY, userId);

//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && view != null) {
//
//            View authorImageView = view.findViewById(R.id.authorImageView);
//
//            ActivityOptions options = ActivityOptions.
//                    makeSceneTransitionAnimation(FollowingPostsActivity.this,
//                            new android.util.Pair<>(authorImageView, getString(R.string.post_author_image_transition_name)));
//            startActivityForResult(intent, ProfileActivity.CREATE_POST_FROM_PROFILE_REQUEST, options.toBundle());
//        } else {
//            startActivityForResult(intent, ProfileActivity.CREATE_POST_FROM_PROFILE_REQUEST);
//        }
        startActivityForResult(intent, ProfileActivity.CREATE_POST_FROM_PROFILE_REQUEST);
    }
}
