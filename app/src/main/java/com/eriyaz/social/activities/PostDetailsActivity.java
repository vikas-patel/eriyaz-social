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

import android.Manifest;
        import android.animation.Animator;
        import android.annotation.SuppressLint;
        import android.annotation.TargetApi;
        import android.content.Context;
        import android.content.DialogInterface;
        import android.content.Intent;
        import android.content.pm.PackageManager;
        import android.graphics.Rect;
        import android.net.Uri;
        import android.os.Build;
        import android.os.Bundle;
        import android.os.Handler;
        import android.support.annotation.NonNull;
        import android.support.annotation.Nullable;
        import android.support.v4.app.FragmentTransaction;
        import android.support.v7.app.AlertDialog;
        import android.support.v7.view.ActionMode;
        import android.support.v7.widget.DividerItemDecoration;
        import android.support.v7.widget.LinearLayoutManager;
        import android.support.v7.widget.RecyclerView;
        import android.text.Editable;
        import android.text.Html;
        import android.text.TextUtils;
        import android.text.TextWatcher;
        import android.transition.Transition;
        import android.util.Log;
        import android.view.Menu;
        import android.view.MenuInflater;
        import android.view.MenuItem;
        import android.view.MotionEvent;
        import android.view.View;
        import android.view.ViewPropertyAnimator;
        import android.view.ViewTreeObserver;
        import android.view.inputmethod.InputMethodManager;
        import android.widget.Button;
        import android.widget.EditText;
        import android.widget.ImageButton;
        import android.widget.ImageView;
        import android.widget.ProgressBar;
        import android.widget.ScrollView;
        import android.widget.TextView;
        import android.widget.Toast;

        import com.bumptech.glide.load.engine.DiskCacheStrategy;
        import com.eriyaz.social.Application;
import com.eriyaz.social.ApplicationHelper;
import com.eriyaz.social.Constants;
        import com.eriyaz.social.R;
        import com.eriyaz.social.adapters.CommentsAdapter;
        import com.eriyaz.social.adapters.RatingsAdapter;
        import com.eriyaz.social.adapters.holders.CommentViewHolder;
        import com.eriyaz.social.apprater.AppRater;
        import com.eriyaz.social.apprater.AppRaterCallbackImp;
        import com.eriyaz.social.controllers.LikeController;
        import com.eriyaz.social.dialogs.BlockDialog;
        import com.eriyaz.social.dialogs.ComplainDialog;
        import com.eriyaz.social.dialogs.EditCommentDialog;
        import com.eriyaz.social.enums.BoughtFeedbackStatus;
        import com.eriyaz.social.enums.FeedbackScope;
        import com.eriyaz.social.enums.PaymentStatus;
        import com.eriyaz.social.enums.PostOrigin;
        import com.eriyaz.social.enums.PostStatus;
        import com.eriyaz.social.enums.ProfileStatus;
        import com.eriyaz.social.fragments.MistakesPlayFragment;
        import com.eriyaz.social.fragments.PlaybackFragment;
        import com.eriyaz.social.fragments.RecordPlayFragment;
        import com.eriyaz.social.listeners.CustomTransitionListener;
        import com.eriyaz.social.managers.BoughtFeedbackManager;
        import com.eriyaz.social.managers.CommentManager;
import com.eriyaz.social.managers.DatabaseHelper;
import com.eriyaz.social.managers.LikeManager;
        import com.eriyaz.social.managers.PostManager;
        import com.eriyaz.social.managers.ProfileManager;
        import com.eriyaz.social.managers.listeners.OnDataChangedListener;
        import com.eriyaz.social.managers.listeners.OnObjectChangedListener;
        import com.eriyaz.social.managers.listeners.OnPaymentCompleteListener;
        import com.eriyaz.social.managers.listeners.OnPostChangedListener;
        import com.eriyaz.social.managers.listeners.OnTaskCompleteListener;
        import com.eriyaz.social.managers.requestFeedbackManager;
        import com.eriyaz.social.model.Comment;
        import com.eriyaz.social.model.Flag;
        import com.eriyaz.social.model.Like;
        import com.eriyaz.social.model.Post;
        import com.eriyaz.social.model.Profile;
        import com.eriyaz.social.model.Rating;
        import com.eriyaz.social.model.RecordingItem;
        import com.eriyaz.social.photomovie.RecordShareActivity;
        import com.eriyaz.social.utils.FormatterUtil;
        import com.eriyaz.social.utils.GlideApp;
        import com.eriyaz.social.utils.ImageUtil;
        import com.eriyaz.social.utils.OfficialFeedbackRequest;
        import com.eriyaz.social.utils.PermissionsUtil;
        import com.eriyaz.social.utils.RatingUtil;
        import com.eriyaz.social.views.RecordLayout;
        import com.google.android.exoplayer2.util.Util;
        import com.google.android.gms.tasks.OnCompleteListener;
        import com.google.android.gms.tasks.Task;
        import com.google.firebase.auth.FirebaseAuth;
        import com.google.firebase.auth.FirebaseUser;
        import com.google.firebase.database.DataSnapshot;
        import com.google.firebase.database.DatabaseError;
        import com.google.firebase.database.DatabaseReference;
        import com.google.firebase.database.FirebaseDatabase;
        import com.google.firebase.database.ValueEventListener;
        import com.google.firebase.remoteconfig.FirebaseRemoteConfig;

        import java.io.File;
        import java.io.Serializable;
        import java.util.List;
        import java.util.concurrent.TimeUnit;

