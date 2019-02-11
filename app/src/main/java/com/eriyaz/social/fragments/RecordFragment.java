package com.eriyaz.social.fragments;

import android.Manifest;
import android.annotation.SuppressLint;
import android.support.v4.app.Fragment;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.IBinder;
import android.support.v7.app.AlertDialog;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.eriyaz.social.Constants;
import com.eriyaz.social.R;
import com.eriyaz.social.activities.BaseActivity;
import com.eriyaz.social.activities.BaseAlertDialogBuilder;
import com.eriyaz.social.activities.CreatePostActivity;
import com.eriyaz.social.managers.listeners.OnRecordingEndListener;
import com.eriyaz.social.model.RecordingItem;
import com.eriyaz.social.services.RecordingService;
import com.eriyaz.social.utils.FormatterUtil;
import com.eriyaz.social.utils.PermissionsUtil;
import com.google.android.exoplayer2.util.Util;
import com.melnykov.fab.FloatingActionButton;

import java.io.File;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * to handle interaction events.
 * Use the {@link RecordFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class RecordFragment extends BaseFragment implements RecordingService.RecordListener{
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    //private static final String ARG_POSITION = "position";
    private static final String LOG_TAG = RecordFragment.class.getSimpleName();

    //private int position;

    //Recording controls
    private FloatingActionButton mRecordButton = null;
    private Button mPauseButton = null;
    private CountDownTimer countDownTimer;

    private TextView mRecordingPrompt;
    private TextView mToggleRecordingText;
    private TextView mCountDownText;
    private ProgressBar recordProgressBar;
    private int mRecordPromptCount = 0;

    private boolean mStartRecording = true;
    private boolean mPauseRecording = true;
    private boolean isShortRecording = true;

//    private Chronometer mChronometer = null;
    long timeWhenPaused = 0; //stores time when user clicks pause button
    private RecordingService recordingService;

    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've binded to LocalService, cast the IBinder and get LocalService instance
            RecordingService.LocalBinder binder = (RecordingService.LocalBinder) service;
            recordingService = binder.getServiceInstance(); //Get instance of your service!
            recordingService.registerClient(RecordFragment.this); //Activity register in the service as client for callabcks!
            //tvServiceState.setText("Connected to service...");
            //tbStartTask.setEnabled(true);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {

        }
    };
    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment Record_Fragment.
     */
    public static RecordFragment newInstance() {
        RecordFragment f = new RecordFragment();
        Bundle b = new Bundle();
        f.setArguments(b);
        return f;
    }

    public RecordFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View recordView = inflater.inflate(R.layout.fragment_record, container, false);
        mCountDownText = (TextView) recordView.findViewById(R.id.countDownTimer);

//        mChronometer = (Chronometer) recordView.findViewById(R.id.chronometer);
        //update recording prompt text
        mRecordingPrompt = (TextView) recordView.findViewById(R.id.recording_status_text);
        mToggleRecordingText = (TextView) recordView.findViewById(R.id.toggle_recording_type);

        mRecordButton = (FloatingActionButton) recordView.findViewById(R.id.btnRecord);
        mRecordButton.setColorNormal(getResources().getColor(R.color.primary));
        mRecordButton.setColorPressed(getResources().getColor(R.color.primary_dark));
        mRecordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onRecord();
            }
        });
        recordProgressBar = recordView.findViewById(R.id.recordProgressBar);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            recordProgressBar.setVisibility(View.INVISIBLE);
        }

        mToggleRecordingText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isShortRecording) {
                    openConfirmSwitchDialog();
                } else {
                    mToggleRecordingText.setText(getResources().getString(R.string.toggle_longer_recording));
                    mCountDownText.setText(Constants.RECORDING.DEFAULT_RECORDING_TEXT);
                }
                isShortRecording = !isShortRecording;
            }
        });

