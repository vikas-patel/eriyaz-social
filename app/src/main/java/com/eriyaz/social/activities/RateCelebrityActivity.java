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

import android.animation.Animator;
import android.app.ActionBar;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.eriyaz.social.R;
import com.eriyaz.social.adapters.RatingsAdapter;
import com.eriyaz.social.controllers.RatingController;
import com.eriyaz.social.dialogs.AvatarDialog;
import com.eriyaz.social.enums.PostOrigin;
import com.eriyaz.social.enums.PostStatus;
import com.eriyaz.social.enums.ProfileStatus;
import com.eriyaz.social.managers.PostManager;
import com.eriyaz.social.managers.ProfileManager;
import com.eriyaz.social.managers.listeners.OnDataChangedListener;
import com.eriyaz.social.managers.listeners.OnObjectChangedListener;
import com.eriyaz.social.managers.listeners.OnPostChangedListener;
import com.eriyaz.social.model.Flag;
import com.eriyaz.social.model.Point;
import com.eriyaz.social.model.Post;
import com.eriyaz.social.model.Profile;
import com.eriyaz.social.model.Rating;
import com.eriyaz.social.utils.FormatterUtil;
import com.eriyaz.social.utils.RatingUtil;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.util.Util;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.xw.repo.BubbleSeekBar;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

public class RateCelebrityActivity extends BaseActivity {
    private static final String TAG = RateCelebrityActivity.class.getSimpleName();

    private static final String CELEBRITY_POST_ID = "-L2zcBNTtj1WZjCeqNAv";

    public static final String POST_ID_EXTRA_KEY = "PostDetailsActivity.POST_ID_EXTRA_KEY";
    public static final String AUTHOR_ANIMATION_NEEDED_EXTRA_KEY = "PostDetailsActivity.AUTHOR_ANIMATION_NEEDED_EXTRA_KEY";
    public static final int TIME_OUT_LOADING_COMMENTS = 30000;
    public static final int UPDATE_POST_REQUEST = 1;
    public static final String POST_STATUS_EXTRA_KEY = "PostDetailsActivity.POST_STATUS_EXTRA_KEY";
    public static final String POST_ORIGIN_EXTRA_KEY = "PostDetailsActivity.POST_ORIGIN_EXTRA_KEY";
    public static final String CELEBRITY_AVATAR_URL = "https://firebasestorage.googleapis.com/v0/b/eriyaz-social-dev.appspot.com/o/images%2Fcelebrity-singers%2Fatif-aslam.jpeg?alt=media&token=b45444f9-99a7-4d17-9eb6-4badf196fb5e";


    @Nullable
    private Post post;
    private Profile profile;
    private ScrollView scrollView;
    private ImageView ratingsImageView;
    private TextView ratingCounterTextView;
    private TextView averageRatingTextView;

    private ProgressBar playerProgressBar;
    private TextView fileName;

    private TextView ratingsLabel;
    private ProgressBar ratingsProgressBar;
    private RecyclerView ratingsRecyclerView;
    private TextView warningRatingsTextView;
    private boolean attemptToLoadRatings = false;

    private MenuItem complainActionMenuItem;
    private MenuItem editActionMenuItem;
    private MenuItem deleteActionMenuItem;
    private MenuItem publicActionMenuItem;

    private String postId;

    private PostManager postManager;
    private ProfileManager profileManager;

    //    private LikeController likeController;
//    private RatingController ratingController;
    private boolean postRemovingProcess = false;
    private boolean isPostExist;
    private boolean authorAnimationInProgress = false;

    private boolean isAuthorAnimationRequired;
    private RatingsAdapter ratingsAdapter;
    private ActionMode mActionMode;
    private boolean isEnterTransitionFinished = false;
    final FirebaseRemoteConfig remoteConfig = FirebaseRemoteConfig.getInstance();
    private int paymentAmount = (int) remoteConfig.getLong("payment_amount");

    private PlayerView playerView;
    private long startTimePlayer;
    private SimpleExoPlayer player;
    private long playbackPosition = 0;
    private int currentWindow = 0;
    private boolean playWhenReady = false;

