/*
 * Copyright 2017 Rozdoum
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

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.BackgroundColorSpan;
import android.text.style.TextAppearanceSpan;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.eriyaz.social.R;
import com.eriyaz.social.adapters.ProfileTabAdapter;
import com.eriyaz.social.enums.PostStatus;
import com.eriyaz.social.fragments.PostsByUserFragment;
import com.eriyaz.social.managers.ProfileManager;
import com.eriyaz.social.managers.listeners.OnObjectChangedListener;
import com.eriyaz.social.model.Profile;
import com.eriyaz.social.utils.LogUtil;
import com.eriyaz.social.utils.LogoutHelper;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ProfileActivity extends BaseActivity implements GoogleApiClient.OnConnectionFailedListener {
    private static final String TAG = ProfileActivity.class.getSimpleName();
    public static final int CREATE_POST_FROM_PROFILE_REQUEST = 22;
    public static final String USER_ID_EXTRA_KEY = "ProfileActivity.USER_ID_EXTRA_KEY";
    public static final String USER_POINTS_EXTRA_KEY = "ProfileActivity.USER_POINTS_EXTRA_KEY";

    // UI references.
    private TextView nameEditText;
    private View messageTextLayout;
    private ImageView imageView;
    private ProgressBar progressBar;
//    private TextView postsLabelTextView;

    private FirebaseAuth mAuth;
    private GoogleApiClient mGoogleApiClient;
    private String currentUserId;
    private String userID;
    private int userPoints;

    private TabLayout tabLayout;
    private ViewPager profileTabViewPager;

    private TextView pointsCountersTextView;
    private ProfileManager profileManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        actionBar = getSupportActionBar();

        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        userID = getIntent().getStringExtra(USER_ID_EXTRA_KEY);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser != null) {
            currentUserId = firebaseUser.getUid();
        }

        // Set up the login form.
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        imageView = (ImageView) findViewById(R.id.imageView);
        nameEditText = (TextView) findViewById(R.id.nameEditText);
        messageTextLayout = findViewById(R.id.messageTextLayout);
        pointsCountersTextView = (TextView) findViewById(R.id.pointsCountersTextView);
//        postsLabelTextView = (TextView) findViewById(R.id.postsLabelTextView);

        profileTabViewPager = findViewById(R.id.profileTabPager);
        ProfileTabAdapter profileTabAdapter = new ProfileTabAdapter(getSupportFragmentManager(), userID);
        profileTabViewPager.setAdapter(profileTabAdapter);

        tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(profileTabViewPager);

        messageTextLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                startMessageActivity();
            }
        });

        supportPostponeEnterTransition();
    }

    @Override
    public void onStart() {
        super.onStart();
        loadProfile();

        if (mGoogleApiClient != null) {
            mGoogleApiClient.connect();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        profileManager.closeListeners(this);

        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            mGoogleApiClient.stopAutoManage(this);
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        ProfileTabAdapter tabAdapter = (ProfileTabAdapter) profileTabViewPager.getAdapter();
        PostsByUserFragment selectedFragment;

        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case CreatePostActivity.CREATE_NEW_POST_REQUEST:
                    profileTabViewPager.setCurrentItem(1);
                    selectedFragment = tabAdapter.getSelectedFragment(profileTabViewPager.getCurrentItem());
                    selectedFragment.getPostsAdapter().loadPosts();
                    showSnackBar(R.string.message_post_was_created);
                    setResult(RESULT_OK);
                    break;

                case PostDetailsActivity.UPDATE_POST_REQUEST:
                    selectedFragment = tabAdapter.getSelectedFragment(profileTabViewPager.getCurrentItem());
                    if (data != null) {
                        PostStatus postStatus = (PostStatus) data.getSerializableExtra(PostDetailsActivity.POST_STATUS_EXTRA_KEY);
                        if (selectedFragment == null || selectedFragment.getPostsAdapter() == null) return;
                        if (postStatus.equals(PostStatus.REMOVED)) {
                            selectedFragment.getPostsAdapter().removeSelectedPost();

                        } else if (postStatus.equals(PostStatus.UPDATED)) {
                            selectedFragment.getPostsAdapter().updateSelectedPost();
                        }
                    }
                    break;
            }
        }
    }

    private Spannable buildCounterSpannable(String label, int value) {
        SpannableStringBuilder contentString = new SpannableStringBuilder();
        contentString.append(String.valueOf(value));
        contentString.append("\n");
        int start = contentString.length();
        contentString.append(label);
        contentString.setSpan(new TextAppearanceSpan(this, R.style.TextAppearance_Second_Light), start, contentString.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        contentString.setSpan(new TextAppearanceSpan(this, R.style.TextAppearance_Title_White), 0, start-1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        if (value > 0) {
            contentString.setSpan(new BackgroundColorSpan(getResources().getColor(R.color.light_green)), 0, start-1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        } else {
            contentString.setSpan(new BackgroundColorSpan(getResources().getColor(R.color.red)), 0, start-1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return contentString;
    }

    private void loadProfile() {
        profileManager = ProfileManager.getInstance(this);
        profileManager.getProfileValue(ProfileActivity.this, userID, createOnProfileChangedListener());
    }

    private OnObjectChangedListener<Profile> createOnProfileChangedListener() {
        return new OnObjectChangedListener<Profile>() {
            @Override
            public void onObjectChanged(Profile obj) {
                if (isActivityDestroyed()) return;
                fillUIFields(obj);
            }
        };
    }

    private void fillUIFields(Profile profile) {
        if (profile != null) {
            nameEditText.setText(profile.getUsername());

            if (profile.getPhotoUrl() != null) {
                Glide.with(this)
                        .load(profile.getPhotoUrl())
                        .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                        .crossFade()
                        .error(R.drawable.ic_stub)
                        .listener(new RequestListener<String, GlideDrawable>() {
                            @Override
                            public boolean onException(Exception e, String model, Target<GlideDrawable> target, boolean isFirstResource) {
                                scheduleStartPostponedTransition(imageView);
                                progressBar.setVisibility(View.GONE);
                                return false;
                            }

                            @Override
                            public boolean onResourceReady(GlideDrawable resource, String model, Target<GlideDrawable> target, boolean isFromMemoryCache, boolean isFirstResource) {
                                scheduleStartPostponedTransition(imageView);
                                progressBar.setVisibility(View.GONE);
                                return false;
                            }
                        })
                        .into(imageView);
            } else {
                progressBar.setVisibility(View.GONE);
                imageView.setImageResource(R.drawable.ic_stub);
            }

            userPoints = (int) profile.getPoints();
            String pointsLabel = getResources().getString(R.string.score_label);
            pointsCountersTextView.setText(buildCounterSpannable(pointsLabel, userPoints));
        }
    }

    private void scheduleStartPostponedTransition(final ImageView imageView) {
        imageView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                imageView.getViewTreeObserver().removeOnPreDrawListener(this);
                supportStartPostponedEnterTransition();
                return true;
            }
        });
    }

    private void startMainActivity() {
        Intent intent = new Intent(ProfileActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

    private void startEditProfileActivity() {
        if (hasInternetConnection()) {
            Intent intent = new Intent(ProfileActivity.this, EditProfileActivity.class);
            startActivity(intent);
        } else {
            showSnackBar(R.string.internet_connection_failed);
        }
    }

    private void startMessageActivity() {
        if (hasInternetConnection()) {
            Intent intent = new Intent(ProfileActivity.this, MessageActivity.class);
            intent.putExtra(ProfileActivity.USER_ID_EXTRA_KEY, userID);
            startActivity(intent);
        } else {
            showSnackBar(R.string.internet_connection_failed);
        }
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        LogUtil.logDebug(TAG, "onConnectionFailed:" + connectionResult);
    }

    private void openCreatePostActivity() {
        Intent intent = new Intent(this, CreatePostActivity.class);
        startActivityForResult(intent, CreatePostActivity.CREATE_NEW_POST_REQUEST);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (userID.equals(currentUserId)) {
            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.profile_menu, menu);
            return true;
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.editProfile:
                startEditProfileActivity();
                return true;
            case R.id.signOut:
                LogoutHelper.signOut(mGoogleApiClient, this, true);
                startMainActivity();
                return true;
            case R.id.createPost:
                if (hasInternetConnection()) {
                    openCreatePostActivity();
                } else {
                    showSnackBar(R.string.internet_connection_failed);
                }
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void updatePost() {
        ProfileTabAdapter tabAdapter = (ProfileTabAdapter) profileTabViewPager.getAdapter();
        PostsByUserFragment selectedFragment = tabAdapter.getSelectedFragment(profileTabViewPager.getCurrentItem());
        selectedFragment.getPostsAdapter().updateSelectedPost();
    }
}
