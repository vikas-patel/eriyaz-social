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

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.TextAppearanceSpan;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.eriyaz.social.R;
import com.eriyaz.social.adapters.ProfileTabAdapter;
import com.eriyaz.social.enums.PostStatus;
import com.eriyaz.social.fragments.PostsByUserFragment;
import com.eriyaz.social.fragments.requestFragment;
import com.eriyaz.social.managers.ProfileManager;
import com.eriyaz.social.managers.listeners.OnObjectChangedListener;
import com.eriyaz.social.model.Post;
import com.eriyaz.social.model.Profile;
import com.eriyaz.social.utils.GlideApp;
import com.eriyaz.social.utils.ImageUtil;
import com.eriyaz.social.utils.LogUtil;
import com.eriyaz.social.utils.LogoutHelper;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ProfileActivity extends BaseCurrentProfileActivity implements GoogleApiClient.OnConnectionFailedListener {
    private static final String TAG = ProfileActivity.class.getSimpleName();
    public static final int CREATE_POST_FROM_PROFILE_REQUEST = 22;
    public static final String USER_ID_EXTRA_KEY = "ProfileActivity.USER_ID_EXTRA_KEY";
    public static final String USER_POINTS_EXTRA_KEY = "ProfileActivity.USER_POINTS_EXTRA_KEY";
    public static final String RATING_TAB_DEFAULT_EXTRA_KEY = "ProfileActivity.RATING_TAB_DEFAULT_EXTRA_KEY";

    // UI references.
    private TextView nameEditText;
    private View messageTextLayout, requestFeedback;
    private ImageView imageView;
    private ProgressBar progressBar;
//    private TextView postsLabelTextView;

    private FirebaseAuth mAuth;
    private GoogleApiClient mGoogleApiClient;
    static public String currentUserId;
    static public String userID;
    private int userPoints;
    private int reputationsPoints;
    private int likesCount;
    private boolean isRatingTabDefault;

    private TabLayout tabLayout;
    private ViewPager profileTabViewPager;

    private TextView pointsCountersTextView;
    private TextView reputationsCountersTextView;
    private TextView likesCountTextView;
    private ProfileManager profileManager;
    final FirebaseRemoteConfig remoteConfig = FirebaseRemoteConfig.getInstance();


    private DatabaseReference databaseReference;
    public static List<Post> items = new ArrayList<Post>();
    public static String str[];


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
        isRatingTabDefault = getIntent().getBooleanExtra(RATING_TAB_DEFAULT_EXTRA_KEY, false);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser != null) {
            currentUserId = firebaseUser.getUid();
        }

        // Set up the login form.
        progressBar = findViewById(R.id.progressBar);
        imageView = findViewById(R.id.imageView);
        nameEditText = findViewById(R.id.nameEditText);
        messageTextLayout = findViewById(R.id.messageTextLayout);
        requestFeedback = findViewById(R.id.requestFeedbackLayout);
        pointsCountersTextView = findViewById(R.id.pointsCountersTextView);
        reputationsCountersTextView = findViewById(R.id.reputationsCountersTextView);
        likesCountTextView = findViewById(R.id.likesCountTextView);