    private BubbleSeekBar ratingBar;
    private Rating rating;
    private RatingController ratingController;
    private int intialRatingValue = 0;

    private boolean isRatingsCollapsed = true;

    private boolean isRatingSubmitted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rate_celebrity);
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        profileManager = ProfileManager.getInstance(this);
        postManager = PostManager.getInstance(this);


        isAuthorAnimationRequired = getIntent().getBooleanExtra(AUTHOR_ANIMATION_NEEDED_EXTRA_KEY, false);
//        postId = getIntent().getStringExtra(POST_ID_EXTRA_KEY);
        postId = CELEBRITY_POST_ID;

        incrementWatchersCount();

        fileName = (TextView) findViewById(R.id.file_name_text);

        playerProgressBar = (ProgressBar) findViewById(R.id.playerProgressBar);
        scrollView = (ScrollView) findViewById(R.id.scrollView);

        ratingsRecyclerView = (RecyclerView) findViewById(R.id.ratingsRecyclerView);
        ratingsLabel = (TextView) findViewById(R.id.ratingsLabel);
        ratingsProgressBar = (ProgressBar) findViewById(R.id.ratingsProgressBar);
        warningRatingsTextView = (TextView) findViewById(R.id.warningRatingsTextView);

//        likesContainer = (ViewGroup) findViewById(R.id.likesContainer);
//        likesImageView = (ImageView) findViewById(R.id.likesImageView);
        ratingsImageView = (ImageView) findViewById(R.id.ratingImageView);
        ratingCounterTextView = (TextView) findViewById(R.id.ratingCounterTextView);
        averageRatingTextView = (TextView) findViewById(R.id.averageRatingTextView);

        playerView = findViewById(R.id.exoPlayerView);

        postManager.getPost(this, postId, createOnPostChangeListener());

        ratingBar = (BubbleSeekBar) findViewById(R.id.ratingBar);
        rating = new Rating();

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

        findViewById(R.id.submitRating).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                findViewById(R.id.seekbarContainer).setVisibility(View.GONE);
                findViewById(R.id.rateBelowLabel).setVisibility(View.GONE);


                int progress = ratingBar.getProgress();
                findViewById(R.id.viewOtherRatings).setVisibility(View.VISIBLE);
                ((TextView) findViewById(R.id.celebrityName)).setText(String.format(getString(R.string.celebrity_name),post.getCelebrityName()));
                ((TextView) findViewById(R.id.celebrityRecievedRating)).setText(String.format(getString(R.string.celebrity_rating), progress));
                findViewById(R.id.celebrityReveal).setVisibility(View.VISIBLE);
                if(progress < 10) {
                    findViewById(R.id.warning).setVisibility(View.VISIBLE);
                }
                ratingController.handleRatingClickAction((BaseActivity) RateCelebrityActivity.this, post, progress);