public class PostDetailsActivity extends BaseCurrentProfileActivity implements EditCommentDialog.CommentDialogCallback,
        ComplainDialog.ComplainCallback, BlockDialog.BlockCallback {
    private static final String TAG = PostDetailsActivity.class.getSimpleName();

    public static final String POST_ID_EXTRA_KEY = "PostDetailsActivity.POST_ID_EXTRA_KEY";
    public static final String AUTHOR_ANIMATION_NEEDED_EXTRA_KEY = "PostDetailsActivity.AUTHOR_ANIMATION_NEEDED_EXTRA_KEY";
    public static final int TIME_OUT_LOADING_COMMENTS = 30000;
    public static final int UPDATE_POST_REQUEST = 1;
    public static final String POST_STATUS_EXTRA_KEY = "PostDetailsActivity.POST_STATUS_EXTRA_KEY";
    public static final String POST_ORIGIN_EXTRA_KEY = "PostDetailsActivity.POST_ORIGIN_EXTRA_KEY";
    public static final String IS_FEEDBACK_REQUEST_NOTIFICATION = "PostDetailsActivity.IS_FEEDBACK_REQUEST_NOTIFICATION";
    public static final String IS_ADMIN_EXTRA_KEY = "PostDetailsActivity.IS_ADMIN_EXTRA_KEY";
    public static final String IS_COMMENT_NOTIFICATION = "PostDetailsActivity.IS_COMMENT_NOTIFICATION";

    private EditText commentEditText;
    @Nullable
    private Post post;
    private Profile profile;
    private ScrollView scrollView;
    private ImageView ratingsImageView;
    private TextView ratingCounterTextView;
    private TextView averageRatingTextView;
    private TextView ratingLabelTextView;
    private TextView aggregatePercentileTextView;

    private TextView commentsLabel;
    //    private TextView likeCounterTextView;
    private TextView commentsCountTextView;
    private TextView authorTextView;
    private TextView dateTextView;
    private ImageView authorImageView;
    private ProgressBar progressBar;
    private TextView fileName;
    private TextView audioLength;
    private View fileContainerView;
    private TextView descriptionEditText;
    private ProgressBar commentsProgressBar;
    private RecyclerView commentsRecyclerView;
    private TextView warningCommentsTextView;
    private TextView feedbackStatusTextView;

    private TextView ratingsLabel;
    private ProgressBar ratingsProgressBar;
    private RecyclerView ratingsRecyclerView;
    private TextView warningRatingsTextView;
    private boolean attemptToLoadRatings = false;

    private boolean attemptToLoadComments = false;

    private MenuItem complainActionMenuItem;
    private MenuItem editActionMenuItem;
    private MenuItem deleteActionMenuItem;
    private MenuItem publicActionMenuItem;
    private MenuItem removeRatingMenuItem;

    private String postId;
    private boolean isIntentFromNotification;

    private PostManager postManager;
    private CommentManager commentManager;
    private ProfileManager profileManager;
    private BoughtFeedbackManager boughtFeedbackManager;

    //    private LikeController likeController;
//    private RatingController ratingController;
    private boolean postRemovingProcess = false;
    private boolean isPostExist;
    private boolean authorAnimationInProgress = false;

    private boolean isAuthorAnimationRequired;
    private boolean isAdmin;
    private CommentsAdapter commentsAdapter;
    private RatingsAdapter ratingsAdapter;
    private ActionMode mActionMode;
    private boolean isEnterTransitionFinished = false;
    private Rating rating;
    private Button buyFeedbackButton;
    private Button recordShareButton;
    final FirebaseRemoteConfig remoteConfig = FirebaseRemoteConfig.getInstance();
    private int paymentAmount = (int) remoteConfig.getLong("payment_amount");
    private RecordLayout commentRecordLayout;
    private ImageButton mRecordButton;
    private boolean mStartRecording = true;
    private Button sendButton;
    private AppRater appRater;

    requestFeedbackManager f;
    boolean isFeedbackRequest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_details);
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        profileManager = ProfileManager.getInstance(this);
        postManager = PostManager.getInstance(this);
        commentManager = CommentManager.getInstance(this);
        boughtFeedbackManager = BoughtFeedbackManager.getInstance(this);


        isAuthorAnimationRequired = getIntent().getBooleanExtra(AUTHOR_ANIMATION_NEEDED_EXTRA_KEY, false);
        isAdmin = getIntent().getBooleanExtra(IS_ADMIN_EXTRA_KEY, false);
        postId = getIntent().getStringExtra(POST_ID_EXTRA_KEY);
        isFeedbackRequest = getIntent().getBooleanExtra(IS_FEEDBACK_REQUEST_NOTIFICATION,false);
        Log.d("TAG", String.valueOf(isFeedbackRequest));

        fileName = (TextView) findViewById(R.id.file_name_text);
        descriptionEditText = findViewById(R.id.descriptionEditText);
        audioLength = (TextView) findViewById(R.id.file_length_text);
        fileContainerView = findViewById(R.id.fileViewContainer);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        commentsRecyclerView = (RecyclerView) findViewById(R.id.commentsRecyclerView);
        scrollView = (ScrollView) findViewById(R.id.scrollView);
        commentsLabel = (TextView) findViewById(R.id.commentsLabel);
        commentEditText = (EditText) findViewById(R.id.commentEditText);
        feedbackStatusTextView = findViewById(R.id.feedbackScopeTextView);

        ratingsRecyclerView = (RecyclerView) findViewById(R.id.ratingsRecyclerView);
        ratingsLabel = (TextView) findViewById(R.id.ratingsLabel);
        ratingsProgressBar = (ProgressBar) findViewById(R.id.ratingsProgressBar);
        warningRatingsTextView = (TextView) findViewById(R.id.warningRatingsTextView);

//        likesContainer = (ViewGroup) findViewById(R.id.likesContainer);
//        likesImageView = (ImageView) findViewById(R.id.likesImageView);
        ratingsImageView = (ImageView) findViewById(R.id.ratingImageView);
        ratingCounterTextView = (TextView) findViewById(R.id.ratingCounterTextView);
        averageRatingTextView = (TextView) findViewById(R.id.averageRatingTextView);
        ratingLabelTextView = findViewById(R.id.ratingLabelTextView);
        aggregatePercentileTextView = findViewById(R.id.aggregatePercentileTextView);
//        ratingBar = (BubbleSeekBar) findViewById(R.id.ratingBar);

        authorImageView = (ImageView) findViewById(R.id.authorImageView);
        authorTextView = (TextView) findViewById(R.id.authorTextView);
//        likeCounterTextView = (TextView) findViewById(R.id.likeCounterTextView);
        commentsCountTextView = (TextView) findViewById(R.id.commentsCountTextView);
        dateTextView = (TextView) findViewById(R.id.dateTextView);
        commentsProgressBar = (ProgressBar) findViewById(R.id.commentsProgressBar);
        warningCommentsTextView = (TextView) findViewById(R.id.warningCommentsTextView);
        commentRecordLayout = findViewById(R.id.recordLayout);
        mRecordButton = findViewById(R.id.recordButton);
        mRecordButton.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("NewApi")
            @Override
            public void onClick(View v) {
                onRecord();
            }
        });

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && isAuthorAnimationRequired) {
            authorImageView.setScaleX(0);
            authorImageView.setScaleY(0);

            // Add a listener to get noticed when the transition ends to animate the fab button
            getWindow().getSharedElementEnterTransition().addListener(new CustomTransitionListener() {
                @TargetApi(Build.VERSION_CODES.LOLLIPOP)
                @Override
                public void onTransitionEnd(Transition transition) {
                    super.onTransitionEnd(transition);
                    //disable execution for exit transition
                    if (!isEnterTransitionFinished) {
                        isEnterTransitionFinished = true;
                        com.eriyaz.social.utils.AnimationUtils.showViewByScale(authorImageView)
                                .setListener(authorAnimatorListener)
                                .start();
                    }
                }
            });
        }

        sendButton = (Button) findViewById(R.id.sendButton);

        postManager.getPost(this, postId, createOnPostChangeListener());


