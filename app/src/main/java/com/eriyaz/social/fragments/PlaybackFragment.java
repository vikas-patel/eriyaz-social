package com.eriyaz.social.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.SparseArray;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import com.eriyaz.social.R;
import com.eriyaz.social.activities.BaseActivity;
import com.eriyaz.social.activities.MainActivity;
import com.eriyaz.social.activities.PostDetailsActivity;
import com.eriyaz.social.activities.ProfileActivity;
import com.eriyaz.social.controllers.RatingController;
import com.eriyaz.social.dialogs.CommentDialog;
import com.eriyaz.social.model.Post;
import com.eriyaz.social.model.Rating;
import com.eriyaz.social.model.RecordingItem;
import com.eriyaz.social.utils.RatingUtil;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.ui.SimpleExoPlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.upstream.FileDataSource;
import com.google.android.exoplayer2.util.Util;
import com.xw.repo.BubbleSeekBar;

import java.util.Date;

/**
 * Created by Daniel on 1/1/2015.
 */
public class PlaybackFragment extends DialogFragment {

    public static final String RECORDING_ITEM = "recording_item";
    private RecordingItem item;
    private Button closeButton = null;
    private TextView mFileNameTextView = null;
    private BubbleSeekBar ratingBar;
    private Post post;
    private Rating rating;
    private RatingController ratingController;
    private boolean isRatingChanged = false;
    private View ratingLayout;
    private int maxPlayedTime;
    private long startTimePlayer;
    private SimpleExoPlayerView playerView;
    private SimpleExoPlayer player;
    private int currentWindow = 0;
    private long playbackPosition = 0;
    private boolean playWhenReady = true;
    private ComponentListener componentListener;

    public PlaybackFragment newInstance(RecordingItem item) {
        PlaybackFragment f = new PlaybackFragment();
        Bundle b = new Bundle();
        b.putParcelable(RECORDING_ITEM, item);
        f.setArguments(b);
        return f;
    }

