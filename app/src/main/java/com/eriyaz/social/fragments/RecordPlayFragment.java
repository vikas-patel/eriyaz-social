package com.eriyaz.social.fragments;

import android.app.Dialog;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import com.eriyaz.social.R;
import com.eriyaz.social.activities.BaseActivity;
import com.eriyaz.social.activities.BaseAlertDialogBuilder;
import com.eriyaz.social.activities.PostDetailsActivity;
import com.eriyaz.social.enums.ProfileStatus;
import com.eriyaz.social.managers.ProfileManager;
import com.eriyaz.social.model.Post;
import com.eriyaz.social.model.Rating;
import com.eriyaz.social.model.RecordingItem;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.ui.SimpleExoPlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.upstream.FileDataSource;
import com.google.android.exoplayer2.util.Util;

import java.util.Date;

/**
 * Created by Daniel on 1/1/2015.
 */
public class RecordPlayFragment extends BaseDialogFragment {

    public static final String RECORDING_ITEM = "recording_item";
    private RecordingItem item;
    private Button closeButton = null;
    private TextView mFileNameTextView = null;
    private long startTimePlayer;
    private SimpleExoPlayerView playerView;
    private SimpleExoPlayer player;
    private int currentWindow = 0;
    private long playbackPosition = 0;
    private boolean playWhenReady = true;
    private ComponentListener componentListener;

    public static RecordPlayFragment newInstance(RecordingItem item) {
        RecordPlayFragment f = new RecordPlayFragment();
        Bundle b = new Bundle();
        b.putParcelable(RECORDING_ITEM, item);
        f.setArguments(b);
        return f;
    }

    public static RecordPlayFragment newInstance(RecordingItem item, Post post, Rating rating) {
        RecordPlayFragment f = new RecordPlayFragment();
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

        AlertDialog.Builder builder = new BaseAlertDialogBuilder(getActivity());
        View view = getActivity().getLayoutInflater().inflate(R.layout.fragment_record_play, null);

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
        playerView.setControllerHideOnTouch(false);
        player.addListener(componentListener);

        player.setPlayWhenReady(playWhenReady);
        player.seekTo(currentWindow, playbackPosition);
        Uri uri = Uri.parse(item.getFilePath());
        // play from fileSystem
        MediaSource mediaSource;
        if (item.isServer()) {
            mediaSource = buildMediaSource(uri);
        } else {
            mediaSource = buildMediaSourceFromFileUrl(uri);
        }
        player.prepare(mediaSource, false, false);
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