//        postImageView.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                LogUtil.logInfo("PostDetailsActivity", "Image Clicked.");
//                openImageDetailScreen();
//            }
//        });

        fileContainerView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    RecordingItem item = new RecordingItem();
                    item.setName(post.getTitle());
                    item.setLength(post.getAudioDuration());
                    item.setFilePath(post.getImagePath());
                    PlaybackFragment playbackFragment =
                            new PlaybackFragment().newInstance(item, post, rating, profile.getUsername(), isFeedbackRequest);
                    FragmentTransaction transaction = getSupportFragmentManager()
                            .beginTransaction();
                    playbackFragment.show(transaction, "dialog_playback");
                } catch (Exception e) {
                    Log.e("", "exception", e);
                }
            }
        });

        commentEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (charSequence.toString().trim().length() > 0) {
                    sendButton.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isAuthorized()) return;
                sendComment();
            }
        });

        commentsCountTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                scrollToFirstComment();
            }
        });

        View.OnClickListener onAuthorClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (post != null) {
                    if (post.isAnonymous()) {
                        showSnackBar("Post is anonymous");
                    } else {
                        openProfileActivity(post.getAuthorId(), v);
                    }
                }
            }
        };

        authorImageView.setOnClickListener(onAuthorClickListener);

        authorTextView.setOnClickListener(onAuthorClickListener);

        Serializable serializable = getIntent().getSerializableExtra(POST_ORIGIN_EXTRA_KEY);
        if (serializable != null && serializable instanceof PostOrigin) {
            PostOrigin origin = (PostOrigin) serializable;
            if (origin.equals(PostOrigin.PUSH_NOTIFICATION)) {
                analytics.logOpenPostDetailsFromPushNotification();
            } else if (origin.equals(PostOrigin.APP_NOTIFICATION)) {
                analytics.logOpenPostDetailsFromAppNotification();
            }
        }
        supportPostponeEnterTransition();


        buyFeedbackButton = findViewById(R.id.buy_feedback_button);

        recordShareButton = findViewById(R.id.record_share_button);
        recordShareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestRecordShareActivity();
            }
        });
        appRater = new AppRater(this);
        appRater.setAppRaterCallback(new AppRaterCallbackImp(PostDetailsActivity.this));




    }


    public boolean isAuthorized() {
        if (!hasInternetConnection()) {
            showSnackBar(R.string.internet_connection_failed);
            return false;
        }
        ProfileStatus status = ProfileManager.getInstance(PostDetailsActivity.this).checkProfile();
        if (status.equals(ProfileStatus.NOT_AUTHORIZED) || status.equals(ProfileStatus.NO_PROFILE)) {
            doAuthorization(status);
            return false;
        }
        Application application = (Application) getApplication();
        if (application.isBlocked(post.getAuthorId())) {
            showWarningDialog(String.format(getResources().getString(R.string.blocked_msg), "comment"));
            return false;
        }
        return true;
    }

    private void createOfficialFeedbackRequest() {
        OfficialFeedbackRequest request = new OfficialFeedbackRequest(post.getAuthorId(), post.getId(), paymentAmount, PostDetailsActivity.this);
        request.create(new OnPaymentCompleteListener() {
            @Override
            public void onTaskComplete(PaymentStatus status) {
                if (isActivityDestroyed()) return;
                if (status.equals(PaymentStatus.SUCCESS)) {
                    showSnackBar("Payment Successful.");
                } else if (status.equals(PaymentStatus.PENDING)) {
                    showDialog("Payment Status Pending");
                } else {
                    showDialog("Payment Failed");
                }
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        if (Util.SDK_INT > 23) {
            commentRecordLayout.initializePlayer();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if ((Util.SDK_INT <= 23 || commentRecordLayout.getPlayer() == null)) {
            commentRecordLayout.initializePlayer();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (Util.SDK_INT <= 23) {
            commentRecordLayout.releasePlayer();
            if (!mStartRecording) stopRecording();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (Util.SDK_INT > 23) {
            commentRecordLayout.releasePlayer();
            if (!mStartRecording) stopRecording();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        postManager.closeListeners(this);
        commentRecordLayout.deleteCommentAudioFile();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            View v = getCurrentFocus();
            if (v instanceof EditText) {
                Rect outRect = new Rect();
                v.getGlobalVisibleRect(outRect);
                if (!outRect.contains((int) event.getRawX(), (int) event.getRawY())) {
                    v.clearFocus();
                    hideKeyBoard();
                }
            }
        }
        return super.dispatchTouchEvent(event);
    }

    @Override
    public void onBackPressed() {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && isAuthorAnimationRequired) {
            if (!authorAnimationInProgress) {
                ViewPropertyAnimator hideAuthorAnimator = com.eriyaz.social.utils.AnimationUtils.hideViewByScale(authorImageView);
                hideAuthorAnimator.setListener(authorAnimatorListener);
                hideAuthorAnimator.withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        PostDetailsActivity.super.onBackPressed();
                    }
                });
            }

        } else {
            super.onBackPressed();
        }
    }

    private void initCommentRecyclerView() {
        commentsAdapter = new CommentsAdapter(post, isAdmin);
        commentsAdapter.setCallback(new CommentsAdapter.Callback() {
            @Override
            public void onDeleteClick(View view, int position) {
                Comment comment = commentsAdapter.getItemByPosition(position);
                removeComment(comment);
            }

            @Override
            public void onLikeClick(LikeController likeController, int position) {
                Comment comment = commentsAdapter.getItemByPosition(position);
                likeController.handleLikeClickAction(PostDetailsActivity.this, comment);
                appRater.checkToShowRatingOnEvent();
            }

            @Override
            public void onEditClick(View view, int position) {
                Comment comment = commentsAdapter.getItemByPosition(position);
                openEditCommentDialog(comment);
            }

            @Override
            public void onRewardClick(View view, int position, int points) {
                Comment comment = commentsAdapter.getItemByPosition(position);
                comment.setReputationPoints(points);
                updateComment(comment, points);
            }

            @Override
            public void onUserRewardClick(View view, int position, int points) {
                Comment comment = commentsAdapter.getItemByPosition(position);
                comment.setUserRewardPoints(points);
                updateComment(comment, points);
            }

            @Override
            public void onPlayClick(View view, int position, String authorName) {
                Comment comment = commentsAdapter.getItemByPosition(position);
                try {
                    RecordingItem item = new RecordingItem(authorName + "'s voice comment", comment.getAudioPath(), 0);
                    item.setServer(true);
                    RecordPlayFragment playbackFragment =
                            RecordPlayFragment.newInstance(item);
                    FragmentTransaction transaction = getSupportFragmentManager()
                            .beginTransaction();
                    playbackFragment.show(transaction, "dialog_playback");
                } catch (Exception e) {
                    Log.e(TAG, "exception", e);
                }
            }

            @Override
            public void onBlockClick(View view, int position) {
                Comment comment = commentsAdapter.getItemByPosition(position);
                if (!hasInternetConnection()) {
                    showSnackBar(R.string.internet_connection_failed);
                    return;
                }
                String blockUser = comment.getAuthorId();
                openUserBlockDialog(blockUser);
            }

            @Override
            public void onReportClick(View view, int position) {
                Comment comment = commentsAdapter.getItemByPosition(position);
                if (!hasInternetConnection()) {
                    showSnackBar(R.string.internet_connection_failed);
                    return;
                }
                ProfileStatus profileStatus = profileManager.checkProfile();
                if (profileStatus.equals(ProfileStatus.PROFILE_CREATED)) {
                    Flag flag = new Flag(postId, "", comment.getId(), "",
                            comment.getAuthorId(), FirebaseAuth.getInstance().getCurrentUser().getUid());
                    openUserComplainDialog(flag);
                } else {
                    doAuthorization(profileStatus);
                }
            }

            @Override
            public void onAuthorClick(String authorId, View view) {
                openProfileActivity(authorId, view);
            }

            @Override
            public void onLikeUserListClick(int position) {
                Comment comment = commentsAdapter.getItemByPosition(position);
                openLikeUserListActivity(comment.getId());
            }

            @Override
            public void onTimeStampClick(String comment, String timestamp) {
                try {
                    RecordingItem item = new RecordingItem();
                    item.setName(post.getTitle());
                    item.setLength(post.getAudioDuration());
                    item.setFilePath(post.getImagePath());

                    MistakesPlayFragment mistakesPlayFragment =
                            new MistakesPlayFragment().newInstance(item, post, rating, comment, timestamp);
                    FragmentTransaction transaction = getSupportFragmentManager()
                            .beginTransaction();
                    mistakesPlayFragment.show(transaction, "dialog_playback");
                } catch (Exception e) {
                    Log.e("", "exception", e);
                }
            }
        });
        commentsRecyclerView.setAdapter(commentsAdapter);
        commentsRecyclerView.setNestedScrollingEnabled(false);
        commentsRecyclerView.addItemDecoration(new DividerItemDecoration(commentsRecyclerView.getContext(),
                ((LinearLayoutManager) commentsRecyclerView.getLayoutManager()).getOrientation()));

        commentManager.getCommentsList(this, postId, createOnCommentsChangedDataListener());
    }

    private void initRatingRecyclerView() {
        ratingsAdapter = new RatingsAdapter(post);
        ratingsAdapter.setCallback(new RatingsAdapter.Callback() {

            @Override
            public void onReportClick(View view, int position) {
                Rating rating = ratingsAdapter.getItemByPosition(position);
                if (!hasInternetConnection()) {
                    showSnackBar(R.string.internet_connection_failed);
                    return;
                }
                ProfileStatus profileStatus = profileManager.checkProfile();
                if (profileStatus.equals(ProfileStatus.PROFILE_CREATED)) {
                    Flag flag = new Flag(postId, rating.getId(), "", "",
                            rating.getAuthorId(), FirebaseAuth.getInstance().getCurrentUser().getUid());
                    openUserComplainDialog(flag);
                } else {
                    doAuthorization(profileStatus);
                }
            }

            @Override
            public void onBlockClick(View view, int position) {
                Rating rating = ratingsAdapter.getItemByPosition(position);
                if (!hasInternetConnection()) {
                    showSnackBar(R.string.internet_connection_failed);
                    return;
                }
                String blockUser = rating.getAuthorId();
                openUserBlockDialog(blockUser);
            }

            @Override
            public void onRemoveRatingClick(View v, int position) {
                Rating rating = ratingsAdapter.getItemByPosition(position);
                if(!hasInternetConnection()){
                    showSnackBar(R.string.internet_connection_failed);
                    return;
                }

                DatabaseHelper databaseHelper = ApplicationHelper.getDatabaseHelper();
                databaseHelper.hideRating(postId, rating, 0.0f);
                //ratings hidden so set to 1.
                rating.setRating(0.0f);
                rating.setRatingRemoved(true);
                ratingsAdapter.notifyItemChanged(position);
            }

            @Override
            public void onAuthorClick(String authorId, View view) {
                openProfileActivity(authorId, view);
            }

            @Override
            public void onReplyClick(int position) {
            }

            @Override
            public void onRequestFeedbackClick(int position) {
                Rating selectedRating = ratingsAdapter.getItemByPosition(position);
                f = new requestFeedbackManager(
                        PostDetailsActivity.this,
                        getCurrentUserId(),
                        selectedRating.getAuthorId(),
                        post.getImageTitle().substring(5),
                        currentProfile.getUsername(),
                        currentProfile.getPoints());
            }

            @Override
            public void makeRatingVisible(int position) {
                Rating selectedRating = ratingsAdapter.getItemByPosition(position);
                // check if sufficient points
                if (profile != null) {
                    if (profile.getPoints() > 0) {
                        // decrement user points
                        profile.setPoints(profile.getPoints()-1);
                        profileManager.decrementUserPoints(profile.getId());
                        showToastPointLost();
                    } else {
                        showPointsNeededDialog();
                        return;
                    }
                }
                //mark rating viewed
                selectedRating.setViewedByPostAuthor(true);
                ratingsAdapter.notifyItemChanged(position);
                profileManager.markRatingViewed(post.getId(), selectedRating);
            }
        });
        ratingsRecyclerView.setAdapter(ratingsAdapter);
        ratingsRecyclerView.setNestedScrollingEnabled(false);
        ratingsRecyclerView.addItemDecoration(new DividerItemDecoration(ratingsRecyclerView.getContext(),
                ((LinearLayoutManager) ratingsRecyclerView.getLayoutManager()).getOrientation()));

        postManager.getRatingsList(this, postId, createOnRatingsChangedDataListener());
    }

    private void showToastPointLost() {
        Toast toast = Toast.makeText(getApplicationContext(), R.string.point_spent_view_rating, Toast.LENGTH_LONG);
        toast.getView().setBackgroundColor(getResources().getColor(R.color.red));
        TextView text = toast.getView().findViewById(android.R.id.message);
        text.setTextColor(getResources().getColor(R.color.icons));
        toast.show();
    }

    private void showDialog(String msg) {
        AlertDialog.Builder builder = new BaseAlertDialogBuilder(this);
        builder.setMessage(msg);
        builder.setPositiveButton(R.string.button_ok, null);
        builder.show();
    }

    private void showPointsNeededDialog() {
        AlertDialog.Builder builder = new BaseAlertDialogBuilder(this);
        builder.setMessage(String.format(getResources().getString(R.string.insufficient_points_view_rating), profile.getUsername()));
        builder.setPositiveButton(R.string.button_ok, null);
        builder.show();
    }

//    private void startActionMode(Comment selectedComment) {
//        if (mActionMode != null) {
//            return;
//        }
//
//        //check access to modify or remove post
//        if (hasAccessToEditComment(selectedComment.getAuthorId()) || hasAccessToModifyPost()) {
//            mActionMode = startSupportActionMode(new ActionModeCallback(selectedComment));
//        }
//    }

    private OnPostChangedListener createOnPostChangeListener() {
        return new OnPostChangedListener() {
            @Override
            public void onObjectChanged(Post obj) {
                if (obj != null) {
                    post = obj;
                    afterPostLoaded();
                } else if (!postRemovingProcess) {
                    isPostExist = false;
                    Intent intent = getIntent();
                    setResult(RESULT_OK, intent.putExtra(POST_STATUS_EXTRA_KEY, PostStatus.REMOVED));
                    showPostWasRemovedDialog();
                }
            }

            @Override
            public void onError(String errorText) {
                AlertDialog.Builder builder = new BaseAlertDialogBuilder(PostDetailsActivity.this);
                builder.setMessage(errorText);
                builder.setPositiveButton(R.string.button_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                });
                builder.setCancelable(false);
                builder.show();
            }
        };
    }

    private void afterPostLoaded() {

        isIntentFromNotification = getIntent().getBooleanExtra(PostDetailsActivity.IS_COMMENT_NOTIFICATION, false);

        //invalidateOptionsMenu();//it will call onCreateContextMenu again so that we can hide editPost option if user is seeing others post

        isPostExist = true;
        initRatingRecyclerView();
        initCommentRecyclerView();
        initLikes();
        fillPostFields();
        setBoughtFeedbackStatus();
        updateCounters();
        initLikeButtonState();
        invalidateOptionsMenu();
        progressBar.setVisibility(View.GONE);

        if(isIntentFromNotification) {
            new Handler().postDelayed(this::scrollToFirstComment,1000);
        }
    }

    private void showPostWasRemovedDialog() {
        AlertDialog.Builder builder = new BaseAlertDialogBuilder(PostDetailsActivity.this);
        builder.setMessage(R.string.error_post_was_removed);
        builder.setPositiveButton(R.string.button_ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }
        });
        builder.setCancelable(false);
        builder.show();
    }

    private void scrollToFirstComment() {
        if (post != null && post.getCommentsCount() > 0) {
            scrollView.smoothScrollTo(0, commentsLabel.getTop());
        }
    }

    private void fillPostFields() {
        if (post != null) {
            if (TextUtils.isEmpty(post.getDescription())) {
                descriptionEditText.setVisibility(View.GONE);
            } else {
                descriptionEditText.setText(post.getDescription());
            }
            fileName.setText(post.getTitle());
            long minutes = TimeUnit.MILLISECONDS.toMinutes(post.getAudioDuration());
            long seconds = TimeUnit.MILLISECONDS.toSeconds(post.getAudioDuration())
                    - TimeUnit.MINUTES.toSeconds(minutes);
            audioLength.setText(String.format("%02d:%02d", minutes, seconds));
            if (post.getFeedbackScope() != null) {
                feedbackStatusTextView.setVisibility(View.VISIBLE);
                if (post.getFeedbackScope().equals(FeedbackScope.EXPERT)) {
                    feedbackStatusTextView.setText(R.string.feedback_scope_expert);
                    feedbackStatusTextView.setTextColor(getResources().getColor(R.color.red));
                }
            }
            loadAuthorImage();
        }
    }

    private void setBoughtFeedbackStatus() {
        if (post.getBoughtFeedbackStatus() == BoughtFeedbackStatus.ASKED) {
            buyFeedbackButton.setText(getString(R.string.awaiting_official_feedback));
            buyFeedbackButton.setEnabled(false);
            buyFeedbackButton.setVisibility(View.VISIBLE);
            return;
        } else if (post.getBoughtFeedbackStatus() == BoughtFeedbackStatus.GIVEN) {
            buyFeedbackButton.setText(getString(R.string.received_official_feedback));
            buyFeedbackButton.setEnabled(false);
            buyFeedbackButton.setVisibility(View.VISIBLE);
            return;
        } else if (post.getBoughtFeedbackStatus() == BoughtFeedbackStatus.PAYMENT_STATUS_PENDING) {
            buyFeedbackButton.setText(getString(R.string.payment_status_pending));
            buyFeedbackButton.setEnabled(false);
            buyFeedbackButton.setVisibility(View.VISIBLE);
            return;
        }
//        if (hasAccessToModifyPost()) {
//            buyFeedbackButton.setVisibility(View.VISIBLE);
//        }
    }