    public PlaybackFragment newInstance(RecordingItem item, Post post, Rating rating) {
        PlaybackFragment f = new PlaybackFragment();
        Bundle b = new Bundle();
        b.putParcelable(RECORDING_ITEM, item);
        b.putSerializable(PostDetailsActivity.POST_ID_EXTRA_KEY, post);
        b.putSerializable(Rating.RATING_ID_EXTRA_KEY, rating);
        f.setArguments(b);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        item = getArguments().getParcelable(RECORDING_ITEM);
        post = (Post) getArguments().getSerializable(PostDetailsActivity.POST_ID_EXTRA_KEY);
        rating = (Rating) getArguments().getSerializable(Rating.RATING_ID_EXTRA_KEY);
        if (rating == null) rating = new Rating();
        // own recording yet to be submitted
        BaseActivity activity = (BaseActivity) getActivity();
        if (post == null) {
            activity.getAnalytics().logOpenRecordedAudio();
        } else {
            activity.getAnalytics().logOpenAudio(post.getAuthorId());
        }
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        if (post != null) {
            BaseActivity activity = (BaseActivity) getActivity();
            maxPlayedTime = Math.max(maxPlayedTime, Math.round(player.getCurrentPosition()/1000));
            activity.getAnalytics().logPlayedTime(post.getAuthorId(), post.getTitle(), maxPlayedTime);
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
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        Dialog dialog = super.onCreateDialog(savedInstanceState);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        View view = getActivity().getLayoutInflater().inflate(R.layout.fragment_media_playback, null);

        mFileNameTextView = (TextView) view.findViewById(R.id.file_name_text_view);
        ratingBar = (BubbleSeekBar) view.findViewById(R.id.ratingBar);
        ratingLayout = view.findViewById(R.id.seekbarContainerLayout);
        closeButton = view.findViewById(R.id.closeButton);
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        mFileNameTextView.setText(item.getName());
        updateRatingDetails();
        builder.setView(view);
        // request a window without the title
        dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        componentListener = new ComponentListener();
        playerView = view.findViewById(R.id.exoPlayerView);

        return builder.create();
    }

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
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if ((Util.SDK_INT <= 23 || player == null)) {
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
    public void onDestroy() {
        super.onDestroy();
        releasePlayer();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (Util.SDK_INT > 23) {
            releasePlayer();
        }
    }

    private void initializePlayer() {
        if (startTimePlayer == 0) startTimePlayer = new Date().getTime();
        player = ExoPlayerFactory.newSimpleInstance(
                new DefaultRenderersFactory(getActivity()),
                new DefaultTrackSelector(), new DefaultLoadControl());

        playerView.setPlayer(player);
        player.addListener(componentListener);

        player.setPlayWhenReady(playWhenReady);
        player.seekTo(currentWindow, playbackPosition);
        Uri uri = Uri.parse(item.getFilePath());
        // play from fileSystem
        MediaSource mediaSource;
        if (post == null) {
            mediaSource = buildMediaSourceFromFileUrl(uri);
        } else {
            mediaSource = buildMediaSource(uri);
        }
        player.prepare(mediaSource, true, false);
    }

    private MediaSource buildMediaSourceFromFileUrl(Uri uri){
        DataSpec dataSpec = new DataSpec(uri);
        final FileDataSource fileDataSource = new FileDataSource();
        try {
            fileDataSource.open(dataSpec);
        } catch (FileDataSource.FileDataSourceException e) {
            e.printStackTrace();
        }

        DataSource.Factory factory = new DataSource.Factory() {
            @Override
            public DataSource createDataSource() {
                return fileDataSource;
            }
        };
        return new ExtractorMediaSource.Factory(factory).createMediaSource(uri);
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
        if (post == null) {
            ratingLayout.setVisibility(View.GONE);
            ratingBar.setVisibility(View.GONE);
            return;
        }
        ratingController = new RatingController(ratingBar, post.getId(), rating);


        ratingBar.setCustomSectionTextArray(new BubbleSeekBar.CustomSectionTextArray() {
            @NonNull
            @Override
            public SparseArray<String> onCustomize(int sectionCount, @NonNull SparseArray<String> array) {
                array.clear();
                array.put(1, "not ok");
                array.put(3, "ok");
                array.put(5, "good");
                array.put(7, "amazing");
                return array;
            }
        });
        ratingBar.setOnProgressChangedListener(new BubbleSeekBar.OnProgressChangedListenerAdapter() {
            @Override
            public void onProgressChanged(BubbleSeekBar bubbleSeekBar, int progress, float progressFloat) {
                int color = RatingUtil.getRatingColor(getActivity(), progress);
                bubbleSeekBar.setSecondTrackColor(color);
                bubbleSeekBar.setThumbColor(color);
                bubbleSeekBar.setBubbleColor(color);
            }

            @Override
            public void getProgressOnActionUp(BubbleSeekBar bubbleSeekBar, int progress, float progressFloat) {
                ratingController.setUpdatingRatingCounter(false);
                isRatingChanged = true;
                if (progress > 0 && progress <= 5) {
                    openCommentDialog();
                    return;
                }
                ratingController.handleRatingClickAction((BaseActivity) getActivity(), post, progress);
            }
        });
        ratingBar.setProgress(rating.getRating());
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


    private void openCommentDialog() {
        CommentDialog commentDialog = new CommentDialog();
        Bundle args = new Bundle();
        args.putString(PostDetailsActivity.POST_ID_EXTRA_KEY, post.getId());
        commentDialog.setArguments(args);
        commentDialog.setTargetFragment(this,CommentDialog.NEW_COMMENT_REQUEST);
        commentDialog.show(getFragmentManager(), CommentDialog.TAG);
    }

    private class ComponentListener extends Player.DefaultEventListener {

        @Override
        public void onPlayerStateChanged(boolean playWhenReady,
                                         int playbackState) {
            if (player != null) {
                maxPlayedTime = Math.max(maxPlayedTime, Math.round(player.getCurrentPosition()/1000));
            }
        }

    }
}

