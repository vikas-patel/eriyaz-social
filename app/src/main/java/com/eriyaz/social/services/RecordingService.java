package com.eriyaz.social.services;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.MediaRecorder;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.eriyaz.social.R;
import com.eriyaz.social.activities.CreatePostActivity;
import com.eriyaz.social.model.RecordingItem;
import com.eriyaz.social.utils.LogUtil;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Daniel on 12/28/2014.
 */
public class RecordingService extends Service {

    private static final String LOG_TAG = "RecordingService";
    public static final String LOW_QUALITY_EXTRA_KEY = "RecordingService.LOW_QUALITY_EXTRA_KEY";

    private String mFileName = null;
    private String mFilePath = null;

    private MediaRecorder mRecorder = null;

//    private DBHelper mDatabase;

    private long mStartingTimeMillis = 0;
    private long mElapsedMillis = 0;
    private int mElapsedSeconds = 0;
    private OnTimerChangedListener onTimerChangedListener = null;
    private static final SimpleDateFormat mTimerFormat = new SimpleDateFormat("mm:ss", Locale.getDefault());

    private Timer mTimer = null;
    private TimerTask mIncrementTimerTask = null;
    private final IBinder binder = (IBinder) new LocalBinder();
    private RecordListener listener;


    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }
    //returns the instance of the service
    public class LocalBinder extends Binder {
        public RecordingService getServiceInstance(){
            return RecordingService.this;
        }
    }
    //Here Activity register to the service as Callbacks client
    public void registerClient(RecordListener activity){
        this.listener = activity;
    }

    public interface OnTimerChangedListener {
        void onTimerChanged(int seconds);
    }

    @Override
    public void onCreate() {
        super.onCreate();
//        mDatabase = new DBHelper(getApplicationContext());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        boolean isLowQuality = false;
        if (intent != null) isLowQuality = intent.getBooleanExtra(LOW_QUALITY_EXTRA_KEY, false);
        startRecording(isLowQuality);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (mRecorder != null) {
            stopRecording();
        }

        super.onDestroy();
    }

    public void startRecording(boolean isLowQuality) {
        setFileNameAndPath();

        mRecorder = new MediaRecorder();
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mRecorder.setOutputFile(mFilePath);
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mRecorder.setAudioChannels(1);
        mRecorder.setAudioSamplingRate(44100);
        if (isLowQuality) {
            mRecorder.setAudioEncodingBitRate(32000);
        } else {
            mRecorder.setAudioEncodingBitRate(128000);
        }

        try {
            mRecorder.prepare();
            //startTimer();
            //startForeground(1, createNotification());
        } catch (IOException e) {
            Log.e(LOG_TAG, "prepare() failed" + e.getMessage());
        }
        mRecorder.start();
        mStartingTimeMillis = System.currentTimeMillis();
    }

    public void setFileNameAndPath(){
        int count = 0;
        File f;

        do{
            count++;

            mFileName = getString(R.string.default_file_name) + new Date().getTime()
                    //+ "_" + (mDatabase.getCount() + count)
                    + ".mp4";
            mFilePath = Environment.getExternalStorageDirectory().getAbsolutePath();
            mFilePath += "/" + getResources().getString(R.string.app_name) + "/" + mFileName;

            f = new File(mFilePath);
        }while (f.exists() && !f.isDirectory());
    }

    public void stopRecording() {
        try {
            mRecorder.stop();
        } catch (RuntimeException e) {
            // RuntimeException is thrown when stop() is called immediately after start().
            // In this case the output file is not properly constructed ans should be deleted.
            LogUtil.logError(LOG_TAG, "RuntimeException: stop() is called immediately after start()", e);
            new File (mFilePath).delete();
            mRecorder.release();
            mRecorder = null;
            return;
            //noinspection ResultOfMethodCallIgnored
        }
        mElapsedMillis = (System.currentTimeMillis() - mStartingTimeMillis);
        mRecorder.release();

        //remove notification
//        if (mIncrementTimerTask != null) {
//            mIncrementTimerTask.cancel();
//            mIncrementTimerTask = null;
//        }

        mRecorder = null;
        RecordingItem recordingItem = new RecordingItem(mFileName, mFilePath, mElapsedMillis);
        if (listener != null) listener.onIntentResult(recordingItem);

//        try {
//            mDatabase.addRecording(mFileName, mFilePath, mElapsedMillis);
//
//        } catch (Exception e){
//            Log.e(LOG_TAG, "exception", e);
//        }+
    }

    private void startTimer() {
        mTimer = new Timer();
        mIncrementTimerTask = new TimerTask() {
            @Override
            public void run() {
                mElapsedSeconds++;
                if (onTimerChangedListener != null)
                    onTimerChangedListener.onTimerChanged(mElapsedSeconds);
                NotificationManager mgr = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                mgr.notify(1, createNotification());
            }
        };
        mTimer.scheduleAtFixedRate(mIncrementTimerTask, 1000, 1000);
    }

    //TODO:
    private Notification createNotification() {
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(getApplicationContext())
                        .setSmallIcon(R.drawable.ic_mic_white_36dp)
                        .setContentTitle(getString(R.string.notification_recording))
                        .setContentText(mTimerFormat.format(mElapsedSeconds * 1000))
                        .setOngoing(true);

        mBuilder.setContentIntent(PendingIntent.getActivities(getApplicationContext(), 0,
                new Intent[]{new Intent(getApplicationContext(), CreatePostActivity.class)}, 0));

        return mBuilder.build();
    }

    public interface RecordListener {
        void onIntentResult(RecordingItem recordingItem);
    }
}