//                ratingController.setUpdatingRatingCounter(false);
                findViewById(R.id.confirmRatingSection).setVisibility(View.GONE);
                findViewById(R.id.nextButton).setVisibility(View.VISIBLE);
                isRatingSubmitted = true;


            }
        });

        findViewById(R.id.cancelRating).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ratingBar.setProgress(0);
                findViewById(R.id.confirmRatingSection).setVisibility(View.GONE);
            }
        });

        String avatarImageUrl = CELEBRITY_AVATAR_URL;
        Glide.with(this)
            .load(avatarImageUrl)
            .diskCacheStrategy(DiskCacheStrategy.SOURCE)
            .crossFade()
            .into((ImageView)findViewById(R.id.celebrityAvatar));


    }

    private void updateRatingDetails() {
        intialRatingValue = (int) rating.getRating();
        ratingController = new RatingController(ratingBar, post.getId(), rating);
        ratingBar.setOnProgressChangedListener(new BubbleSeekBar.OnProgressChangedListenerAdapter() {
            @Override
            public void onProgressChanged(BubbleSeekBar bubbleSeekBar, int progress, float progressFloat, boolean fromUser) {
                int color = RatingUtil.getRatingColor(RateCelebrityActivity.this, progress);
                bubbleSeekBar.setSecondTrackColor(color);
                bubbleSeekBar.setThumbColor(color);
                bubbleSeekBar.setBubbleColor(color);
            }

            @Override
            public void getProgressOnActionUp(BubbleSeekBar bubbleSeekBar, final int progress, float progressFloat) {
                ((TextView) findViewById(R.id.confirmRatingText)).setText(Html.fromHtml(String.format(getString(R.string.rating_selection),progress)));
                findViewById(R.id.confirmRatingSection).setVisibility(View.VISIBLE);
            }

            @Override
            public void getProgressOnFinally(BubbleSeekBar bubbleSeekBar, int progress, float progressFloat, boolean fromUser) {
            }
        });

    }

    public void toggleRatingsAndComments(View v) {
        final View ratingsAndCommentsContainer = findViewById(R.id.ratingsAndCommentsContainer);
        if(isRatingsCollapsed) {
           ratingsAndCommentsContainer.setVisibility(View.VISIBLE);
            scrollView.post(new Runnable() {
                public void run() {
                    scrollView.scrollTo(0,ratingsAndCommentsContainer.getTop());
                }
            });

            isRatingsCollapsed = false;
        } else {
            ratingsAndCommentsContainer.setVisibility(View.GONE);
            scrollView.scrollTo(0, scrollView.getTop());

            isRatingsCollapsed = true;
        }
    }

    @Override
    public void showPointSnackbar(Point point) {    //disable points snackbar
    }

    private void initializePlayer() {
        if (startTimePlayer == 0) startTimePlayer = new Date().getTime();
        player = ExoPlayerFactory.newSimpleInstance(
                new DefaultRenderersFactory(this),
                new DefaultTrackSelector(), new DefaultLoadControl());

        playerView.setPlayer(player);
        playerView.setControllerHideOnTouch(false);
        player.setPlayWhenReady(playWhenReady);
        player.seekTo(currentWindow, playbackPosition);


        if(post != null) {
            Uri uri = Uri.parse(post.getImagePath());
            Log.d("URI", post.getImagePath());
            // play from fileSystem
            MediaSource mediaSource;
            mediaSource = buildMediaSource(uri);
            player.prepare(mediaSource, true, false);
        }

        player.addListener(new Player.EventListener() {
            @Override
            public void onTimelineChanged(Timeline timeline, Object manifest, int reason) {

            }

            @Override
            public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {

            }

            @Override
            public void onLoadingChanged(boolean isLoading) {

            }

            @Override
            public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
                if(!isRatingSubmitted) {
                    if (playbackState == Player.STATE_READY) {
                        playerProgressBar.setVisibility(View.GONE);
                        playerView.setVisibility(View.VISIBLE);
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                findViewById(R.id.seekbarProgressBar).setVisibility(View.GONE);
                                findViewById(R.id.seekbarContainer).setVisibility(View.VISIBLE);
                            }
                        }, 1000);
                    }
                }
            }

            @Override
            public void onRepeatModeChanged(int repeatMode) {

            }

            @Override
            public void onShuffleModeEnabledChanged(boolean shuffleModeEnabled) {

            }

            @Override
            public void onPlayerError(ExoPlaybackException error) {

            }

            @Override
            public void onPositionDiscontinuity(int reason) {

            }

            @Override
            public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {

            }

            @Override
            public void onSeekProcessed() {

            }
        });
    }

    private void releasePlayer() {
        if (player != null) {
            playbackPosition = player.getCurrentPosition();
            currentWindow = player.getCurrentWindowIndex();
            playWhenReady = player.getPlayWhenReady();
            player.release();
            player = null;
        }
    }

    private MediaSource buildMediaSource(Uri uri) {
        return new ExtractorMediaSource.Factory(
                new DefaultHttpDataSourceFactory("eriyaz.social-exoplayer")).
                createMediaSource(uri);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (Util.SDK_INT > 23) {
            initializePlayer();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if ((Util.SDK_INT <= 23)) {
            initializePlayer();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (Util.SDK_INT <= 23) {
            releasePlayer();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (Util.SDK_INT > 23) {
            releasePlayer();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        postManager.closeListeners(this);
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
        return;
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

            }

            @Override
            public void onAuthorClick(String authorId, View view) {
            }

            @Override
            public void makeRatingVisible(int position) {

            }

            @Override
            public void onReplyClick(int position) {
                Rating selectedRating = ratingsAdapter.getItemByPosition(position);
                openRaterMessageActivity(selectedRating.getAuthorId());
            }


        });
        ratingsRecyclerView.setAdapter(ratingsAdapter);
        ratingsRecyclerView.setNestedScrollingEnabled(false);
        ratingsRecyclerView.addItemDecoration(new DividerItemDecoration(ratingsRecyclerView.getContext(),
                ((LinearLayoutManager) ratingsRecyclerView.getLayoutManager()).getOrientation()));

        postManager.getRatingsList(this, postId, createOnRatingsChangedDataListener());
    }

    private void openUserComplainDialog(Flag flag) {
        showSnackBar("Complaint Registered.");
    }

    private void openRaterMessageActivity(String userId) {
        if (hasInternetConnection()) {
            Intent intent = new Intent(RateCelebrityActivity.this, MessageActivity.class);
            intent.putExtra(ProfileActivity.USER_ID_EXTRA_KEY, userId);
            startActivity(intent);
        } else {
            showSnackBar(R.string.internet_connection_failed);
        }
    }

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
                AlertDialog.Builder builder = new BaseAlertDialogBuilder(RateCelebrityActivity.this);
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
        isPostExist = true;
        fillPostFields();
        updateCounters();
        initLikeButtonState();
        invalidateOptionsMenu();
        initializePlayer();
        updateRatingDetails();
        initRatingRecyclerView();
    }

    private void incrementWatchersCount() {
        postManager.incrementWatchersCount(postId);
        Intent intent = getIntent();
        setResult(RESULT_OK, intent.putExtra(POST_STATUS_EXTRA_KEY, PostStatus.UPDATED));
    }

    private void showPostWasRemovedDialog() {
        AlertDialog.Builder builder = new BaseAlertDialogBuilder(RateCelebrityActivity.this);
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


    private void fillPostFields() {
        if (post != null) {
            fileName.setText(post.getTitle());
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


    private void updateCounters() {
        if (post == null) {
            return;
        }

        ratingCounterTextView.setText("(" + post.getRatingsCount() + ")");
        String avgRatingText = "";
        if (post.getAverageRating() > 0) {
            avgRatingText = String.format( "%.1f", post.getAverageRating());
        }
        averageRatingTextView.setText(avgRatingText);

        CharSequence date = FormatterUtil.getRelativeTimeSpanStringShort(this, post.getCreatedDate());

        ratingsLabel.setText(String.format(getString(R.string.label_ratings), post.getRatingsCount()));

        if (post.getRatingsCount() == 0) {
            ratingsLabel.setVisibility(View.GONE);
            ratingsProgressBar.setVisibility(View.GONE);
        } else if (ratingsLabel.getVisibility() != View.VISIBLE) {
            ratingsLabel.setVisibility(View.VISIBLE);
        }
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

    private void openImageDetailScreen() {
        if (post != null) {
            Intent intent = new Intent(this, ImageDetailActivity.class);
            intent.putExtra(ImageDetailActivity.IMAGE_URL_EXTRA_KEY, post.getImagePath());
            startActivity(intent);
        }
    }

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
//            editActionMenuItem.setVisible(true);
            deleteActionMenuItem.setVisible(true);
        }

        if (publicActionMenuItem != null && post.isAnonymous() && hasAccessToModifyPost()) {
            publicActionMenuItem.setVisible(true);
        }

        if (complainActionMenuItem != null && post != null && !post.isHasComplain() && profile.isAdmin()) {
            complainActionMenuItem.setVisible(true);
        }
        return super.onPrepareOptionsMenu(menu);
    }


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
}
