package com.eriyaz.social.fragments;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.text.Html;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.eriyaz.social.Application;
import com.eriyaz.social.Constants;
import com.eriyaz.social.R;
import com.eriyaz.social.activities.BaseActivity;
import com.eriyaz.social.activities.BaseAlertDialogBuilder;
import com.eriyaz.social.activities.BaseCurrentProfileActivity;
import com.eriyaz.social.activities.MainActivity;
import com.eriyaz.social.activities.PostDetailsActivity;
import com.eriyaz.social.activities.ProfileActivity;
import com.eriyaz.social.controllers.RatingController;
import com.eriyaz.social.enums.ProfileStatus;
import com.eriyaz.social.managers.CommentManager;
import com.eriyaz.social.managers.ProfileManager;
import com.eriyaz.social.managers.listeners.OnTaskCompleteListener;
import com.eriyaz.social.model.Comment;
import com.eriyaz.social.model.Post;
import com.eriyaz.social.model.Profile;
import com.eriyaz.social.model.Rating;
import com.eriyaz.social.model.RecordingItem;
import com.eriyaz.social.utils.PermissionsUtil;
import com.eriyaz.social.utils.PreferencesUtil;
import com.eriyaz.social.utils.RatingUtil;
import com.eriyaz.social.utils.TimestampTagUtil;
import com.eriyaz.social.views.RecordLayout;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.ui.SimpleExoPlayerView;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.util.Util;
import com.google.firebase.auth.FirebaseAuth;
import com.volokh.danylo.hashtaghelper.HashTagHelper;
import com.xw.repo.BubbleSeekBar;

import java.io.File;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by Daniel on 1/1/2015.
 */
public class PlaybackFragment extends BaseDialogFragment {

    public static final String RECORDING_ITEM = "recording_item";
    private RecordingItem item;
    private Button closeButton = null;
    private TextView mFileNameTextView = null;
    private BubbleSeekBar ratingBar;
    private Post post;
    private Rating rating;
    private String authorName;
    private RatingController ratingController;
    private boolean isRatingChanged = false;
    private long startTimePlayer;
    private SimpleExoPlayerView playerView;
    private SimpleExoPlayer player;
    private int currentWindow = 0;
    private long playbackPosition = 0;
    private boolean playWhenReady = false;
    private ComponentListener componentListener;
    private TextView moreTextView;
//    private LinearLayout detailedFeedbackLayout;
    private LinearLayout commentLayout;
    private LinearLayout textCommentLayout;
    private Button submitButton;
//    private RadioGroup melodyRadioGroup;
//    private RadioGroup voiceQualityRadioGroup;
    private CommentManager commentManager;
    private TextView ratingTextView, rateBelowText, rateInfoText;
    private TextView earnExtraTextView;
    private TextView recordErrorTextView;
//    private TextView melodyPercentageLabel;
//    private TextView voiceQualityLabel;
    private FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
    private Application application;
//    private CheckBox harkateView;
    private CheckBox pronounciationView;
//    private CheckBox highPitchView;
    private CheckBox feelView;
    private int intialRatingValue = 0;

    private EditText mistakesTextView;
    private HashTagHelper mistakesTextHashTagHelper;
    private Button mistakeTapButton;
    // Recorder
    private boolean mStartRecording = true;
    private ImageButton mRecordButton;
    private RecordLayout commentRecordLayout;

    public PlaybackFragment newInstance(RecordingItem item) {
        PlaybackFragment f = new PlaybackFragment();
        Bundle b = new Bundle();
        b.putParcelable(RECORDING_ITEM, item);
        f.setArguments(b);
        return f;
    }

