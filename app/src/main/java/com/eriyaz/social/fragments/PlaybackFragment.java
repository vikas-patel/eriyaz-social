package com.eriyaz.social.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.ColorFilter;
import android.graphics.LightingColorFilter;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import com.eriyaz.social.R;
import com.eriyaz.social.activities.BaseActivity;
import com.eriyaz.social.activities.MainActivity;
import com.eriyaz.social.activities.PostDetailsActivity;
import com.eriyaz.social.activities.ProfileActivity;
import com.eriyaz.social.controllers.LikeController;
import com.eriyaz.social.controllers.RatingController;
import com.eriyaz.social.dialogs.CommentDialog;
import com.eriyaz.social.model.Post;
import com.eriyaz.social.model.Rating;
import com.eriyaz.social.model.RecordingItem;
import com.eriyaz.social.utils.RatingUtil;
import com.melnykov.fab.FloatingActionButton;
import com.xw.repo.BubbleSeekBar;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Created by Daniel on 1/1/2015.
 */
public class PlaybackFragment extends DialogFragment {

    private static final String LOG_TAG = "PlaybackFragment";

    public static final String RECORDING_ITEM = "recording_item";
    private RecordingItem item;

    private Handler mHandler = new Handler();

    private MediaPlayer mMediaPlayer = null;

    private SeekBar mSeekBar = null;
    private FloatingActionButton mPlayButton = null;
    private Button closeButton = null;
    private TextView mCurrentProgressTextView = null;
    private TextView mFileNameTextView = null;
    private TextView mFileLengthTextView = null;
    private TextView txtPercentage;
    private BubbleSeekBar ratingBar;
    private Post post;
    private Rating rating;
    private RatingController ratingController;
    private boolean isRatingChanged = false;
    private View ratingLayout;
    private boolean animateRatingThumb = true;
    private int maxPlayedTime;

    //stores whether or not the mediaplayer is currently playing audio
    private boolean isPlaying = false;

    //stores minutes and seconds of the length of the file.
    long minutes = 0;
    long seconds = 0;

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
        long itemDuration = item.getLength();
        minutes = TimeUnit.MILLISECONDS.toMinutes(itemDuration);
        seconds = TimeUnit.MILLISECONDS.toSeconds(itemDuration)
                - TimeUnit.MINUTES.toSeconds(minutes);
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
            activity.getAnalytics().logPlayedTime(post.getAuthorId(), maxPlayedTime);
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
        mFileLengthTextView = (TextView) view.findViewById(R.id.file_length_text_view);
        mCurrentProgressTextView = (TextView) view.findViewById(R.id.current_progress_text_view);
        txtPercentage = (TextView) view.findViewById(R.id.txtPercentage);
        ratingBar = (BubbleSeekBar) view.findViewById(R.id.ratingBar);
        ratingLayout = view.findViewById(R.id.seekbarContainerLayout);

        mSeekBar = (SeekBar) view.findViewById(R.id.seekbar);

