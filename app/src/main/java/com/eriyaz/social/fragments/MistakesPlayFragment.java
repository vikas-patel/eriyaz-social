package com.eriyaz.social.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import com.eriyaz.social.Application;
import com.eriyaz.social.R;
import com.eriyaz.social.activities.BaseActivity;
import com.eriyaz.social.activities.PostDetailsActivity;
import com.eriyaz.social.enums.ProfileStatus;
import com.eriyaz.social.managers.ProfileManager;
import com.eriyaz.social.model.Post;
import com.eriyaz.social.model.Rating;
import com.eriyaz.social.model.RecordingItem;
import com.eriyaz.social.utils.TimestampTagUtil;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.ui.SimpleExoPlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.upstream.FileDataSource;
import com.google.android.exoplayer2.util.Util;
import com.google.firebase.auth.FirebaseAuth;
import com.volokh.danylo.hashtaghelper.HashTagHelper;

import java.util.Date;
import java.util.List;

/**
 * Created by Daniel on 1/1/2015.
 */
public class MistakesPlayFragment extends DialogFragment {

    public static final String RECORDING_ITEM = "recording_item";
    private static final String MISTAKES_TEXT = "mistakes_text";
    private static final String TIMESTAMP = "timestamp";
    private RecordingItem item;
    private Button closeButton = null;
    private TextView mFileNameTextView = null;
    private TextView mistakesCommentTextView = null;
    private String mistakesCommentText = "";
    private long startTimePlayer;
    private PlayerView playerView;
    private SimpleExoPlayer player;
    private int currentWindow = 0;
    private long playbackPosition = 0;
    private boolean playWhenReady = false;
    private ComponentListener componentListener;
    private FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
    private Application application;
    private HashTagHelper mistakesTextHashTagHelper;

    public MistakesPlayFragment newInstance(RecordingItem item) {
        MistakesPlayFragment f = new MistakesPlayFragment();
        Bundle b = new Bundle();
        b.putParcelable(RECORDING_ITEM, item);
        f.setArguments(b);
        return f;
    }

    public MistakesPlayFragment newInstance(RecordingItem item, Post post, Rating rating, String mistakesCommentText, String timestamp) {
        MistakesPlayFragment f = new MistakesPlayFragment();
        Bundle b = new Bundle();
        b.putParcelable(RECORDING_ITEM, item);
        b.putSerializable(PostDetailsActivity.POST_ID_EXTRA_KEY, post);
        b.putSerializable(Rating.RATING_ID_EXTRA_KEY, rating);
        b.putSerializable(MISTAKES_TEXT , mistakesCommentText);
        b.putSerializable(TIMESTAMP , timestamp);
        f.setArguments(b);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        item = getArguments().getParcelable(RECORDING_ITEM);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        BaseActivity activity = (BaseActivity) getActivity();
        activity.getAnalytics().logOpenRecordedAudio();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        Dialog dialog = super.onCreateDialog(savedInstanceState);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        View view = getActivity().getLayoutInflater().inflate(R.layout.fragment_mistakes_play, null);

        mFileNameTextView = (TextView) view.findViewById(R.id.file_name_text_view);


        closeButton = view.findViewById(R.id.closeButton);
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onCloseButton();
            }
        });

        mFileNameTextView.setText(item.getName());

        builder.setView(view);
        // request a window without the title
        dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        componentListener = new ComponentListener();
        playerView = view.findViewById(R.id.exoPlayerView);
        //            player.seekTo(TimestampTagUtil.timestampToMillis(getArguments().getString(TIMESTAMP)));
        playbackPosition = TimestampTagUtil.timestampToMillis(getArguments().getString(TIMESTAMP));

        mistakesCommentTextView = (TextView) view.findViewById(R.id.mistakes_comment_text);
        mistakesCommentTextView.setText(getArguments().getString(MISTAKES_TEXT));


        mistakesTextHashTagHelper = HashTagHelper.Creator.create(getResources().getColor(R.color.red), new HashTagHelper.OnHashTagClickListener() {
            @Override
            public void onHashTagClicked(String hashTag) {
                if(TimestampTagUtil.isValidTimestamp(hashTag)) {
                    player.seekTo(TimestampTagUtil.timestampToMillis(hashTag));
                }
            }
        }, new char[] {':'});
        mistakesTextHashTagHelper.handle(mistakesCommentTextView);

        return builder.create();
    }

    private void onCloseButton() {
        dismiss();
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
        return true;
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
        getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
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

        setTimelineMarkers();

        player.setPlayWhenReady(playWhenReady);
        player.seekTo(currentWindow, playbackPosition);


        Uri uri = Uri.parse(item.getFilePath());


        MediaSource mediaSource = buildMediaSource(uri);
        player.prepare(mediaSource, true, false);
    }

    private void setTimelineMarkers() {
        List<String> timestamps = mistakesTextHashTagHelper.getAllHashTags();
        for (String timestamp : timestamps) {
            if(!TimestampTagUtil.isValidTimestamp(timestamp)) {
                timestamps.remove(timestamp);
            }
        }
        long[] adTimes = new long[timestamps.size()];
        boolean[] adPlayed = new boolean[timestamps.size()];
        for (int i=0; i<timestamps.size(); i++) {
            adTimes[i] = TimestampTagUtil.timestampToMillis(timestamps.get(i));
        }
        playerView.setExtraAdGroupMarkers(adTimes, adPlayed);
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
}