    public PlaybackFragment newInstance(RecordingItem item, Post post, Rating rating, String authorName) {
        PlaybackFragment f = new PlaybackFragment();
        Bundle b = new Bundle();
        b.putParcelable(RECORDING_ITEM, item);
        b.putSerializable(PostDetailsActivity.POST_ID_EXTRA_KEY, post);
        b.putSerializable(Rating.RATING_ID_EXTRA_KEY, rating);
        b.putString(Profile.AUTHOR_NAME_EXTRA_KEY, authorName);
        f.setArguments(b);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        item = getArguments().getParcelable(RECORDING_ITEM);
        post = (Post) getArguments().getSerializable(PostDetailsActivity.POST_ID_EXTRA_KEY);
        rating = (Rating) getArguments().getSerializable(Rating.RATING_ID_EXTRA_KEY);
        authorName = getArguments().getString(Profile.AUTHOR_NAME_EXTRA_KEY);
        if (rating == null) rating = new Rating();

    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        // post deleted
        if (getActivity() == null) return;
        BaseActivity activity = (BaseActivity) getActivity();
        if (activity.isActivityDestroyed()) return;
        if (post != null) {
            if (player != null && player.getPlayWhenReady() && player.getPlaybackState() == Player.STATE_READY) {
                markPlayerPositions();
            }
            if (player != null && !isListenNotEnough()) {
                application.addPlayedPost(post.getId());
            }
            activity.getAnalytics().logPlayedTime(post.getAuthorId(), post.getTitle(), (int) totalPlayed/1000);
        }
        if (!isRatingChanged) return;
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).updatePost();
        } else if (getActivity() instanceof ProfileActivity) {
            ((ProfileActivity) getActivity()).updatePost();
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        BaseActivity activity = (BaseActivity) getActivity();
        application = (Application) getActivity().getApplication();
        activity.getAnalytics().logOpenAudio(post.getAuthorId());
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        Dialog dialog = super.onCreateDialog(savedInstanceState);

        AlertDialog.Builder builder = new BaseAlertDialogBuilder(getActivity());
        View view = getActivity().getLayoutInflater().inflate(R.layout.fragment_media_playback, null);

        mFileNameTextView = (TextView) view.findViewById(R.id.file_name_text_view);
        ratingBar = (BubbleSeekBar) view.findViewById(R.id.ratingBar);
        View seekbarView=view.findViewById(R.id.seekbarContainer);
        LinearLayout mainLayout=view.findViewById(R.id.mainLayout);
        rateBelowText=view.findViewById(R.id.ratingTextView);
        closeButton = view.findViewById(R.id.closeButton);
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onCloseButton();
            }
        });
//        detailedFeedbackLayout = view.findViewById(R.id.detailedFeedbackLayout);
        commentLayout = view.findViewById(R.id.commentLayout);
        textCommentLayout = view.findViewById(R.id.newCommentContainer);
        moreTextView = view.findViewById(R.id.moreTextView);
        moreTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openDetailedFeedback();
            }
        });
        if (!PreferencesUtil.isUserRatedMany(getActivity())) {
            moreTextView.setVisibility(View.GONE);
        }

        // Changes for issue 103
        if (post.getRatingsCount() >= 15) {
            // Hide ratings bar
            rateBelowText.setVisibility(View.GONE);
            seekbarView.setVisibility(View.GONE);
            //rateInfoText.setVisibility(View.VISIBLE);

            LinearLayout.LayoutParams layoutParams=new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            TextView ratingTextInfo=new TextView(getContext());
            ratingTextInfo.setLayoutParams(layoutParams);
            ratingTextInfo.setText(R.string.ratingInfo);
            ratingTextInfo.setTextSize(16);
            ratingTextInfo.setTextColor(getResources().getColor(R.color.primary_dark));
            ratingTextInfo.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
            mainLayout.addView(ratingTextInfo, 2);
        }

        submitButton = view.findViewById(R.id.submitButton);
        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                submitDetailedFeedback();
                v.setClickable(false);

                v.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        v.setClickable(true);

                    }
                }, 2000);
            }
        });

        mistakesTextView = view.findViewById(R.id.mistakesTextView);
        mistakeTapButton = view.findViewById(R.id.mistakeTapButton);

        mistakesTextHashTagHelper = HashTagHelper.Creator.create(getResources().getColor(R.color.red), new HashTagHelper.OnHashTagClickListener() {
            @Override
            public void onHashTagClicked(String hashTag) {
                if(TimestampTagUtil.isValidTimestamp(hashTag)) {
                    player.seekTo(TimestampTagUtil.timestampToMillis(hashTag));
                }
            }
        }, new char[] {':'});
        mistakesTextHashTagHelper.handle(mistakesTextView);

        mistakeTapButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mistakesTextView.getText().insert(mistakesTextView.getSelectionStart(), String.format(" #%s ",
                        TimestampTagUtil.millisToTimestamp(Math.max(0,player.getContentPosition()-1000))));
                mistakesTextView.requestFocus();
            }
        });