        ColorFilter filter = new LightingColorFilter
                (getResources().getColor(R.color.primary), getResources().getColor(R.color.primary));
//        mSeekBar.getProgressDrawable().setColorFilter(filter);
//        mSeekBar.getThumb().setColorFilter(filter);

        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override

            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(mMediaPlayer != null && fromUser) {
                    mMediaPlayer.seekTo(progress);
                    mHandler.removeCallbacks(mRunnable);

                    long minutes = TimeUnit.MILLISECONDS.toMinutes(mMediaPlayer.getCurrentPosition());
                    long seconds = TimeUnit.MILLISECONDS.toSeconds(mMediaPlayer.getCurrentPosition())
                            - TimeUnit.MINUTES.toSeconds(minutes);
                    mCurrentProgressTextView.setText(String.format("%02d:%02d", minutes,seconds));

                    updateSeekBar();

                } else if (mMediaPlayer == null && fromUser) {
                    prepareMediaPlayerFromPoint(progress);
                    updateSeekBar();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                if(mMediaPlayer != null) {
                    // remove message Handler from updating progress bar
                    mHandler.removeCallbacks(mRunnable);
                }
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (mMediaPlayer != null) {
                    mHandler.removeCallbacks(mRunnable);
                    mMediaPlayer.seekTo(seekBar.getProgress());

                    long minutes = TimeUnit.MILLISECONDS.toMinutes(mMediaPlayer.getCurrentPosition());
                    long seconds = TimeUnit.MILLISECONDS.toSeconds(mMediaPlayer.getCurrentPosition())
                            - TimeUnit.MINUTES.toSeconds(minutes);
                    mCurrentProgressTextView.setText(String.format("%02d:%02d", minutes,seconds));
                    updateSeekBar();
                }
            }
        });

        mPlayButton = (FloatingActionButton) view.findViewById(R.id.fab_play);
        mPlayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onPlay(isPlaying);
                isPlaying = !isPlaying;
            }
        });

        closeButton = view.findViewById(R.id.closeButton);
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        mFileNameTextView.setText(item.getName());
        mFileLengthTextView.setText(String.format("%02d:%02d", minutes,seconds));

        updateRatingDetails();
        builder.setView(view);

        // request a window without the title
        dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);

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
    }

    @Override
    public void onPause() {
        super.onPause();

        if (mMediaPlayer != null) {
            stopPlaying();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mMediaPlayer != null) {
            stopPlaying();
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
                if (progressFloat > 0) animateRatingThumb = false;
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

    // Play start/stop
    private void onPlay(boolean isPlaying){
        if (!isPlaying) {
            //currently MediaPlayer is not playing audio
            if(mMediaPlayer == null) {
                startPlaying(); //start from beginning
            } else {
                resumePlaying(); //resume the currently paused MediaPlayer
            }

        } else {
            //pause the MediaPlayer
            pausePlaying();
        }
    }

    private void startPlaying() {
        mPlayButton.setImageResource(R.drawable.ic_media_pause);
        //Fix: app crashes when button is pressed multiple times on loading
        mPlayButton.setEnabled(false);
        mMediaPlayer = new MediaPlayer();

        try {
            txtPercentage.setText("Loading...");
            mMediaPlayer.setDataSource(item.getFilePath());

            mMediaPlayer.setOnBufferingUpdateListener(new MediaPlayer.OnBufferingUpdateListener() {
                public void onBufferingUpdate(MediaPlayer mp, int percent)
                {
                    double ratio = percent / 100.0;
//                    int bufferingLevel = (int)(mp.getDuration() * ratio);
//                    mSeekBar.setSecondaryProgress(bufferingLevel);
                    if (percent == 100) {
                        txtPercentage.setText("");
                    } else {
                        txtPercentage.setText(percent + "%");
                    }
                }
            });

            mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    txtPercentage.setText("");
                    mSeekBar.setMax(mMediaPlayer.getDuration());
                    mMediaPlayer.start();
                    mPlayButton.setEnabled(true);
                    updateSeekBar();
                }
            });

            mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    stopPlaying();
                }
            });

            mMediaPlayer.prepareAsync();
        } catch (IOException e) {
            Log.e(LOG_TAG, "prepare() failed");
        }

        //keep screen on while playing audio
        getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private void prepareMediaPlayerFromPoint(int progress) {
        //set mediaPlayer to start from middle of the audio file

        mMediaPlayer = new MediaPlayer();

        try {
            mMediaPlayer.setDataSource(item.getFilePath());
            mMediaPlayer.prepare();
            mSeekBar.setMax(mMediaPlayer.getDuration());
            mMediaPlayer.seekTo(progress);

            mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    stopPlaying();
                }
            });

        } catch (IOException e) {
            Log.e(LOG_TAG, "prepare() failed");
        }

        //keep screen on while playing audio
        getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private void pausePlaying() {
        mPlayButton.setImageResource(R.drawable.ic_media_play);
        mHandler.removeCallbacks(mRunnable);
        mMediaPlayer.pause();
    }

    private void resumePlaying() {
        mPlayButton.setImageResource(R.drawable.ic_media_pause);
        mHandler.removeCallbacks(mRunnable);
        mMediaPlayer.start();
        updateSeekBar();
    }

    private void stopPlaying() {
        mPlayButton.setImageResource(R.drawable.ic_media_play);
        mHandler.removeCallbacks(mRunnable);
        mMediaPlayer.stop();
        mMediaPlayer.reset();
        mMediaPlayer.release();
        mMediaPlayer = null;

        mSeekBar.setProgress(mSeekBar.getMax());
        isPlaying = !isPlaying;

        mCurrentProgressTextView.setText(mFileLengthTextView.getText());
        mSeekBar.setProgress(mSeekBar.getMax());

        //allow the screen to turn off again once audio is finished playing
        getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private void openCommentDialog() {
        CommentDialog commentDialog = new CommentDialog();
        Bundle args = new Bundle();
        args.putString(PostDetailsActivity.POST_ID_EXTRA_KEY, post.getId());
        commentDialog.setArguments(args);
        commentDialog.setTargetFragment(this,CommentDialog.NEW_COMMENT_REQUEST);
        commentDialog.show(getFragmentManager(), CommentDialog.TAG);
    }

    //updating mSeekBar
    private Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            if(mMediaPlayer != null){

                int mCurrentPosition = mMediaPlayer.getCurrentPosition();
                mSeekBar.setProgress(mCurrentPosition);

                long minutes = TimeUnit.MILLISECONDS.toMinutes(mCurrentPosition);
                long seconds = TimeUnit.MILLISECONDS.toSeconds(mCurrentPosition)
                        - TimeUnit.MINUTES.toSeconds(minutes);
                maxPlayedTime = Math.max(maxPlayedTime, (int) TimeUnit.MILLISECONDS.toSeconds(mCurrentPosition));
                mCurrentProgressTextView.setText(String.format("%02d:%02d", minutes, seconds));
                if (animateRatingThumb && ratingController != null) {
                    ratingController.startAnimateRatingButton(LikeController.AnimationType.COLOR_ANIM);
                }
                updateSeekBar();
            }
        }
    };

    private void updateSeekBar() {
        mHandler.postDelayed(mRunnable, 1000);
    }
}