//    private void loadPostDetailsImage() {
//        if (post == null) {
//            return;
//        }
//
//        String imageUrl = post.getImagePath();
//        int width = Utils.getDisplayWidth(this);
//        int height = (int) getResources().getDimension(R.dimen.post_detail_image_height);
//        Glide.with(this)
//                .load(imageUrl)
//                .centerCrop()
//                .override(width, height)
//                .diskCacheStrategy(DiskCacheStrategy.ALL)
//                .error(R.drawable.ic_stub)
//                .listener(new RequestListener<String, GlideDrawable>() {
//                    @Override
//                    public boolean onException(Exception e, String model, Target<GlideDrawable> target, boolean isFirstResource) {
//                        scheduleStartPostponedTransition(postImageView);
//                        return false;
//                    }
//
//                    @Override
//                    public boolean onResourceReady(GlideDrawable resource, String model, Target<GlideDrawable> target, boolean isFromMemoryCache, boolean isFirstResource) {
//                        scheduleStartPostponedTransition(postImageView);
//                        progressBar.setVisibility(View.GONE);
//                        return false;
//                    }
//                })
//                .crossFade()
//                .into(postImageView);
//    }

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

    private void loadAuthorImage() {
        if (post != null && post.getAuthorId() != null) {
            profileManager.getProfileSingleValue(post.getAuthorId(), createProfileChangeListener());
        }
    }

    private void updateCounters() {
        if (post == null) {
            return;
        }

        long commentsCount = post.getCommentsCount();
        commentsCountTextView.setText(String.valueOf(commentsCount));
        commentsLabel.setText(String.format(getString(R.string.label_comments), commentsCount));
//        likeCounterTextView.setText(String.valueOf(post.getLikesCount()));
        ratingCounterTextView.setText("(" + post.getRatingsCount() + ")");
        if (hasAccessToModifyPost()) {
            String avgRatingText = "";
            if (post.getAverageRating() > 0) {
                avgRatingText = String.format( "%.1f", post.getAverageRating());
            }
            averageRatingTextView.setVisibility(View.VISIBLE);
            aggregatePercentileTextView.setVisibility(View.VISIBLE);
            ratingLabelTextView.setVisibility(View.VISIBLE);
            averageRatingTextView.setText(avgRatingText);
            String percentileStr = RatingUtil.getRatingPercentile(Math.round(post.getAverageRating()));
            aggregatePercentileTextView.setText(Html.fromHtml(String.format(getString(R.string.aggregate_post_percentile), percentileStr)));
            aggregatePercentileTextView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // open rating chart
                    Intent intent = new Intent(PostDetailsActivity.this, RatingsChartActivity.class);
                    startActivity(intent);
                }
            });
            if (post.getAverageRating() > 15) {
                ratingLabelTextView.setText("AMAZING");
                ratingLabelTextView.setTextColor(getResources().getColor(R.color.dark_green));
            } else if (post.getAverageRating() > 10) {
                ratingLabelTextView.setText("GOOD");
                ratingLabelTextView.setTextColor(getResources().getColor(R.color.light_green));
            } else if (post.getAverageRating() > 5) {
                ratingLabelTextView.setText("AVERAGE");
                ratingLabelTextView.setTextColor(getResources().getColor(R.color.accent));
            } else if (post.getAverageRating() > 0){
                ratingLabelTextView.setText("NOT OK");
                ratingLabelTextView.setTextColor(getResources().getColor(R.color.red));
            } else {
                ratingLabelTextView.setText("");
            }
        }