//        melodyRadioGroup = view.findViewById(R.id.melodyPercRadioGroup);
//        voiceQualityRadioGroup = view.findViewById(R.id.voiceQualityRadioGroup);
//        melodyPercentageLabel = view.findViewById(R.id.melodyPercentageLabel);
        ratingTextView = view.findViewById(R.id.ratingTextView);
        earnExtraTextView = view.findViewById(R.id.earnExtraTextView);
        recordErrorTextView = view.findViewById(R.id.recordErrorTextView);
//        voiceQualityLabel = view.findViewById(R.id.voiceQualityLabel);
//        harkateView = view.findViewById(R.id.harkateCheckboxId);
//        pronounciationView = view.findViewById(R.id.pronounciationCheckboxId);
//        highPitchView = view.findViewById(R.id.highPitchCheckboxId);
//        feelView = view.findViewById(R.id.noFeelCheckboxId);

        mFileNameTextView.setText(item.getName());
        updateRatingDetails();
        builder.setView(view);
        // request a window without the title
        dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        componentListener = new ComponentListener();
        playerView = view.findViewById(R.id.exoPlayerView);
        commentManager = CommentManager.getInstance(this.getActivity());
        commentRecordLayout = view.findViewById(R.id.recordLayout);
        mRecordButton = view.findViewById(R.id.recordButton);
        mRecordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onRecord();
            }
        });

        return builder.create();
    }

    private void onCloseButton() {
        if (ratingBar.getProgress() > 0 && ratingBar.getProgress() <= 5) {
            AlertDialog.Builder builder = new BaseAlertDialogBuilder(getActivity());
            builder.setMessage(Html.fromHtml(getString(R.string.rating_lost_popup)))
                    .setNegativeButton(R.string.button_title_cancel, null)
                    .setPositiveButton(R.string.button_ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dismiss();
                        }
                    });
            builder.create().show();
            return;
        }
        if (commentRecordLayout.getRecordItem() != null) {
            AlertDialog.Builder builder = new BaseAlertDialogBuilder(getActivity());
            builder.setMessage(R.string.confirm_close_play_popup)
                    .setNegativeButton(R.string.button_title_cancel, null)
                    .setPositiveButton(R.string.button_ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dismiss();
                        }
                    });
            builder.create().show();
        } else {
            dismiss();
        }
    }

    private void openDetailedFeedback() {
        commentLayout.setVisibility(View.VISIBLE);
        textCommentLayout.setVisibility(View.VISIBLE);
        submitButton.setVisibility(View.VISIBLE);
        moreTextView.setVisibility(View.GONE);
        earnExtraTextView.setVisibility(View.GONE);
    }

    private void hideVoiceFeedback() {
        if (commentLayout.getVisibility() == View.VISIBLE && textCommentLayout.getVisibility() == View.GONE) {
            commentLayout.setVisibility(View.GONE);
            submitButton.setVisibility(View.GONE);
            moreTextView.setVisibility(View.VISIBLE);
        }
    }

    private void openRecordLayout() {
        commentLayout.setVisibility(View.VISIBLE);
        textCommentLayout.setVisibility(View.GONE);
        submitButton.setVisibility(View.VISIBLE);
        moreTextView.setVisibility(View.GONE);
        earnExtraTextView.setVisibility(View.GONE);
    }

    private boolean isAuthorized() {
        BaseActivity baseActivity = (BaseActivity) getActivity();
        if (!baseActivity.hasInternetConnection()) {
            baseActivity.showSnackBar(R.string.internet_connection_failed);
            return false;
        }
        ProfileStatus status = ProfileManager.getInstance(this.getActivity()).checkProfile();
        if (status.equals(ProfileStatus.NOT_AUTHORIZED) || status.equals(ProfileStatus.NO_PROFILE)) {
            baseActivity.doAuthorization(status);
            return false;
        }
        if (application.isBlocked(post.getAuthorId())) {
            baseActivity.showWarningDialog(String.format(getResources().getString(R.string.blocked_msg), "rate"));
            return false;
        }
        return true;
    }

    private void submitCommentFeedback(Comment detailed_comment) {
        detailed_comment.setAuthorId(firebaseAuth.getCurrentUser().getUid());

        OnTaskCompleteListener listener = (success) -> {
                if (getActivity() != null) ((BaseActivity) getActivity()).hideProgress();
                if (success) {
                    dismiss();
                } else {
                    ((BaseActivity) getActivity()).showSnackBar(R.string.error_fail_create_detailed_feedback);
                }
        };
        ((BaseActivity) getActivity()).showProgress(R.string.message_submit_detailed_feedback);
        if (commentRecordLayout.getRecordItem() != null) {
            Uri audioUri = Uri.fromFile(new File(commentRecordLayout.getRecordItem().getFilePath()));
            commentManager.createOrUpdateCommentWithAudio(audioUri, detailed_comment, post.getId(), listener);
        } else {
            commentManager.createComment(detailed_comment, post.getId(), listener);
        }
    }

    private void submitDetailedFeedback() {
        if (!isAuthorized()) return;
        boolean error = false;
        if (ratingBar.getProgress() == 0 && post.getRatingsCount() < 10) {
            ratingTextView.setError("Rating is not set.");
            error = true;
        } else if (ratingBar.getProgress() == 0 && post.getRatingsCount()==15) {
            ratingTextView.setError(null);
            error = false;
        }
        else {
            ratingTextView.setError(null);
        }
        String commentText = mistakesTextView.getText().toString();
        if (commentRecordLayout.getRecordItem() == null && commentText.isEmpty()) {
            if (ratingBar.getProgress() <= 10) {
                recordErrorTextView.setText(R.string.mandatory_voice_feedback_error);
            } else {
                recordErrorTextView.setText(R.string.detailed_feedback_error);
            }
            error = true;
        }
        if (error) return;
        Comment comment = new Comment();
        if (!commentText.isEmpty()) comment.setText(commentText);
        comment.setCreatedDate(Calendar.getInstance().getTimeInMillis());
        if (ratingBar.getProgress() > 0 && ratingBar.getProgress() <= 10) {
            ratingController.handleRatingClickAction((BaseActivity) getActivity(), post, ratingBar.getProgress());
        } else {
            comment.setDetailedFeedback(true);
        }
        submitCommentFeedback(comment);
        return;
//        mistakesTextView.setError(null);
//        if (ratingBar.getProgress() == 0) {
//            ratingTextView.setError("Rating is not set.");
//            error = true;
//        } else {
//            ratingTextView.setError(null);
//        }
//        if (melodyRadioGroup.getCheckedRadioButtonId() == -1) {
//            melodyPercentageLabel.setError("'Tone Match %' is not set.");
//            error = true;
//        } else {
//            melodyPercentageLabel.setError(null);
//        }
//
//        if (voiceQualityRadioGroup.getCheckedRadioButtonId() == -1) {
//            voiceQualityLabel.setError("'Voice Quality is not set.");
//            error = true;
//        } else {
//            voiceQualityLabel.setError(null);
//        }
//
//        RadioButton selectedVoiceQuality = voiceQualityRadioGroup.findViewById(voiceQualityRadioGroup.getCheckedRadioButtonId());
//        String ratingDetailedText = String.format(getString(R.string.rating_detailed_text),
//                getMelodyText(),
//                selectedVoiceQuality.getText(),
//                getProblems());
//
//        String commentText = mistakesTextView.getText().toString();
//        if (!commentText.isEmpty() || commentRecordLayout.getRecordItem() != null) {
//            submitCommentFeedback();
//            // extra marks only on rating create
//            ratingController.updateDetailedText(ratingDetailedText, new OnTaskCompleteListener() {
//                @Override
//                public void onTaskComplete(boolean success) {}
//            });
//        } else {
//            // extra marks only on rating create
//            ratingController.updateDetailedText(ratingDetailedText, new OnTaskCompleteListener() {
//                @Override
//                public void onTaskComplete(boolean success) {
//                    if (getActivity() != null) ((BaseActivity) getActivity()).hideProgress();
//                    if (success) {
//                        dismiss();
//                    } else {
//                        ((BaseActivity) getActivity()).showSnackBar(R.string.error_fail_create_detailed_feedback);
//                    }
//                }
//            });
//        }
    }