//        postsLabelTextView = (TextView) findViewById(R.id.postsLabelTextView);

        profileTabViewPager = findViewById(R.id.profileTabPager);
        ProfileTabAdapter profileTabAdapter = new ProfileTabAdapter(getSupportFragmentManager(), userID);
        profileTabViewPager.setAdapter(profileTabAdapter);

        tabLayout = findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(profileTabViewPager);
        if (isRatingTabDefault) {
            profileTabViewPager.setCurrentItem(1);
        }

        if (firebaseUser != null && firebaseUser.getUid().equals(userID)) {
            requestFeedback.setVisibility(View.GONE);
        }

        requestFeedback.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (hasInternetConnection()) {
                    // Show loading fragment while fetch song list
                    ProgressDialog mProgressDialog;
                    mProgressDialog = new ProgressDialog(ProfileActivity.this);
                    mProgressDialog.setMessage("Loading..");
                    mProgressDialog.setCanceledOnTouchOutside(true);
                    mProgressDialog.setCancelable(false);
                    mProgressDialog.show();

                    databaseReference = FirebaseDatabase.getInstance().getReference("posts");

                    databaseReference.addValueEventListener(new ValueEventListener() {

                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            items.clear();
                            for (DataSnapshot d : dataSnapshot.getChildren()) {
                                Post p = d.getValue(Post.class);

                                if (p.getAuthorId().equals(currentUserId))
                                    items.add(p);
                            }
                            mProgressDialog.dismiss();

                            // check if list is empty, if yes it show toast message
                            if (items.size() != 0) {
                                str = new String[items.size()];
                                // ArrayList to Array Conversion
                                for (int j = 0; j < items.size(); j++)
                                    str[j] = String.valueOf(items.get(j).getTitle());

                                requestFragment requestfragment = new requestFragment();
                                requestfragment.show(getFragmentManager(), "ddd");
                            } else
                                Toast.makeText(ProfileActivity.this, "You don't have posted any song.", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });
                } else
                    showSnackBar(R.string.internet_connection_failed);

            }
        });

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
                    selectedFragment = tabAdapter.getSelectedFragment(profileTabViewPager.getCurrentItem());
                    selectedFragment.getPostsAdapter().loadFirstPage();
                    showSnackBar(R.string.message_post_was_created);
                    setResult(RESULT_OK);
                    break;

                case PostDetailsActivity.UPDATE_POST_REQUEST:
                    selectedFragment = tabAdapter.getSelectedFragment(profileTabViewPager.getCurrentItem());
                    if (data != null) {
                        PostStatus postStatus = (PostStatus) data.getSerializableExtra(PostDetailsActivity.POST_STATUS_EXTRA_KEY);
                        if (selectedFragment == null || selectedFragment.getPostsAdapter() == null)
                            return;
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
        contentString.append(label + " ");
//        contentString.append("\n");
        int start = contentString.length();
        contentString.append(String.valueOf(value));
//        contentString.setSpan(new TextAppearanceSpan(this, R.style.TextAppearance_Title_White), start, contentString.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        contentString.setSpan(new TextAppearanceSpan(this, R.style.TextAppearance_Second_Light), 0, start, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
//        if (value > 0) {
//            contentString.setSpan(new BackgroundColorSpan(getResources().getColor(R.color.light_green)), start, contentString.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
//        } else {
//            contentString.setSpan(new BackgroundColorSpan(getResources().getColor(R.color.red)), start, contentString.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
//        }
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
                ImageUtil.loadImage(GlideApp.with(this), profile.getPhotoUrl(), imageView, new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                        scheduleStartPostponedTransition(imageView);
                        progressBar.setVisibility(View.GONE);
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                        scheduleStartPostponedTransition(imageView);
                        progressBar.setVisibility(View.GONE);
                        return false;
                    }
                });
            } else {
                progressBar.setVisibility(View.GONE);
                imageView.setImageDrawable(ImageUtil.getTextDrawable(profile.getUsername(),
                        getResources().getDimensionPixelSize(R.dimen.profile_screen_avatar_size),
                        getResources().getDimensionPixelSize(R.dimen.profile_screen_avatar_size)));
//                imageView.setImageResource(R.drawable.ic_stub);
            }

            userPoints = (int) profile.getPoints();
            reputationsPoints = (int) profile.getReputationPoints();
            likesCount = profile.getLikesCount();
            String pointsLabel = getResources().getString(R.string.score_label);
            pointsCountersTextView.setText(buildCounterSpannable(pointsLabel, userPoints));
            String reputationsLabel = getResources().getString(R.string.reputation_label);
            reputationsCountersTextView.setText(buildCounterSpannable(reputationsLabel, reputationsPoints));
            String likesLabel = getResources().getString(R.string.likes_label);
            likesCountTextView.setText(buildCounterSpannable(likesLabel, likesCount));
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
        int points_post_create = (int) remoteConfig.getLong("points_post_create");
//        if (userPoints == null) userPoints = 0L;
        if (userPoints < points_post_create) {
            int pointsNeeded = points_post_create - (int) userPoints;
            String pointsNeededMsg = getResources().getQuantityString(R.plurals.points_needed_text, pointsNeeded, pointsNeeded);
            showWarningDialog(pointsNeededMsg);
            return;
        }
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
