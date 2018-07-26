/*
 *  Copyright 2017 Rozdoum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.eriyaz.social.activities;

import android.app.ActivityOptions;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.LayerDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.eriyaz.social.BuildConfig;
import com.eriyaz.social.Constants;
import com.eriyaz.social.ForceUpdateChecker;
import com.eriyaz.social.R;
import com.eriyaz.social.adapters.PostsAdapter;
import com.eriyaz.social.behaviors.MoveUpwardBehavior;
import com.eriyaz.social.enums.PostStatus;
import com.eriyaz.social.enums.ProfileStatus;
import com.eriyaz.social.fragments.PlaybackFragment;
import com.eriyaz.social.managers.DatabaseHelper;
import com.eriyaz.social.managers.PostManager;
import com.eriyaz.social.managers.listeners.OnObjectChangedListener;
import com.eriyaz.social.managers.listeners.OnObjectExistListener;
import com.eriyaz.social.model.Post;
import com.eriyaz.social.model.Profile;
import com.eriyaz.social.utils.AnimationUtils;
import com.eriyaz.social.utils.DeepLinkUtil;
import com.eriyaz.social.utils.LogUtil;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks;
import com.google.firebase.dynamiclinks.PendingDynamicLinkData;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.uxcam.UXCam;

import static com.eriyaz.social.utils.ImageUtil.setBadgeCount;

public class MainActivity extends BaseActivity implements ForceUpdateChecker.OnUpdateNeededListener{
    private static final String TAG = MainActivity.class.getSimpleName();

    private PostsAdapter postsAdapter;
    private RecyclerView recyclerView;
//    private Button recordButton;
    private Button recordButton;

    private PostManager postManager;
    private TextView newPostsCounterTextView;
    private PostManager.PostCounterWatcher postCounterWatcher;
    private long WARNING_MIN_POINTS = 5;
    private boolean counterAnimationInProgress = false;
    private long userPoints = 0;
    final FirebaseRemoteConfig remoteConfig = FirebaseRemoteConfig.getInstance();
    private Profile profile;

    // private Snackbar karmaSnackbar;
    static {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        postManager = PostManager.getInstance(this);
        ProfileStatus profileStatus = profileManager.checkProfile();
        if(profileStatus.equals(ProfileStatus.NOT_AUTHORIZED) || profileStatus.equals(ProfileStatus.NO_PROFILE)) {
            if (!BuildConfig.DEBUG) {
                UXCam.startWithKey("8e284e93d1b8286");
            }
            doAuthorization(profileStatus);
        }

        initContentView();

        postCounterWatcher = new PostManager.PostCounterWatcher() {
            @Override
            public void onPostCounterChanged(int newValue) {
                updateNewPostCounter();
            }
        };

        postManager.setPostCounterWatcher(postCounterWatcher);

        getDynamicLink();
        ForceUpdateChecker.with(this).onUpdateNeeded(this).check();
    }

    @Override
    public void onUpdateCompulsory() {
        AlertDialog dialog = new BaseAlertDialogBuilder(this)
                .setTitle("New version available")
                .setMessage("Please, update app to new version to continue reposting.")
//                .setNegativeButton("No, thanks", null)
                .setPositiveButton("Update", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        redirectStore();
                    }
                }).create();
        dialog.setCancelable(false);
//        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }

    @Override
    public void onUpdateReminder(boolean isPersistent) {
        showUpdateAppBanner(isPersistent);
    }

    private void redirectStore() {
        final Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.app_url)));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }


    private OnObjectChangedListener<Profile> createOnProfileChangedListener() {
        return new OnObjectChangedListener<Profile>() {
            @Override
            public void onObjectChanged(Profile aProfile) {
                profile = aProfile;
                userPoints = profile.getPoints();
//                if (userPoints < WARNING_MIN_POINTS) showShareAppBanner();
                invalidateOptionsMenu();
            }
        };
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateNewPostCounter();
        if (profileManager.checkProfile().equals(ProfileStatus.PROFILE_CREATED)) {
            profileManager.getProfileValue(MainActivity.this,
                    FirebaseAuth.getInstance().getCurrentUser().getUid(),
                    createOnProfileChangedListener());
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        profileManager.closeListeners(MainActivity.this);
    }

    private void showShareAppBanner() {
        int reward_points = (int) remoteConfig.getLong("reward_points");
        final Snackbar snackbar = Snackbar.make(recordButton, String.format(getString(R.string.app_share_banner), reward_points), Snackbar.LENGTH_LONG)
                .setActionTextColor(getResources().getColor(R.color.accent))
                .setAction("SHARE", new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onShareClick();
            }
        });
        snackbar.getView().setBackgroundColor(getResources().getColor(R.color.red));
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                snackbar.show();
            }
        }, 6000);
    }

    private void showUpdateAppBanner(boolean isPersistent) {
        final Snackbar snackbar = Snackbar.make(recordButton, getString(R.string.app_update_banner), isPersistent? Snackbar.LENGTH_INDEFINITE : Snackbar.LENGTH_LONG)
                .setActionTextColor(getResources().getColor(R.color.accent))
                .setAction("UPDATE", new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        redirectStore();
                    }
                });
        snackbar.getView().setBackgroundColor(getResources().getColor(R.color.red));
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                snackbar.show();
            }
        }, 6000);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case ProfileActivity.CREATE_POST_FROM_PROFILE_REQUEST:
                    refreshPostList();
                    break;
                case CreatePostActivity.CREATE_NEW_POST_REQUEST:
                    refreshPostList();
                    showFloatButtonRelatedSnackBar(R.string.message_post_was_created);
                    profileManager.getProfileSingleValue(FirebaseAuth.getInstance().getCurrentUser().getUid(), createProfilePostCountListener());
                    break;

                case PostDetailsActivity.UPDATE_POST_REQUEST:
                    if (data != null) {
                        PostStatus postStatus = (PostStatus) data.getSerializableExtra(PostDetailsActivity.POST_STATUS_EXTRA_KEY);
                        if (postStatus.equals(PostStatus.REMOVED)) {
                            postsAdapter.removeSelectedPost();
                            showFloatButtonRelatedSnackBar(R.string.message_post_was_removed);
                        } else if (postStatus.equals(PostStatus.UPDATED)) {
                            postsAdapter.updateSelectedPost();
                        }
                    }
                    break;
                case FeedbackActivity.CREATE_FEEDBACK:
                    showFloatButtonRelatedSnackBar(R.string.feedback_sent);
                    break;
            }
        }
    }

    private OnObjectChangedListener<Profile> createProfilePostCountListener() {
        return new OnObjectChangedListener<Profile>() {
            @Override
            public void onObjectChanged(Profile profile) {
                if (profile.getPostCount() == 1) {
                    // show first post popup
                    analytics.logFirstPost();
                    showPopupDialog(R.string.rating_benchmark);
                }
            }
        };
    }

    private void refreshPostList() {
        postsAdapter.loadFirstPage();
        if (postsAdapter.getItemCount() > 0) {
            recyclerView.scrollToPosition(0);
        }
    }

    private void initContentView() {
        if (recyclerView == null) {
//            recordButton = (FloatingActionButton) findViewById(R.id.addNewPostFab);
            recordButton = (Button) findViewById(R.id.addNewPostFab);
            MoveUpwardBehavior fancyBehavior = new MoveUpwardBehavior();
            CoordinatorLayout.LayoutParams params =
                    (CoordinatorLayout.LayoutParams) recordButton.getLayoutParams();
            params.setBehavior(fancyBehavior);
            if (recordButton != null) {
                recordButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (hasInternetConnection()) {
                            addPostClickAction();
                        } else {
                            showFloatButtonRelatedSnackBar(R.string.internet_connection_failed);
                        }
                    }
                });
            }

            newPostsCounterTextView = (TextView) findViewById(R.id.newPostsCounterTextView);
            newPostsCounterTextView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    refreshPostList();
                }
            });

            final ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressBar);
            SwipeRefreshLayout swipeContainer = (SwipeRefreshLayout) findViewById(R.id.swipeContainer);
            recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
            postsAdapter = new PostsAdapter(this, swipeContainer);
            postsAdapter.setCallback(new PostsAdapter.Callback() {
                @Override
                public void onItemClick(final Post post, final View view) {
                    PostManager.getInstance(MainActivity.this).isPostExistSingleValue(post.getId(), new OnObjectExistListener<Post>() {
                        @Override
                        public void onDataChanged(boolean exist) {
                            if (exist) {
                                openPostDetailsActivity(post, view);
                            } else {
                                showFloatButtonRelatedSnackBar(R.string.error_post_was_removed);
                            }
                        }
                    });
                }

                @Override
                public void onListLoadingFinished() {
                    progressBar.setVisibility(View.GONE);
                }

                @Override
                public void onAuthorClick(Post post, View view) {
                    if (post.isAnonymous() && (profile == null || !profile.isAdmin())) {
                        showSnackBar("Post is anonymous");
                        return;
                    }
                    openProfileActivity(post.getAuthorId(), view);
                }

                @Override
                public void onCanceled(String message) {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
                }
            });

            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            ((SimpleItemAnimator) recyclerView.getItemAnimator()).setSupportsChangeAnimations(false);
            recyclerView.setAdapter(postsAdapter);
            postsAdapter.loadFirstPage();
            updateNewPostCounter();

            recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                    hideCounterView();
                    super.onScrolled(recyclerView, dx, dy);
                }
            });
            if (!hasInternetConnection())
                showFloatButtonRelatedSnackBar(R.string.internet_connection_failed);
        }
    }

    private void hideCounterView() {
        if (!counterAnimationInProgress && newPostsCounterTextView.getVisibility() == View.VISIBLE) {
            counterAnimationInProgress = true;
            AlphaAnimation alphaAnimation = AnimationUtils.hideViewByAlpha(newPostsCounterTextView);
            alphaAnimation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {

                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    counterAnimationInProgress = false;
                    newPostsCounterTextView.setVisibility(View.GONE);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });

            alphaAnimation.start();
        }
    }

    private void showCounterView() {
        AnimationUtils.showViewByScaleAndVisibility(newPostsCounterTextView);
    }

    private void openPostDetailsActivity(Post post, View v) {
        Intent intent = new Intent(MainActivity.this, PostDetailsActivity.class);
        intent.putExtra(PostDetailsActivity.POST_ID_EXTRA_KEY, post.getId());

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

            View imageView = v.findViewById(R.id.fileViewContainer);
            View authorImageView = v.findViewById(R.id.authorImageView);

            ActivityOptions options = ActivityOptions.
                    makeSceneTransitionAnimation(MainActivity.this,
                            new android.util.Pair<>(imageView, getString(R.string.post_image_transition_name)),
                            new android.util.Pair<>(authorImageView, getString(R.string.post_author_image_transition_name))
                    );
//            startActivityForResult(intent, PostDetailsActivity.UPDATE_POST_REQUEST, options.toBundle());
            startActivityForResult(intent, PostDetailsActivity.UPDATE_POST_REQUEST);
        } else {
            startActivityForResult(intent, PostDetailsActivity.UPDATE_POST_REQUEST);
        }
    }

    public void showFloatButtonRelatedSnackBar(int messageId) {
        showSnackBar(recordButton, messageId);
    }

    private void addPostClickAction() {
        ProfileStatus profileStatus = profileManager.checkProfile();

        if (profileStatus.equals(ProfileStatus.PROFILE_CREATED)) {
            attemptCreatePostActivity();
        } else {
            doAuthorization(profileStatus);
        }
    }

    private void attemptCreatePostActivity() {
        final int points_post_create = (int) remoteConfig.getLong("points_post_create");
        if (userPoints >= points_post_create) {
            openCreatePostActivity();
            return;
        }
        LogUtil.logInfo(TAG, "load profile points");
        showProgress(R.string.loading_record);
        profileManager.getProfileSingleValue(FirebaseAuth.getInstance().getCurrentUser().getUid(),
                new OnObjectChangedListener<Profile>() {
                    @Override
                    public void onObjectChanged(Profile aProfile) {
                        hideProgress();
                        profile = aProfile;
                        LogUtil.logInfo(TAG, "fetched profile points " + profile.getPoints());
                        userPoints = profile.getPoints();
                        if (userPoints >= points_post_create) {
                            openCreatePostActivity();
                        } else {
                            int pointsNeeded = points_post_create - (int) userPoints;
                            String pointsNeededMsg = getResources().getQuantityString(R.plurals.points_needed_text, pointsNeeded, pointsNeeded);
                            showWarningDialog(pointsNeededMsg);
                        }
                    }
                });
    }

    private void openCreatePostActivity() {
        Intent intent = new Intent(this, CreatePostActivity.class);
        startActivityForResult(intent, CreatePostActivity.CREATE_NEW_POST_REQUEST);
    }

    private void openProfileActivity(String userId) {
        openProfileActivity(userId, null);
    }

    private void openFeedbackActivity() {
        Intent intent = new Intent(MainActivity.this, FeedbackActivity.class);
        startActivityForResult(intent, FeedbackActivity.CREATE_FEEDBACK);
    }

    private void openAdminActivity() {
        Intent intent = new Intent(MainActivity.this, AdminActivity.class);
        startActivityForResult(intent, Constants.ACTIVITY.CREATE_ADMIN);
    }

    private void openRatingsChartActivity() {
        Intent intent = new Intent(MainActivity.this, RatingsChartActivity.class);
        startActivity(intent);
    }

    private void openTnCActivity() {
        Intent intent = new Intent(MainActivity.this, TnCActivity.class);
        startActivityForResult(intent, Constants.ACTIVITY.CREATE_ADMIN);
    }

    private void openProfileActivity(String userId, View view) {
        Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
        intent.putExtra(ProfileActivity.USER_ID_EXTRA_KEY, userId);

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && view != null) {

//            View authorImageView = view.findViewById(R.id.authorImageView);
//
//            ActivityOptions options = ActivityOptions.
//                    makeSceneTransitionAnimation(MainActivity.this,
//                            new android.util.Pair<>(authorImageView, getString(R.string.post_author_image_transition_name)));

//            startActivityForResult(intent, ProfileActivity.CREATE_POST_FROM_PROFILE_REQUEST, options.toBundle());
            startActivityForResult(intent, ProfileActivity.CREATE_POST_FROM_PROFILE_REQUEST);
        } else {
            startActivityForResult(intent, ProfileActivity.CREATE_POST_FROM_PROFILE_REQUEST);
        }
    }

    private void openNotificationActivity(String userId) {
        Intent intent = new Intent(MainActivity.this, NotificationActivity.class);
        intent.putExtra(ProfileActivity.USER_ID_EXTRA_KEY, userId);
        startActivityForResult(intent, ProfileActivity.CREATE_POST_FROM_PROFILE_REQUEST);
    }

    private void updateNewPostCounter() {
        Handler mainHandler = new Handler(this.getMainLooper());
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                int newPostsQuantity = postManager.getNewPostsCounter();

                if (newPostsCounterTextView != null) {
                    if (newPostsQuantity > 0) {
                        showCounterView();

                        String counterFormat = getResources().getQuantityString(R.plurals.new_posts_counter_format, newPostsQuantity, newPostsQuantity);
                        newPostsCounterTextView.setText(String.format(counterFormat, newPostsQuantity));
                    } else {
                        hideCounterView();
                    }
                }
            }
        });
    }

    private void showPopupDialog(int messageId) {
        AlertDialog.Builder builder = new BaseAlertDialogBuilder(this);
        builder.setMessage(getResources().getString(messageId));
        builder.setPositiveButton(R.string.button_ok, null);
        builder.show();
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (profile == null) return true;
        MenuItem itemNotification = menu.findItem(R.id.notification);
        LayerDrawable icon = (LayerDrawable) itemNotification.getIcon();
        setBadgeCount(this, icon, Integer.toString(profile.getUnseen()));
        if (profile.isAdmin()) {
            MenuItem adminItem = menu.findItem(R.id.admin_menu_item);
            adminItem.setVisible(true);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        ProfileStatus profileStatus = profileManager.checkProfile();
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.profile:
                if (profileStatus.equals(ProfileStatus.PROFILE_CREATED)) {
                    String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    openProfileActivity(userId);
                } else {
                    doAuthorization(profileStatus);
                }
                return true;
            case R.id.feedback:
                openFeedbackActivity();
                return true;
            case R.id.ratings_chart_menu_item:
                openRatingsChartActivity();
                return true;
            case R.id.admin_menu_item:
                openAdminActivity();
                return true;
            case R.id.tnc_menu_item:
                openTnCActivity();
                return true;
            case R.id.notification:
                if (profileStatus.equals(ProfileStatus.PROFILE_CREATED)) {
                    String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    openNotificationActivity(userId);
                } else {
                    doAuthorization(profileStatus);
                }
                return true;
            case R.id.share_app: {
                onShareClick();
                return true;
            }
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    public void onShareClick() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String uid = "anonymous";
        if (user != null) {
            uid = user.getUid();
        }
        final String emailSub = getString(R.string.app_share_email_sub);
        //final String linkStr = "http://eriyaz.com/?invitedby=" + uid;
        final String linkStr = "https://play.google.com/store/apps/details?id=com.eriyaz.social&hl=en"+"&invitedby=" + uid;
        //getAnalytics().logShare(uid);
        getAnalytics().logShare();
        final Integer minVersion = 0;
        getDeepLinkUtil().getLink(linkStr, minVersion, new DeepLinkUtil.DynamicLinkCallback() {
            @Override
            public void getLinkSuccess(Uri uri) {
                getDeepLinkUtil().onShare(uri.toString(), emailSub);
            }

            @Override
            public void getShortLinkFailed(String dynamicLinkStr) {
                getDeepLinkUtil().onShare(dynamicLinkStr, emailSub);

            }
        });
    }


    public void updatePost() {
        postsAdapter.updateSelectedPost();
    }

    public void getDynamicLink() {
        //Toast.makeText(getApplicationContext(),"getDynamicLink", Toast.LENGTH_SHORT).show();
        FirebaseDynamicLinks.getInstance().getDynamicLink(getIntent())
                .addOnSuccessListener(this, new OnSuccessListener<PendingDynamicLinkData>() {
                    @Override
                    public void onSuccess(PendingDynamicLinkData pendingDynamicLinkData) {
                        Uri deepLink = null;
                        if (pendingDynamicLinkData != null) {
                            deepLink = pendingDynamicLinkData.getLink();
                            LogUtil.logInfo(TAG,"getDynamicLink.onSuccess :" +deepLink);
                            //Toast.makeText(getApplicationContext(),deepLink.toString(), Toast.LENGTH_SHORT).show();
                        }

                        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                        if (user == null && deepLink != null && deepLink.getBooleanQueryParameter("invitedby", false)) {
                            String referrerUid = deepLink.getQueryParameter("invitedby");
                            getAnalytics().logInvite(referrerUid);
//                            Toast.makeText(getApplicationContext(),referrerUid, Toast.LENGTH_LONG).show();
                            createAnonymousAccountWithReferrerInfo(referrerUid);
                        }
                    }
                })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        LogUtil.logError(TAG, "getDynamicLink:onFailure", e);
                    }
                });
    }

    private void createAnonymousAccountWithReferrerInfo(String referrerUid) {
        DatabaseHelper.getInstance(MainActivity.this).setReferrerInfo(referrerUid);
    }
}