//        mPauseButton = (Button) recordView.findViewById(R.id.btnPause);
//        mPauseButton.setVisibility(View.GONE); //hide pause button before recording starts
//        mPauseButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                onPauseRecord(mPauseRecording);
//                mPauseRecording = !mPauseRecording;
//            }
//        });

        return recordView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        BaseActivity activity = (BaseActivity) getActivity();
        activity.getAnalytics().logRecording();
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
                    showWarningDialog("Permissions Denied to record audio. Please try again.");
                }
                return;
            }
        }
    }

    private void startRecording() {
        final Intent intent = new Intent(getActivity(), RecordingService.class);
        //remove log
        if (mStartRecording) {
            // start recording
            mRecordButton.setImageResource(R.drawable.ic_media_stop);
            //mPauseButton.setVisibility(View.VISIBLE);
            Toast.makeText(getActivity(),R.string.toast_recording_start, Toast.LENGTH_SHORT).show();
            File folder = new File(Environment.getExternalStorageDirectory() + "/" + getResources().getString(R.string.app_name));
            if (!folder.exists()) {
                //folder /RateMySinging doesn't exist, create the folder
                folder.mkdir();
            }

            //start Chronometer
            countDownTimer = new CountDownTimer(isShortRecording ? Constants.RECORDING.DEFAULT_RECORDING*1000 : Constants.RECORDING.LARGE_RECORDING*1000, 1000){
                public void onTick(long millisUntilFinished){
                    mCountDownText.setText(FormatterUtil.countDownFormat(millisUntilFinished));
                    if (mRecordPromptCount == 0) {
                        mRecordingPrompt.setText(getString(R.string.record_in_progress) + ".");
                    } else if (mRecordPromptCount == 1) {
                        mRecordingPrompt.setText(getString(R.string.record_in_progress) + "..");
                    } else if (mRecordPromptCount == 2) {
                        mRecordingPrompt.setText(getString(R.string.record_in_progress) + "...");
                        mRecordPromptCount = -1;
                    }
                    mRecordPromptCount++;
                }
                public  void onFinish(){
                    mStartRecording = !mStartRecording;
                    stopRecording(intent);
                }
            }.start();

            //start RecordingService
            getActivity().startService(intent);
            getActivity().bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
            //keep screen on while recording
            getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

            mRecordingPrompt.setText(getString(R.string.record_in_progress) + ".");
            mRecordPromptCount++;

        } else {
            //stop recording
            stopRecording(intent);

        }
        mStartRecording = !mStartRecording;
    }

    private void stopRecording(Intent intent) {
        mRecordButton.setImageResource(R.drawable.ic_mic_white_36dp);
        if (countDownTimer != null) countDownTimer.cancel();
        //mPauseButton.setVisibility(View.GONE);
//            mChronometer.stop();
//            mChronometer.setBase(SystemClock.elapsedRealtime());
        timeWhenPaused = 0;
        mRecordingPrompt.setText(getString(R.string.record_prompt));

        getActivity().stopService(intent);
        getActivity().unbindService(mConnection);
        //allow the screen to turn off again once recording is finished
        getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    public void onIntentResult(RecordingItem item) {
        if (getActivity() != null) {
            ((OnRecordingEndListener)getActivity()).onRecordEnd(item);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (Util.SDK_INT <= 23) {
            resetRecording();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (Util.SDK_INT > 23) {
            resetRecording();
        }
    }

    private void resetRecording() {
        if (mConnection != null && !mStartRecording) {
            Intent intent = new Intent(getActivity(), RecordingService.class);
            stopRecording(intent);
            mCountDownText.setText(Constants.RECORDING.DEFAULT_RECORDING_TEXT);
            mStartRecording = !mStartRecording;
        }
    }

    // Recording Start/Stop
    @SuppressLint("NewApi")
    private void onRecord(){
        if (PermissionsUtil.isExplicitPermissionRequired(getActivity())) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE}, PermissionsUtil.MY_PERMISSIONS_RECORD_AUDIO);
        } else {
            startRecording();
        }

    }

    private void openConfirmSwitchDialog() {
        CreatePostActivity rootActivity = (CreatePostActivity) getActivity();
        AlertDialog.Builder builder = new BaseAlertDialogBuilder(this.getActivity());
        builder.setMessage(Html.fromHtml(getString(R.string.confirm_longer_recording)))
                .setNegativeButton(R.string.button_title_cancel, null)
                .setPositiveButton(R.string.button_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        final int userPoints = (int) rootActivity.getProfile().getPoints();
                        if (userPoints < Constants.POINT.LARGE_RECORDING) {
                            AlertDialog.Builder builder = new BaseAlertDialogBuilder(getActivity());
                            builder.setMessage(getResources().getQuantityString(R.plurals.points_needed_longer_recording, userPoints, userPoints));
                            builder.setPositiveButton(R.string.button_ok, null);
                            builder.show();
                            return;
                        }
                        mToggleRecordingText.setText(getResources().getString(R.string.toggle_shorter_recording));
                        mCountDownText.setText(Constants.RECORDING.LARGE_RECORDING_TEXT);
                    }
                });

        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }

    //TODO: implement pause recording
//    private void onPauseRecord(boolean pause) {
//        if (pause) {
//            //pause recording
//            mPauseButton.setCompoundDrawablesWithIntrinsicBounds
//                    (R.drawable.ic_media_play ,0 ,0 ,0);
//            mRecordingPrompt.setText((String)getString(R.string.resume_recording_button).toUpperCase());
//            timeWhenPaused = mChronometer.getBase() - SystemClock.elapsedRealtime();
//            mChronometer.stop();
//        } else {
//            //resume recording
//            mPauseButton.setCompoundDrawablesWithIntrinsicBounds
//                    (R.drawable.ic_media_pause ,0 ,0 ,0);
//            mRecordingPrompt.setText((String)getString(R.string.pause_recording_button).toUpperCase());
//            mChronometer.setBase(SystemClock.elapsedRealtime() + timeWhenPaused);
//            mChronometer.start();
//        }
//    }
}