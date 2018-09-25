package com.eriyaz.social.views;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Environment;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Chronometer;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.eriyaz.social.R;
import com.eriyaz.social.model.RecordingItem;
import com.eriyaz.social.services.RecordingService;
import com.eriyaz.social.utils.LogUtil;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.ui.SimpleExoPlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.upstream.FileDataSource;

import java.io.File;

/**
 * Created by vikas on 29/7/18.
 */

public class RecordLayout extends LinearLayout implements RecordingService.RecordListener{

    // Recorder
    private TextView mRecordingPrompt;
    private int mRecordPromptCount = 0;
    private int currentWindow = 0;
    private long playbackPosition = 0;
    private Chronometer mChronometer = null;
    private RecordingService recordingService;
    private SimpleExoPlayerView commentPlayerView;
    private SimpleExoPlayer commentPlayer;
    private RecordingItem commentRecordItem;
    private Activity mActivity;
    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've binded to LocalService, cast the IBinder and get LocalService instance
            RecordingService.LocalBinder binder = (RecordingService.LocalBinder) service;
            recordingService = binder.getServiceInstance(); //Get instance of your service!
            recordingService.registerClient(RecordLayout.this); //Activity register in the service as client for callabcks!
            //tvServiceState.setText("Connected to service...");
            //tbStartTask.setEnabled(true);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {

        }
    };

    public RecordLayout(Context context) {
        super(context);
        initializeViews(context);
    }

    public RecordLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        initializeViews(context);
    }

    public RecordLayout(Context context,
                       AttributeSet attrs,
                       int defStyle) {
        super(context, attrs, defStyle);
        initializeViews(context);
    }

    private void initializeViews(Context context) {
        mActivity = (Activity) getContext();
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.layout_recorder, this);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        // Sets the images for the previous and next buttons. Uses
        // built-in images so you don't need to add images, but in
        // a real application your images should be in the
        // application package so they are always available.

        commentPlayerView = findViewById(R.id.exoPlayerCommentView);
        mRecordingPrompt = findViewById(R.id.recording_status_text);
        mChronometer = findViewById(R.id.chronometer);
    }

    public void startRecording() {
        // start recording
        deleteCommentAudioFile();
        commentPlayerView.setVisibility(View.GONE);
        mRecordingPrompt.setVisibility(View.VISIBLE);
        mChronometer.setVisibility(View.VISIBLE);
        //mPauseButton.setVisibility(View.VISIBLE);
        Toast.makeText(mActivity,R.string.toast_recording_start, Toast.LENGTH_SHORT).show();
        File folder = new File(Environment.getExternalStorageDirectory() + "/" + getResources().getString(R.string.app_name));
        if (!folder.exists()) {
            //folder /RateMySinging doesn't exist, create the folder
            folder.mkdir();
        }

        //start Chronometer
        mChronometer.setBase(SystemClock.elapsedRealtime());
        mChronometer.start();
        mChronometer.setOnChronometerTickListener(new Chronometer.OnChronometerTickListener() {
            @Override
            public void onChronometerTick(Chronometer chronometer) {
                if (mRecordPromptCount == 0) {
                    mRecordingPrompt.setText(mActivity.getString(R.string.record_in_progress) + ".");
                } else if (mRecordPromptCount == 1) {
                    mRecordingPrompt.setText(mActivity.getString(R.string.record_in_progress) + "..");
                } else if (mRecordPromptCount == 2) {
                    mRecordingPrompt.setText(mActivity.getString(R.string.record_in_progress) + "...");
                    mRecordPromptCount = -1;
                }

                mRecordPromptCount++;
            }
        });
        final Intent intent = new Intent(mActivity, RecordingService.class);
        intent.putExtra(RecordingService.LOW_QUALITY_EXTRA_KEY, true);
        //start RecordingService
        mActivity.startService(intent);
        mActivity.bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        //keep screen on while recording
        mActivity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mRecordingPrompt.setText(mActivity.getString(R.string.record_in_progress) + ".");
        mRecordPromptCount++;
    }

    public void stopRecording() {
        final Intent intent = new Intent(mActivity, RecordingService.class);
        intent.putExtra(RecordingService.LOW_QUALITY_EXTRA_KEY, true);
        mChronometer.setVisibility(View.GONE);
        mRecordingPrompt.setVisibility(View.GONE);
        mChronometer.stop();
        mChronometer.setBase(SystemClock.elapsedRealtime());
        mRecordingPrompt.setText(mActivity.getString(R.string.record_prompt));

        mActivity.stopService(intent);
        mActivity.unbindService(mConnection);
        //allow the screen to turn off again once recording is finished
        mActivity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    public void onIntentResult(RecordingItem item) {
        if (mActivity != null) {
            commentRecordItem = item;
            commentPlayerView.setVisibility(View.VISIBLE);
            initializePlayer();
        }
    }

    public void initializePlayer() {
        if (commentRecordItem == null) return;
        commentPlayer = ExoPlayerFactory.newSimpleInstance(
                new DefaultRenderersFactory(mActivity),
                new DefaultTrackSelector(), new DefaultLoadControl());

        commentPlayerView.setPlayer(commentPlayer);
        commentPlayerView.setControllerHideOnTouch(false);
//        commentPlayer.addListener(componentListener);

        commentPlayer.setPlayWhenReady(false);
        commentPlayer.seekTo(currentWindow, playbackPosition);
        Uri uri = Uri.parse(commentRecordItem.getFilePath());
        // play from fileSystem
        MediaSource mediaSource = buildMediaSourceFromFileUrl(uri);
        commentPlayer.prepare(mediaSource, false, false);
    }

    public void releasePlayer() {
        if (commentPlayer != null) {
            playbackPosition = commentPlayer.getCurrentPosition();
            currentWindow = commentPlayer.getCurrentWindowIndex();
            commentPlayer.release();
            commentPlayer = null;
        }
    }

    public SimpleExoPlayer getPlayer() {
        return commentPlayer;
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

    public void deleteCommentAudioFile() {
        if (commentRecordItem != null) {
            File file = new File(commentRecordItem.getFilePath());
            file.delete();
            commentRecordItem = null;
        }
    }

    public RecordingItem getRecordItem() {
        return commentRecordItem;
    }

    public void reset() {
        commentPlayerView.setVisibility(View.GONE);
        mChronometer.setVisibility(View.GONE);
        mRecordingPrompt.setVisibility(View.GONE);
        deleteCommentAudioFile();
    }
}