//        likeController.setUpdatingLikeCounter(false);
//        ratingController.setUpdatingRatingCounter(false);

        CharSequence date = FormatterUtil.getRelativeTimeSpanStringShort(this, post.getCreatedDate());
        dateTextView.setText(date);

        if (commentsCount == 0) {
            commentsLabel.setVisibility(View.GONE);
            commentsProgressBar.setVisibility(View.GONE);
        } else if (commentsLabel.getVisibility() != View.VISIBLE) {
            commentsLabel.setVisibility(View.VISIBLE);
        }

        ratingsLabel.setText(String.format(getString(R.string.label_ratings), post.getRatingsCount()));

        if (post.getRatingsCount() == 0 ) {
            ratingsLabel.setVisibility(View.GONE);
            ratingsProgressBar.setVisibility(View.GONE);
            } else if (ratingsLabel.getVisibility() != View.VISIBLE) {
                ratingsLabel.setVisibility(View.VISIBLE);
        }

    }

    private OnObjectChangedListener<Profile> createProfileChangeListener() {
        return new OnObjectChangedListener<Profile>() {
            @Override
            public void onObjectChanged(Profile obj) {
                if (isActivityDestroyed()) return;
                profile = obj;
                // enable share recording button
                recordShareButton.setEnabled(true);
                invalidateOptionsMenu();
                if (post.isAnonymous()) {
                    showProfileDetails(post.getNickName(), post.getAvatarImageUrl());
                } else {
                    showProfileDetails(profile.getUsername(), profile.getPhotoUrl());
                }
            }
        };
    }

    private void showProfileDetails(String userName, String profileImageUrl) {
        if (profileImageUrl != null) {
            ImageUtil.loadImage(GlideApp.with(PostDetailsActivity.this), profileImageUrl, authorImageView, DiskCacheStrategy.DATA);
        } else if (userName != null && !userName.isEmpty()){
            authorImageView.setImageDrawable(ImageUtil.getTextDrawable(userName,
                    getResources().getDimensionPixelSize(R.dimen.post_author_image_side),
                    getResources().getDimensionPixelSize(R.dimen.post_author_image_side)));
        }
        authorTextView.setText(userName);
    }

    private OnDataChangedListener<Rating> createOnRatingsChangedDataListener() {
        attemptToLoadRatings = true;

        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (attemptToLoadRatings) {
                    ratingsProgressBar.setVisibility(View.GONE);
                    warningRatingsTextView.setVisibility(View.VISIBLE);
                }
            }
        }, TIME_OUT_LOADING_COMMENTS);


        return new OnDataChangedListener<Rating>() {
            @Override
            public void onListChanged(List<Rating> list) {
                attemptToLoadRatings = false;
                ratingsProgressBar.setVisibility(View.GONE);
                ratingsRecyclerView.setVisibility(View.VISIBLE);
                warningRatingsTextView.setVisibility(View.GONE);
                ratingsAdapter.setList(list);
            }
        };
    }

    private OnDataChangedListener<Comment> createOnCommentsChangedDataListener() {
        attemptToLoadComments = true;

        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (attemptToLoadComments) {
                    commentsProgressBar.setVisibility(View.GONE);
                    warningCommentsTextView.setVisibility(View.VISIBLE);
                }
            }
        }, TIME_OUT_LOADING_COMMENTS);


        return new OnDataChangedListener<Comment>() {
            @Override
            public void onListChanged(List<Comment> list) {
                attemptToLoadComments = false;
                commentsProgressBar.setVisibility(View.GONE);
                commentsRecyclerView.setVisibility(View.VISIBLE);
                warningCommentsTextView.setVisibility(View.GONE);
                commentsAdapter.setList(list);
                initCommentLikeButtonState();
            }
        };
    }

    private OnDataChangedListener<Like> createOnCommentLikeChangedDataListener() {

        return new OnDataChangedListener<Like>() {
            @Override
            public void onListChanged(List<Like> list) {
                for (Like like:list) {
                    CommentViewHolder viewHolder = (CommentViewHolder) commentsRecyclerView.findViewHolderForItemId(like.getId().hashCode());
                    if (viewHolder != null)viewHolder.initLike(true);
                }
            }
        };
    }

    private void openImageDetailScreen() {
        if (post != null) {
            Intent intent = new Intent(this, ImageDetailActivity.class);
            intent.putExtra(ImageDetailActivity.IMAGE_URL_EXTRA_KEY, post.getImagePath());
            startActivity(intent);
        }
    }

    private void openLikeUserListActivity(String commentId) {
        Intent intent = new Intent(PostDetailsActivity.this, UsersListActivity.class);
        intent.putExtra(UsersListActivity.COMMENT_ID_EXTRA_KEY, commentId);
        startActivity(intent);
    }

    private void openProfileActivity(String userId, View view) {
        Intent intent = new Intent(PostDetailsActivity.this, ProfileActivity.class);
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

//    private OnObjectExistListener<Like> createOnLikeObjectExistListener() {
//        return new OnObjectExistListener<Like>() {
//            @Override
//            public void onDataChanged(boolean exist) {
//                likeController.initLike(exist);
//            }
//        };
//    }

    private OnObjectChangedListener<Rating> createOnRatingObjectChangedListener() {
        return new OnObjectChangedListener<Rating>() {
            @Override
            public void onObjectChanged(Rating obj) {
//                ratingController.initRating(obj);
                if (obj != null && obj.getRating() > 0) {
                    rating = obj;
                    ratingsImageView.setImageResource(R.drawable.ic_star_active);
                } else {
                    rating = new Rating();
                    ratingsImageView.setImageResource(R.drawable.ic_star);
                }
            }
        };
    }

    private void initLikeButtonState() {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser != null && post != null) {
//            postManager.hasCurrentUserLike(this, post.getId(), firebaseUser.getUid(), createOnLikeObjectExistListener());
            postManager.getCurrentUserRating(this, post.getId(), firebaseUser.getUid(), createOnRatingObjectChangedListener());
        }
    }

    private void initCommentLikeButtonState() {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser != null && post != null) {
//            postManager.hasCurrentUserLike(this, post.getId(), firebaseUser.getUid(), createOnLikeObjectExistListener());
            LikeManager.getInstance(this).getCurrentUserCommentLikeListSingleValue(post.getId(), firebaseUser.getUid(), createOnCommentLikeChangedDataListener());
        }
    }

    private void initLikes() {
//        likeController = new LikeController(this, post, likeCounterTextView, likesImageView, false);
//        ratingController = new RatingController(post.getId(), ratingCounterTextView, averageRatingTextView, ratingBar, false);
        //if ratingByCurrentUser value is changed.
//        ratingBar.setOnRatingBarChangeListener(new RatingBar.OnRatingBarChangeListener() {
//            @Override
//            public void onRatingChanged(RatingBar ratingBar, float ratingByCurrentUser, boolean fromUser) {
//                if (isPostExist && fromUser) {
//                    ratingController.handleRatingClickAction(PostDetailsActivity.this, post, ratingByCurrentUser);
//                }
//            }
//        });
        // customize section texts
//        ratingBar.setCustomSectionTextArray(new BubbleSeekBar.CustomSectionTextArray() {
//            @NonNull
//            @Override
//            public SparseArray<String> onCustomize(int sectionCount, @NonNull SparseArray<String> array) {
//                array.clear();
//                array.put(1, "not ok");
//                array.put(3, "ok");
//                array.put(5, "good");
//                array.put(7, "amazing");
//                return array;
//            }
//        });
//        ratingBar.setOnProgressChangedListener(new BubbleSeekBar.OnProgressChangedListenerAdapter() {
//            @Override
//            public void onProgressChanged(BubbleSeekBar bubbleSeekBar, int progress, float progressFloat) {
//                int color = RatingUtil.getRatingColor(PostDetailsActivity.this, progress);
//                bubbleSeekBar.setSecondTrackColor(color);
//                bubbleSeekBar.setThumbColor(color);
//                bubbleSeekBar.setBubbleColor(color);
//            }

//            @Override
//            public void getProgressOnActionUp(BubbleSeekBar bubbleSeekBar, int progress, float progressFloat) {
//                if (isPostExist) {
//                    if (RatingUtil.viewedByAuthor(post)) {
//                        showRatingSelfRecordingDialog();
//                        ratingBar.setProgress(ratingController.getRating().getRating());
//                        return;
//                    }
//                    if (progress > 0 && progress <= 5) {
//                        openCommentDialog();
//                        return;
//                    }
//                    ratingController.handleRatingClickAction(PostDetailsActivity.this, post, progress);
//                }
//            }
//        });


//        likesContainer.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                if (isPostExist) {
//                    likeController.handleLikeClickAction(PostDetailsActivity.this, post);
//                }
//            }
//        });
//
//        //long click for changing animation
//        likesContainer.setOnLongClickListener(new View.OnLongClickListener() {
//            @Override
//            public boolean onLongClick(View v) {
//                if (likeController.getLikeAnimationType() == LikeController.AnimationType.BOUNCE_ANIM) {
//                    likeController.setLikeAnimationType(LikeController.AnimationType.COLOR_ANIM);
//                } else {
//                    likeController.setLikeAnimationType(LikeController.AnimationType.BOUNCE_ANIM);
//                }
//
//                Snackbar snackbar = Snackbar
//                        .make(likesContainer, "Animation was changed", Snackbar.LENGTH_LONG);
//
//                snackbar.show();
//                return true;
//            }
//        });
    }