//    private CharSequence getProblems() {
//        StringBuffer problems = new StringBuffer();
//        if (harkateView.isChecked()) {
//            problems.append(harkateView.getText());
//        }
//        if (pronounciationView.isChecked()) {
//            if (problems.length() > 0) {
//                problems.append(", ");
//            }
//            problems.append(pronounciationView.getText());
//        }
//        if (highPitchView.isChecked()) {
//            if (problems.length() > 0) {
//                problems.append(", ");
//            }
//            problems.append(highPitchView.getText());
//        }
//        if (feelView.isChecked()) {
//            if (problems.length() > 0) {
//                problems.append(", ");
//            }
//            problems.append(feelView.getText());
//        }
//        if (problems.length() > 0) {
//            return "\nProblems: " + problems.toString();
//        }
//        return "";
//    }

//    private CharSequence getMelodyText() {
//        int selectedMelodyRadioId = melodyRadioGroup.getCheckedRadioButtonId();
//        RadioButton selectedButton = melodyRadioGroup.findViewById(selectedMelodyRadioId);
//        return selectedButton.getText();
//    }

    @Override
    public void onStart() {
        super.onStart();

        //set transparent background
        Window window = getDialog().getWindow();
        window.setBackgroundDrawableResource(android.R.color.transparent);

        //disable buttons from dialog
        AlertDialog alertDialog = (AlertDialog) getDialog();
        alertDialog.setCanceledOnTouchOutside(false);
        alertDialog.getButton(Dialog.BUTTON_POSITIVE).setEnabled(false);
        alertDialog.getButton(Dialog.BUTTON_NEGATIVE).setEnabled(false);
        alertDialog.getButton(Dialog.BUTTON_NEUTRAL).setEnabled(false);
        if (Util.SDK_INT > 23) {
            initializePlayer();
            commentRecordLayout.initializePlayer();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if ((Util.SDK_INT <= 23 || player == null)) {
            initializePlayer();
        }
        if ((Util.SDK_INT <= 23 || commentRecordLayout.getPlayer() == null)) {
            commentRecordLayout.initializePlayer();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (Util.SDK_INT <= 23) {
            releasePlayer();
            commentRecordLayout.releasePlayer();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (Util.SDK_INT > 23) {
            releasePlayer();
            commentRecordLayout.releasePlayer();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        commentRecordLayout.deleteCommentAudioFile();
    }

    private void initializePlayer() {
        if (startTimePlayer == 0) startTimePlayer = new Date().getTime();
        player = ExoPlayerFactory.newSimpleInstance(
                new DefaultRenderersFactory(getActivity()),
                new DefaultTrackSelector(), new DefaultLoadControl());

        playerView.setPlayer(player);
        playerView.setControllerHideOnTouch(false);
        player.addListener(componentListener);

        player.setPlayWhenReady(playWhenReady);
        player.seekTo(currentWindow, playbackPosition);
        Uri uri = Uri.parse(item.getFilePath());
        // play from fileSystem
        MediaSource mediaSource;
        mediaSource = buildMediaSource(uri);
        player.prepare(mediaSource, false, false);
    }

    private MediaSource buildMediaSource(Uri uri) {
        return new ExtractorMediaSource.Factory(
                new DefaultHttpDataSourceFactory("eriyaz.social-exoplayer")).
                createMediaSource(uri);
    }

    private void releasePlayer() {
        if (player != null) {
            playbackPosition = player.getCurrentPosition();
            currentWindow = player.getCurrentWindowIndex();
            playWhenReady = player.getPlayWhenReady();
            player.removeListener(componentListener);
            player.release();
            player = null;
        }
    }


    private void updateRatingDetails() {
        intialRatingValue = (int) rating.getRating();
        ratingController = new RatingController(ratingBar, post.getId(), rating);
//        ratingBar.setCustomSectionTextArray(new BubbleSeekBar.CustomSectionTextArray() {
//            @NonNull
//            @Override
//            public SparseArray<String> onCustomize(int sectionCount, @NonNull SparseArray<String> array) {
//                array.clear();
////                array.put(0,"0");
//                array.put(1, "not ok");
////                array.put(2,"5");
//                array.put(3, "ok");
////                array.put(4,"10");
//                array.put(5, "good");
////                array.put(6,"15");
//                array.put(7, "amazing");
////                array.put(8,"20");
//                return array;
//            }
//        });
        ratingBar.setOnProgressChangedListener(new BubbleSeekBar.OnProgressChangedListenerAdapter() {
            @Override
            public void onProgressChanged(BubbleSeekBar bubbleSeekBar, int progress, float progressFloat, boolean fromUser) {
                int color = RatingUtil.getRatingColor(getActivity(), progress);
                bubbleSeekBar.setSecondTrackColor(color);
                bubbleSeekBar.setThumbColor(color);
                bubbleSeekBar.setBubbleColor(color);
            }

            @Override
            public void getProgressOnActionUp(BubbleSeekBar bubbleSeekBar, int progress, float progressFloat) {
                if (!isAuthorized()) {
                    ratingBar.setProgress(rating.getRating());
                    return;
                }

                if (RatingUtil.viewedByAuthor(post)) {
                    showDialog(R.string.rating_self_recording);
                    ratingBar.setProgress(rating.getRating());
                    return;
                }
                // rating first time
                if (!ratingController.isRatingPresent()) {
                    if (player.getPlayWhenReady() && player.getPlaybackState() == Player.STATE_READY) {
                        markPlayerPositions();
                    }
                    if (!application.isPostPlayed(post.getId()) && isListenNotEnough()) {
                        // show warning
                        showDialog(R.string.min_play_record);
                        ratingBar.setProgress(rating.getRating());
                        return;
                    }

                    if (!PreferencesUtil.isUserRatedMany(getActivity())) {
                        PreferencesUtil.incrementUserRatingCount(getActivity());
                    }
                }

                // if new user, check limits
                if (((BaseCurrentProfileActivity) getActivity()).isNewUser()) {
                    // post has rating avg
                    if (post.getAverageRating() > 15 && progress < 14) {
                        showDialog(R.string.new_user_restriction_below_average);
                        ratingBar.setProgress(rating.getRating());
                        return;
                    } else if (progress <= 10) {
                        // post no rating, can't rate below 10
                        showDialog(R.string.new_user_restriction_below_10);
                        ratingBar.setProgress(rating.getRating());
                        return;
                    }
                }

                if (post.isAuthorFirstPost() && progress <= 10) {
                    showDialog(String.format(getString(R.string.first_post_rating_restriction), authorName));
                    ratingBar.setProgress(rating.getRating());
                    return;
                }

                ratingController.setUpdatingRatingCounter(false);
                isRatingChanged = true;
                if (progress > 10 && intialRatingValue == 0) {
                    earnExtraTextView.setVisibility(View.VISIBLE);
                    earnExtraTextView.setText(R.string.earn_extra_point);
                } else {
                    earnExtraTextView.setVisibility(View.GONE);
                }
                moreTextView.setVisibility(View.VISIBLE);

                if (progress > 0 && progress <= 10) {
                    AlertDialog.Builder builder = new BaseAlertDialogBuilder(getActivity());
                    builder.setMessage(getResources().getString(R.string.mandatory_detailed_feedback_popup));
                    builder.setPositiveButton(R.string.button_ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            openRecordLayout();
                        }
                    });
                    builder.show();
                    return;
                } else {
                    hideVoiceFeedback();
                }
                ratingController.handleRatingClickAction((BaseActivity) getActivity(), post, progress);
            }
        });
        ratingBar.setProgress(rating.getRating());
    }

    public boolean isListenNotEnough() {
        if (player.getDuration() == 0 || player.getDuration() < 0) return true;
        if (totalPlayed < Constants.RECORDING.MIN_PLAY_RECORDING*1000
                && player.getDuration() > Constants.RECORDING.MIN_PLAY_RECORDING*1000) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            ratingController.handleRatingClickAction((BaseActivity) getActivity(), post, ratingBar.getProgress());
        } else {
            ratingBar.setProgress(rating.getRating());
        }
    }


    private void showDialog(int messageId) {
        AlertDialog.Builder builder = new BaseAlertDialogBuilder(this.getActivity());
        builder.setMessage(Html.fromHtml(getString(messageId)));
        builder.setPositiveButton(R.string.button_ok, null);
        builder.show();
    }

    private void showDialog(String msg) {
        AlertDialog.Builder builder = new BaseAlertDialogBuilder(this.getActivity());
        builder.setMessage(Html.fromHtml(msg));
        builder.setPositiveButton(R.string.button_ok, null);
        builder.show();
    }

    private long startPosition = 0;
    private long endPosition = 0;
    private long totalPlayed = 0;
    private boolean isPlaying = false;

    private class ComponentListener extends Player.DefaultEventListener {

        @Override
        public void onPlayerStateChanged(boolean playWhenReady,
                                         int playbackState) {
            if (playWhenReady && playbackState == Player.STATE_READY) {
                // fast fwd, don't do anything
                if (isPlaying == true) return;
                getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                startPosition = System.currentTimeMillis();
                endPosition = startPosition;
                isPlaying = true;
            }
            if (playbackState == Player.STATE_ENDED || (!playWhenReady && playbackState == Player.STATE_READY)) {
                markPlayerPositions();
                isPlaying = false;
                getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            }
        }
    }

    private void markPlayerPositions() {
        // player hasn't started yet
        if (startPosition == 0) return;
        endPosition = System.currentTimeMillis();
        totalPlayed = totalPlayed + endPosition - startPosition;
        startPosition = endPosition;
    }

    // Recording Start/Stop
    @SuppressLint("NewApi")
    public void onRecord(){
        if (PermissionsUtil.isExplicitPermissionRequired(getActivity())) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE}, PermissionsUtil.MY_PERMISSIONS_RECORD_AUDIO);
        } else {
            startRecording();
        }
    }

    private void startRecording() {
        if (mStartRecording) {
            mRecordButton.setImageResource(R.drawable.ic_media_stop);
            commentRecordLayout.startRecording();
        } else {
            mRecordButton.setImageResource(R.drawable.ic_mic_white_36dp);
            commentRecordLayout.stopRecording();
        }
        mStartRecording = !mStartRecording;
    }

    //Handling callback
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PermissionsUtil.MY_PERMISSIONS_RECORD_AUDIO: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay!
                    Toast.makeText(getActivity(), "Permissions granted to record audio", Toast.LENGTH_LONG).show();
                    startRecording();
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    showDialog("Permissions Denied to record audio. Please try again.");
                }
                return;
            }
        }
    }
}