//    private void openCommentDialog() {
//        CommentDialog commentDialog = new CommentDialog();
//        Bundle args = new Bundle();
//        args.putString(PostDetailsActivity.POST_ID_EXTRA_KEY, post.getId());
//        commentDialog.setArguments(args);
//        commentDialog.show(getFragmentManager(), CommentDialog.TAG);
//    }

//    public void onCommentDialogResult(int requestCode, int resultCode, Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//        if (resultCode == Activity.RESULT_OK) {
//            ratingController.handleRatingClickAction(PostDetailsActivity.this, post, ratingBar.getProgress());
//        } else {
//            ratingBar.setProgress(ratingController.getRating().getRating());
//        }
//    }

    private void sendComment() {
        if (post == null || !isPostExist) {
            return;
        }

        String commentText = commentEditText.getText().toString();
        if (commentText.length() == 0 && commentRecordLayout.getRecordItem() == null) return;
        OnTaskCompleteListener listener = new OnTaskCompleteListener() {
            @Override
            public void onTaskComplete(boolean success) {
                hideProgress();
                if (success) {
                    scrollToFirstComment();
                    commentRecordLayout.reset();
                    sendButton.setVisibility(View.GONE);
                }
            }
        };
        showProgress(R.string.message_submit_comment);
        if (commentRecordLayout.getRecordItem() != null) {
            Uri audioUri = Uri.fromFile(new File(commentRecordLayout.getRecordItem().getFilePath()));
            Comment comment = new Comment(commentText);
            String authorId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            comment.setAuthorId(authorId);
            commentManager.createOrUpdateCommentWithAudio(audioUri, comment, post.getId(), listener);
        } else {
            commentManager.createComment(commentText, post.getId(), listener);
        }
        commentEditText.setText(null);
        commentEditText.clearFocus();
        hideKeyBoard();
    }

    private void hideKeyBoard() {
        // Check if no view has focus:
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private boolean hasAccessToModifyPost() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        return currentUser != null && post != null && post.getAuthorId().equals(currentUser.getUid());
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (profile == null || post == null) return true;
        if (deleteActionMenuItem != null && hasAccessToModifyPost()) {
           editActionMenuItem.setVisible(true);
            deleteActionMenuItem.setVisible(true);
        }

        if (publicActionMenuItem != null && post.isAnonymous() && hasAccessToModifyPost()) {
            publicActionMenuItem.setVisible(true);
        }

        if (complainActionMenuItem != null && post != null && !post.isHasComplain() && isAdmin) {
            complainActionMenuItem.setVisible(true);
        }

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.post_details_menu, menu);
//        if(post!=null&&!post.getAuthorId().equals(getCurrentUserId())){
//            editActionMenuItem=menu.findItem(R.id.edit_post);
//            editActionMenuItem.setVisible(false);
//        }
        complainActionMenuItem = menu.findItem(R.id.complain_action);
        publicActionMenuItem = menu.findItem(R.id.make_public_action);
        editActionMenuItem = menu.findItem(R.id.edit_post);
        deleteActionMenuItem = menu.findItem(R.id.delete_post_action);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (!isPostExist) {
            return super.onOptionsItemSelected(item);
        }

        // Handle item selection
        switch (item.getItemId()) {
            case R.id.official_feedback_menu_item:
                openConfirmPaymentDialog();
                return true;
            case R.id.share_record_menu_item:
                requestRecordShareActivity();
                return true;
            case R.id.follow_post_menu_item:
                attemptFollowPost();
                return true;
            case R.id.complain_action:
                doComplainAction();
                return true;
            case R.id.make_public_action:
                makePublicAction();
                return true;

            case R.id.edit_post:
                if (hasAccessToModifyPost()) {
                    openEditPostActivity();
                }
                return true;
            case R.id.ratings_chart_menu_item:
                openRatingsChartActivity();
                return true;
            case R.id.delete_post_action:
                if (hasAccessToModifyPost()) {
                    attemptToRemovePost();
                }
                return true;
//
//            case R.id.edit_post:
//                if (hasAccessToModifyPost()) {
//                    openEditPostActivity();
//                    }
//                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void doComplainAction() {
        ProfileStatus profileStatus = profileManager.checkProfile();

        if (profileStatus.equals(ProfileStatus.PROFILE_CREATED)) {
            openComplainDialog();
        } else {
            doAuthorization(profileStatus);
        }
    }

    private void makePublicAction() {
        if (!hasInternetConnection()) {
            showSnackBar(R.string.internet_connection_failed);
            return;
        }
        ProfileStatus profileStatus = profileManager.checkProfile();
        if (profileStatus.equals(ProfileStatus.PROFILE_CREATED)) {
            openConfirmPublicDialog();
        } else {
            doAuthorization(profileStatus);
        }
    }

    private void openRatingsChartActivity() {
        Intent intent = new Intent(PostDetailsActivity.this, RatingsChartActivity.class);
        startActivity(intent);
    }

    @SuppressLint("NewApi")
    private void requestRecordShareActivity() {
        if (PermissionsUtil.isReadWritePermissionRequired(PostDetailsActivity.this)) {
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, PermissionsUtil.MY_PERMISSIONS_READ_EXTERNAL);
        } else {
            openRecordShareActivity();
        }
    }

    private void openRecordShareActivity() {
        if (!hasInternetConnection()) {
            showSnackBar(R.string.internet_connection_failed);
            return;
        }
        Intent intent = new Intent(PostDetailsActivity.this, RecordShareActivity.class);
        RecordingItem item = new RecordingItem();
        item.setName(post.getTitle());
        item.setLength(post.getAudioDuration());
        item.setFilePath(post.getImagePath());
        intent.putExtra(RecordShareActivity.MUSIC_URI_EXTRA_KEY, post.getImagePath());
        intent.putExtra(RecordShareActivity.PROFILE_URL_EXTRA_KEY, profile.getPhotoUrl());
        intent.putExtra(RecordShareActivity.PROFILE_NAME_EXTRA_KEY, profile.getUsername());
        intent.putExtra(RecordShareActivity.POST_TITLE_EXTRA_KEY, post.getTitle());
        intent.putExtra(RecordShareActivity.AUDIO_DURATION_EXTRA_KEY, post.getAudioDuration());
        startActivity(intent);
    }

    //Handling callback
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PermissionsUtil.MY_PERMISSIONS_READ_EXTERNAL: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay!
                    Toast.makeText(this, "Permissions granted to save recording", Toast.LENGTH_LONG).show();
                    openRecordShareActivity();
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    showWarningDialog("Permissions Denied to save recording. Please try again.");
                }
                return;
            }
            case PermissionsUtil.MY_PERMISSIONS_RECORD_AUDIO: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay!
                    Toast.makeText(this, "Permissions granted to record audio", Toast.LENGTH_LONG).show();
                    startRecording();
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    showWarningDialog("Permissions Denied to record audio. Please try again.");
                }
                return;
            }
        }
    }

    private void attemptFollowPost() {
        if (hasInternetConnection()) {
            postManager.followPost(PostDetailsActivity.this, getCurrentUserId(), post.getId(), success ->  {
                if (success) {
                    showSnackBar(R.string.follow_post_success);
                } else {
                    showSnackBar(R.string.error_fail_remove_post);
                }
            });
        } else {
            showSnackBar(R.string.internet_connection_failed);
        }
    }

    private void attemptToRemovePost() {
        if (hasInternetConnection()) {
            if (!postRemovingProcess) {
                openConfirmDeletingDialog();
            }
        } else {
            showSnackBar(R.string.internet_connection_failed);
        }
    }

    private void removePost() {
        postManager.removePost(post, new OnTaskCompleteListener() {
            @Override
            public void onTaskComplete(boolean success) {
                if (success) {
                    Intent intent = getIntent();
                    setResult(RESULT_OK, intent.putExtra(POST_STATUS_EXTRA_KEY, PostStatus.REMOVED));
                    finish();
                } else {
                    postRemovingProcess = false;
                    showSnackBar(R.string.error_fail_remove_post);
                }

                hideProgress();
            }
        });

        showProgress(R.string.removing);
        postRemovingProcess = true;
    }

    private void openEditPostActivity() {
        if (hasInternetConnection()) {
            Intent intent = new Intent(PostDetailsActivity.this, EditPostActivity.class);
            intent.putExtra(EditPostActivity.POST_EXTRA_KEY, post);
            startActivityForResult(intent, EditPostActivity.EDIT_POST_REQUEST);
        } else {
            showSnackBar(R.string.internet_connection_failed);
        }
    }

    private void openConfirmDeletingDialog() {
        AlertDialog.Builder builder = new BaseAlertDialogBuilder(this);
        builder.setMessage(R.string.confirm_deletion_post)
                .setNegativeButton(R.string.button_title_cancel, null)
                .setPositiveButton(R.string.button_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        removePost();
                    }
                });

        builder.create().show();
    }

    private void openConfirmPublicDialog() {
        AlertDialog.Builder builder = new BaseAlertDialogBuilder(this);
        builder.setMessage(R.string.confirm_public_post)
                .setNegativeButton(R.string.button_title_cancel, null)
                .setPositiveButton(R.string.button_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        makePublic();
                    }
                });

        builder.create().show();
    }

    private void openConfirmPaymentDialog() {
        if (!hasInternetConnection()) {
            showSnackBar(R.string.internet_connection_failed);
            return;
        }
        AlertDialog.Builder builder = new BaseAlertDialogBuilder(this);
        builder.setMessage(String.format(getString(R.string.confirm_continue_payment), paymentAmount))
                .setNegativeButton(R.string.button_title_cancel, null)
                .setPositiveButton(R.string.button_title_continue, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        createOfficialFeedbackRequest();
                    }
                });

        builder.create().show();
    }

    private void openComplainDialog() {
        AlertDialog.Builder builder = new BaseAlertDialogBuilder(this);
        builder.setTitle(R.string.add_complain)
                .setMessage(R.string.complain_text)
                .setNegativeButton(R.string.button_title_cancel, null)
                .setPositiveButton(R.string.add_complain, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        addComplain();
                    }
                });

        builder.create().show();
    }

    private void addComplain() {
        postManager.addComplain(post);
        complainActionMenuItem.setVisible(false);
        showSnackBar(R.string.complain_sent);
    }

    private void makePublic() {
        postManager.makePublic(post);
        publicActionMenuItem.setVisible(false);
        showSnackBar(R.string.make_public_success);
    }

    private void removeComment(Comment comment) {
        showProgress();
        commentManager.removeComment(comment, postId, new OnTaskCompleteListener() {
            @Override
            public void onTaskComplete(boolean success) {
                hideProgress();
                showSnackBar(R.string.message_comment_was_removed);
            }
        });
    }

    private void openEditCommentDialog(Comment comment) {
        EditCommentDialog editCommentDialog = new EditCommentDialog();
        Bundle args = new Bundle();
        args.putString(EditCommentDialog.COMMENT_TEXT_KEY, comment.getText());
        args.putString(EditCommentDialog.COMMENT_ID_KEY, comment.getId());
        editCommentDialog.setArguments(args);
        editCommentDialog.show(getFragmentManager(), EditCommentDialog.TAG);
    }

    private void updateComment(String newText, String commentId) {
        showProgress();
        commentManager.updateComment(commentId, newText, postId, new OnTaskCompleteListener() {
            @Override
            public void onTaskComplete(boolean success) {
                hideProgress();
                showSnackBar(R.string.message_comment_was_edited);
            }
        });
    }
    private void updateComment(Comment comment, int points) {
        showProgress();
        commentManager.updateComment(comment, postId, new OnTaskCompleteListener() {
            @Override
            public void onTaskComplete(boolean success) {
                hideProgress();
                if(points!=-2 && points!=0){
                    showSnackBar(R.string.message_comment_was_rewared);}
            }
        });
    }

    @Override
    public void onCommentChanged(String newText, String commentId) {

        updateComment(newText, commentId);
    }

    //    private class ActionModeCallback implements ActionMode.Callback {
//        Comment selectedComment;
//        int position;
//
//        ActionModeCallback(Comment selectedComment) {
//            this.selectedComment = selectedComment;
//        }
//
//        // Called when the action mode is created; startActionMode() was called
//        @Override
//        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
//            // Inflate a menu resource providing context menu items
//            MenuInflater inflater = mode.getMenuInflater();
//            inflater.inflate(R.menu.comment_context_menu, menu);
//
//            menu.findItem(R.id.editMenuItem).setVisible(hasAccessToEditComment(selectedComment.getAuthorId()));
//
//            return true;
//        }
//
//        // Called each time the action mode is shown. Always called after onCreateActionMode, but
//        // may be called multiple times if the mode is invalidated.
//        @Override
//        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
//            return false; // Return false if nothing is done
//        }
//
//        // Called when the user selects a contextual menu item
//        @Override
//        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
//            switch (item.getItemId()) {
//                case R.id.editMenuItem:
//                    openEditCommentDialog(selectedComment);
//                    mode.finish(); // Action picked, so close the CAB
//                    return true;
//                case R.id.deleteMenuItem:
//                    removeComment(selectedComment.getId(), mode, position);
//                    return true;
//                default:
//                    return false;
//            }
//        }
//
//        // Called when the user exits the action mode
//        @Override
//        public void onDestroyActionMode(ActionMode mode) {
//            mActionMode = null;
//        }
//    }
    Animator.AnimatorListener authorAnimatorListener = new Animator.AnimatorListener() {
        @Override
        public void onAnimationStart(Animator animation) {
            authorAnimationInProgress = true;
        }

        @Override
        public void onAnimationEnd(Animator animation) {
            authorAnimationInProgress = false;
        }

        @Override
        public void onAnimationCancel(Animator animation) {
            authorAnimationInProgress = false;
        }

        @Override
        public void onAnimationRepeat(Animator animation) {

        }
    };

    private void openUserComplainDialog(Flag flag) {
        ComplainDialog complainDialog = new ComplainDialog();
        Bundle args = new Bundle();
        args.putSerializable(ComplainDialog.FLAG_KEY, flag);
        complainDialog.setArguments(args);
        complainDialog.show(getFragmentManager(), ComplainDialog.TAG);
    }

    private void openUserBlockDialog(String blockUser) {
        BlockDialog blockDialog = new BlockDialog();
        Bundle args = new Bundle();
        args.putString(BlockDialog.BLOCKED_USER_KEY, blockUser);
        blockDialog.setArguments(args);
        blockDialog.show(getFragmentManager(), ComplainDialog.TAG);
    }

    @Override
    public void onFlagReason(Flag flag) {
        postManager.flagUser(flag, new OnTaskCompleteListener() {
            @Override
            public void onTaskComplete(boolean success) {
                if (success) {
                    showDialog("Received your complaint. Will review and send a warning to user.");
                } else {
                    showSnackBar(R.string.error_fail_create_complain);
                }
            }
        });
    }

    @Override
    public void onBlock(String blockedUser, String reason) {
        profileManager.blockUser(blockedUser, reason, new OnTaskCompleteListener() {
            @Override
            public void onTaskComplete(boolean success) {
                if (success) {
                    showSnackBar(R.string.block_success);
                } else {
                    showSnackBar(R.string.error_fail_block);
                }
            }
        });
    }

    // Recording Start/Stop
    @SuppressLint("NewApi")
    public void onRecord(){
        if (PermissionsUtil.isExplicitPermissionRequired(PostDetailsActivity.this)) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE}, PermissionsUtil.MY_PERMISSIONS_RECORD_AUDIO);
        } else {
            startRecording();
        }
    }

    private void startRecording() {
        if (mStartRecording) {
            mRecordButton.setImageResource(R.drawable.ic_media_stop);
            commentRecordLayout.startRecording();
            mStartRecording = !mStartRecording;
        } else {
            stopRecording();
        }
    }

    private void stopRecording() {
        sendButton.setVisibility(View.VISIBLE);
        mRecordButton.setImageResource(R.drawable.ic_mic_white_36dp);
        commentRecordLayout.stopRecording();
        mStartRecording = !mStartRecording;
    }
}
